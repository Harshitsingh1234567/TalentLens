package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.exception.ResumeProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

/**
 * Extracts raw text from an uploaded resume file.
 *
 * Phase 2 scope: this service performs document text extraction only. It
 * deliberately does not perform heading detection, section segmentation
 * (Skills/Education/Experience/etc.), or skill/keyword identification -
 * those are deferred to the later matching/analysis pipeline, where the
 * structured output is actually consumed.
 *
 * Supported formats are exactly PDF (via Apache PDFBox) and DOCX (via
 * Apache POI), per the project's fixed technology decisions.
 */
@Service
public class ResumeParsingService {

    /**
     * The result of parsing an uploaded resume: its detected file type and
     * the raw text extracted from it.
     */
    public record ParsedResume(ResumeFileType fileType, String extractedText) {
    }

    public ParsedResume parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeProcessingException("The uploaded resume file is empty");
        }

        ResumeFileType fileType = detectFileType(file.getOriginalFilename());

        String rawText;
        try {
            // Deliberately an if/else rather than a `switch` expression over
            // the enum: javac compiles an enum switch into a call through a
            // synthetic, lazily-initialized "$1" switch-map helper class
            // (e.g. ResumeParsingService$1). When this class is mocked with
            // Mockito's default inline mock maker on JDK 21, Byte Buddy's
            // ASM-based bytecode rewriting needs to compute stack-map frames
            // for every method it redefines, including this one, and doing
            // so can fail to resolve that synthetic helper class, breaking
            // with a NoClassDefFoundError. An if/else with only two possible
            // ResumeFileType values is behaviorally identical and avoids
            // generating that helper class entirely - see ResumeServiceTest,
            // which mocks this class.
            if (fileType == ResumeFileType.PDF) {
                rawText = extractPdfText(file);
            } else {
                rawText = extractDocxText(file);
            }
        } catch (IOException | RuntimeException ex) {
            // PDFBox reports a malformed PDF via IOException, but POI's
            // OOXML package parser (used for DOCX) reports a non-OOXML or
            // otherwise unreadable file via an unchecked exception (e.g.
            // NotOfficeXmlFileException, which extends RuntimeException,
            // not IOException). Both are equally "this file could not be
            // parsed" from the caller's perspective, so both are wrapped
            // uniformly here rather than leaking a library-specific,
            // unchecked exception type past this service.
            throw new ResumeProcessingException(
                    "The uploaded file could not be read - it may be corrupted or not a valid "
                            + fileType + " document",
                    ex);
        }

        if (rawText == null || rawText.isBlank()) {
            throw new ResumeProcessingException(
                    "No extractable text was found in the uploaded file "
                            + "(scanned/image-only documents are not currently supported)");
        }

        return new ParsedResume(fileType, rawText.trim());
    }

    private ResumeFileType detectFileType(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResumeProcessingException(
                    "The uploaded file must have a filename with a .pdf or .docx extension");
        }

        String lowerCaseFilename = originalFilename.toLowerCase(Locale.ROOT);
        if (lowerCaseFilename.endsWith(".pdf")) {
            return ResumeFileType.PDF;
        }
        if (lowerCaseFilename.endsWith(".docx")) {
            return ResumeFileType.DOCX;
        }

        throw new ResumeProcessingException("Unsupported resume file type - only PDF and DOCX are supported");
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocxText(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
