package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.exception.ResumeProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies raw text extraction for both supported formats, plus rejection
 * of empty/unsupported/corrupt/unreadable input. Real PDF and DOCX byte
 * content is generated in-memory using the same libraries' writer APIs,
 * rather than relying on checked-in binary fixture files.
 */
class ResumeParsingServiceTest {

    private final ResumeParsingService service = new ResumeParsingService();

    @Test
    void extractsTextFromPdf() throws IOException {
        byte[] pdfBytes = buildPdf("Jane Doe - Backend Engineer with 5 years of Java experience.");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);

        ResumeParsingService.ParsedResume parsed = service.parse(file);

        assertEquals(ResumeFileType.PDF, parsed.fileType());
        assertTrue(parsed.extractedText().contains("Backend Engineer"));
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        byte[] docxBytes = buildDocx("John Smith - Frontend Developer skilled in React and TypeScript.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes);

        ResumeParsingService.ParsedResume parsed = service.parse(file);

        assertEquals(ResumeFileType.DOCX, parsed.fileType());
        assertTrue(parsed.extractedText().contains("Frontend Developer"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "Just plain text".getBytes());

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    @Test
    void rejectsFileWithNoFilename() {
        MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf", new byte[]{1, 2, 3});

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    @Test
    void rejectsCorruptPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "not a real pdf".getBytes());

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    @Test
    void rejectsCorruptDocx() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "not a real docx".getBytes());

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    @Test
    void rejectsPdfWithNoExtractableText() throws IOException {
        byte[] blankPdfBytes = buildPdf(null);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", blankPdfBytes);

        assertThrows(ResumeProcessingException.class, () -> service.parse(file));
    }

    private byte[] buildPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            if (text != null) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText(text);
                    contentStream.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
