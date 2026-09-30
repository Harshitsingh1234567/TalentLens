package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ResumeUploadResponse;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeParsingService resumeParsingService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    private ResumeService resumeService;

    private User owner;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(resumeRepository, resumeParsingService, currentUserService);

        owner = new User("jobseeker@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        owner.setId(42L);
    }

    @Test
    void uploadAssociatesResumeWithAuthenticatedOwner() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[]{1});

        when(currentUserService.resolve(authentication)).thenReturn(owner);
        when(resumeParsingService.parse(file))
                .thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.PDF, "Extracted resume text"));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume toSave = invocation.getArgument(0);
            return withGeneratedId(toSave, 7L);
        });

        ResumeUploadResponse response = resumeService.upload(file, null, authentication);

        assertEquals(7L, response.getId());
        assertEquals("resume.pdf", response.getOriginalFilename());
        assertEquals(ResumeFileType.PDF, response.getFileType());
        assertEquals("Extracted resume text", response.getExtractedText());
        assertNull(response.getCandidateName());

        ArgumentCaptor<Resume> savedCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(savedCaptor.capture());
        assertEquals(owner, savedCaptor.getValue().getOwner());
    }

    @Test
    void uploadTrimsBlankCandidateNameToNull() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1});

        when(currentUserService.resolve(authentication)).thenReturn(owner);
        when(resumeParsingService.parse(file))
                .thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.DOCX, "Some text"));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> withGeneratedId(invocation.getArgument(0), 1L));

        ResumeUploadResponse response = resumeService.upload(file, "   ", authentication);

        assertNull(response.getCandidateName());
    }

    @Test
    void uploadTrimsWhitespaceAroundCandidateName() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1});

        when(currentUserService.resolve(authentication)).thenReturn(owner);
        when(resumeParsingService.parse(file))
                .thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.DOCX, "Some text"));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> withGeneratedId(invocation.getArgument(0), 2L));

        ResumeUploadResponse response = resumeService.upload(file, "  Alex Candidate  ", authentication);

        assertEquals("Alex Candidate", response.getCandidateName());
    }

    /**
     * Simulates the JPA identity-generation strategy assigning an id on
     * save, without requiring a real persistence context in this unit test.
     */
    private Resume withGeneratedId(Resume resume, Long id) {
        Resume withId = new Resume(resume.getOwner(), resume.getCandidateName(), resume.getOriginalFilename(),
                resume.getFileType(), resume.getExtractedText(), resume.getUploadedAt());
        try {
            var idField = Resume.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(withId, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return withId;
    }
}
