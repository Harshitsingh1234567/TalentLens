package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRepository;
import com.resumeanalyzer.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST /api/resumes, exercising the full stack
 * (controller -> service -> real PDFBox/POI parsing -> repository -> H2
 * test database) behind real JWT authentication issued by the Phase 1
 * auth endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ScreeningBatchRepository screeningBatchRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private ScreeningBatchRankingEntryRepository rankingEntryRepository;

    @Autowired
    private ScreeningBatchRankingRepository rankingRepository;

    @BeforeEach
    void cleanDatabase() {
        // All @SpringBootTest classes in this project share one cached
        // Spring context (and H2 database instance) across the test run,
        // so all child tables must be cleared before deleting users.
        // Analysis is deleted first since it references Resume/JobDescription.
        // Phase 7: ranking rows reference Resume/ScreeningBatch/JobDescription
        // and must be cleared before those parent tables.
        rankingEntryRepository.deleteAll();
        rankingRepository.deleteAll();
        analysisRepository.deleteAll();
        resumeRepository.deleteAll();
        screeningBatchRepository.deleteAll();
        jobDescriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void uploadingValidPdfResumeCreatesRecordOwnedByAuthenticatedUser() throws Exception {
        String token = registerAndLogin("jobseeker-pdf@example.com", "SecurePass123", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", buildPdf("Experienced Java backend engineer."));

        String responseBody = mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.originalFilename").value("resume.pdf"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.candidateName").doesNotExist())
                .andExpect(jsonPath("$.extractedText").value(org.hamcrest.Matchers.containsString("backend engineer")))
                .andReturn().getResponse().getContentAsString();

        Long resumeId = ((Number) objectMapper.readValue(responseBody, Map.class).get("id")).longValue();
        Optional<Resume> saved = resumeRepository.findByIdFetchingOwner(resumeId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getOwner().getEmail()).isEqualTo("jobseeker-pdf@example.com");
    }

    @Test
    void uploadingValidDocxResumeWithCandidateNameStoresTag() throws Exception {
        String token = registerAndLogin("recruiter-docx@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile(
                "file", "candidate.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildDocx("Skilled frontend developer with React expertise."));

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("candidateName", "Alex Candidate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("DOCX"))
                .andExpect(jsonPath("$.candidateName").value("Alex Candidate"))
                .andExpect(jsonPath("$.extractedText").value(org.hamcrest.Matchers.containsString("frontend developer")));
    }

    /**
     * Regression test for the production incident where {@code
     * resumes.extracted_text} had drifted to TINYTEXT (255-character max)
     * on the live database, causing "Data truncation: Data too long for
     * column 'extracted_text'" on ordinary resume uploads whose extracted
     * text exceeded 255 characters. This builds a PDF whose extracted
     * text is well beyond that old limit and asserts it is persisted and
     * returned in full (through the real controller -> service ->
     * repository -> H2 schema-generated-from-entity chain), so a future
     * regression to a small-capacity text mapping/column would fail this
     * test rather than only surfacing in production.
     */
    @Test
    void uploadingResumeWithExtractedTextLongerThan255CharactersIsPersistedInFull() throws Exception {
        String token = registerAndLogin("longtext@example.com", "SecurePass123", "JOB_SEEKER");

        StringBuilder longText = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            longText.append(String.format("Line %02d: filler content for regression test.", i));
            if (i < 50) {
                longText.append('\n');
            }
        }
        String expectedText = longText.toString();
        assertThat(expectedText.length()).isGreaterThan(255);

        MockMultipartFile file = new MockMultipartFile(
                "file", "long-resume.pdf", "application/pdf", buildMultilinePdf(expectedText));

        String responseBody = mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        String returnedText = (String) responseMap.get("extractedText");
        assertThat(returnedText.length()).isGreaterThan(255);
        assertThat(returnedText.replace("\r\n", "\n").replace("\r", "\n").trim())
                .isEqualTo(expectedText.trim());

        Long resumeId = ((Number) responseMap.get("id")).longValue();
        Optional<Resume> saved = resumeRepository.findByIdFetchingOwner(resumeId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getExtractedText().length()).isGreaterThan(255);
    }

    @Test
    void uploadingUnsupportedFileTypeIsRejected() throws Exception {
        String token = registerAndLogin("badtype@example.com", "SecurePass123", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "plain text".getBytes());

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Phase 9 (L1): verifies, end-to-end, that a file exceeding the
     * configured per-file multipart limit (spring.servlet.multipart.
     * max-file-size: 5MB in application.yml) is safely rejected with 400
     * rather than an unhandled/uncaught failure. The content does not
     * need to be a valid PDF - Spring's multipart size check rejects an
     * oversized part before the request ever reaches the controller or
     * {@code ResumeParsingService}, so this specifically exercises
     * {@code GlobalExceptionHandler.handleMaxUploadSizeExceeded} rather
     * than any parsing logic.
     */
    @Test
    void uploadingOversizedFileIsRejected() throws Exception {
        String token = registerAndLogin("oversized@example.com", "SecurePass123", "JOB_SEEKER");
        byte[] oversizedContent = new byte[6 * 1024 * 1024]; // 6MB, over the 5MB per-file limit
        MockMultipartFile file = new MockMultipartFile("file", "huge-resume.pdf", "application/pdf", oversizedContent);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void uploadingWithoutJwtIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", buildPdf("text"));

        mockMvc.perform(multipart("/api/resumes").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingResumesReturnsOnlyAuthenticatedUsersOwnResumes() throws Exception {
        String token = registerAndLogin("lister@example.com", "SecurePass123", "JOB_SEEKER");
        mockMvc.perform(multipart("/api/resumes")
                        .file(new MockMultipartFile("file", "first.pdf", "application/pdf", buildPdf("First resume.")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/resumes")
                        .file(new MockMultipartFile("file", "second.pdf", "application/pdf", buildPdf("Second resume.")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Assertions are deliberately order-independent: uploadedAt is a
        // wall-clock Instant, so a strict $[0]/$[1] index assertion could
        // be flaky if two uploads ever landed in the same millisecond.
        mockMvc.perform(get("/api/resumes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].uploadedAt", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].originalFilename")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("first.pdf", "second.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("extractedText"))));
    }

    @Test
    void listingResumesDoesNotReturnAnotherUsersResumes() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com", "SecurePass123", "JOB_SEEKER");
        String otherToken = registerAndLogin("other-user@example.com", "SecurePass123", "JOB_SEEKER");
        mockMvc.perform(multipart("/api/resumes")
                        .file(new MockMultipartFile("file", "owner-resume.pdf", "application/pdf", buildPdf("Owner's resume.")))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/resumes").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listingResumesReturnsEmptyListWhenUserHasNoResumes() throws Exception {
        String token = registerAndLogin("no-resumes@example.com", "SecurePass123", "JOB_SEEKER");

        mockMvc.perform(get("/api/resumes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listingResumesWithoutJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email, String password, String role) throws Exception {
        Map<String, Object> registerPayload = Map.of("email", email, "password", password, "role", role);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        Map<String, Object> loginPayload = Map.of("email", email, "password", password);
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return (String) objectMapper.readValue(loginResponse, Map.class).get("token");
    }

    private byte[] buildPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Writes each line of {@code multilineText} (split on {@code \n}) as
     * its own line of text on a single PDF page, so PDFBox's text
     * stripper extracts it back out as multiple lines - used to build
     * resume text long enough to exceed the old, incorrect 255-character
     * TINYTEXT limit (see {@code uploadingResumeWithExtractedTextLongerThan255CharactersIsPersistedInFull}).
     */
    private byte[] buildMultilinePdf(String multilineText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 750);
                contentStream.setLeading(14);
                for (String line : multilineText.split("\n")) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildDocx(String text) throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
