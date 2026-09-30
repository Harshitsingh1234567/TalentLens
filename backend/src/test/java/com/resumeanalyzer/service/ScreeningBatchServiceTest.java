package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ScreeningBatchResponse;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.ScreeningBatch;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.exception.ResumeProcessingException;
import com.resumeanalyzer.exception.ScreeningBatchValidationException;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningBatchServiceTest {

    @Mock
    private ScreeningBatchRepository screeningBatchRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ResumeParsingService resumeParsingService;

    @Mock
    private Authentication authentication;

    private ScreeningBatchService screeningBatchService;

    private User recruiter;
    private User jobSeeker;

    @BeforeEach
    void setUp() {
        screeningBatchService = new ScreeningBatchService(
                screeningBatchRepository, resumeRepository, currentUserService, resumeParsingService);

        recruiter = new User("recruiter@example.com", "$2a$10$hash", Role.RECRUITER);
        assignId(recruiter, 1L);
        jobSeeker = new User("seeker@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        assignId(jobSeeker, 2L);
    }

    @Test
    void createBatchPersistsBatchAndAllValidResumesForRecruiter() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.save(any(ScreeningBatch.class))).thenAnswer(invocation -> {
            ScreeningBatch batch = invocation.getArgument(0);
            assignId(batch, 100L);
            return batch;
        });
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            assignId(resume, 200L + resumeSaveCount++);
            return resume;
        });

        MockMultipartFile file1 = new MockMultipartFile("files", "alice_resume.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "bob_resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1});
        when(resumeParsingService.parse(file1)).thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.PDF, "Alice's text"));
        when(resumeParsingService.parse(file2)).thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.DOCX, "Bob's text"));

        ScreeningBatchResponse response = screeningBatchService.createBatch(
                "Backend Engineer - Sept", List.of(file1, file2), authentication);

        assertEquals(100L, response.getId());
        assertEquals("Backend Engineer - Sept", response.getName());
        assertEquals(2, response.getResumes().size());
        assertTrue(response.getFailedFiles().isEmpty());
        assertEquals("alice_resume", response.getResumes().get(0).candidateName());
        assertEquals("bob_resume", response.getResumes().get(1).candidateName());
        verify(resumeRepository, times(2)).save(any(Resume.class));
    }

    @Test
    void createBatchRecordsPerFileFailureButStillPersistsOtherValidFiles() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.save(any(ScreeningBatch.class))).thenAnswer(invocation -> {
            ScreeningBatch batch = invocation.getArgument(0);
            assignId(batch, 101L);
            return batch;
        });
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            assignId(resume, 300L);
            return resume;
        });

        MockMultipartFile goodFile = new MockMultipartFile("files", "good.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile corruptFile = new MockMultipartFile("files", "corrupt.pdf", "application/pdf", new byte[]{1});
        when(resumeParsingService.parse(goodFile)).thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.PDF, "text"));
        when(resumeParsingService.parse(corruptFile)).thenThrow(new ResumeProcessingException("The uploaded file is not a valid PDF document"));

        ScreeningBatchResponse response = screeningBatchService.createBatch(
                "Batch A", List.of(goodFile, corruptFile), authentication);

        assertEquals(1, response.getResumes().size());
        assertEquals(1, response.getFailedFiles().size());
        assertEquals("corrupt.pdf", response.getFailedFiles().get(0).originalFilename());
        assertEquals("The uploaded file is not a valid PDF document", response.getFailedFiles().get(0).reason());
        verify(resumeRepository, times(1)).save(any(Resume.class));
    }

    @Test
    void createBatchStillPersistsAnEmptyBatchWhenEveryFileFails() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.save(any(ScreeningBatch.class))).thenAnswer(invocation -> {
            ScreeningBatch batch = invocation.getArgument(0);
            assignId(batch, 102L);
            return batch;
        });

        MockMultipartFile corruptFile = new MockMultipartFile("files", "corrupt.pdf", "application/pdf", new byte[]{1});
        when(resumeParsingService.parse(corruptFile)).thenThrow(new ResumeProcessingException("Corrupted file"));

        ScreeningBatchResponse response = screeningBatchService.createBatch(
                "Batch B", List.of(corruptFile), authentication);

        assertEquals(102L, response.getId());
        assertTrue(response.getResumes().isEmpty());
        assertEquals(1, response.getFailedFiles().size());
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void createBatchAllowsDuplicateFilesWithinTheSameRequest() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.save(any(ScreeningBatch.class))).thenAnswer(invocation -> {
            ScreeningBatch batch = invocation.getArgument(0);
            assignId(batch, 103L);
            return batch;
        });
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            assignId(resume, 400L + resumeSaveCount++);
            return resume;
        });

        MockMultipartFile duplicate1 = new MockMultipartFile("files", "same_name.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile duplicate2 = new MockMultipartFile("files", "same_name.pdf", "application/pdf", new byte[]{1});
        when(resumeParsingService.parse(any(MultipartFile.class)))
                .thenReturn(new ResumeParsingService.ParsedResume(ResumeFileType.PDF, "identical text"));

        ScreeningBatchResponse response = screeningBatchService.createBatch(
                "Batch C", List.of(duplicate1, duplicate2), authentication);

        // Both occurrences are persisted as their own, independent Resume
        // rows - no silent de-duplication or merging.
        assertEquals(2, response.getResumes().size());
        verify(resumeRepository, times(2)).save(any(Resume.class));
    }

    @Test
    void createBatchRejectsCallerWhoIsNotRecruiter() {
        when(currentUserService.resolve(authentication)).thenReturn(jobSeeker);

        assertThrows(AccessDeniedException.class,
                () -> screeningBatchService.createBatch("Batch", List.of(), authentication));

        verify(screeningBatchRepository, never()).save(any());
    }

    @Test
    void createBatchRejectsBlankName() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});

        assertThrows(ScreeningBatchValidationException.class,
                () -> screeningBatchService.createBatch("   ", List.of(file), authentication));

        verify(screeningBatchRepository, never()).save(any());
    }

    @Test
    void createBatchRejectsNameOver255Characters() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});
        String tooLong = "x".repeat(256);

        assertThrows(ScreeningBatchValidationException.class,
                () -> screeningBatchService.createBatch(tooLong, List.of(file), authentication));

        verify(screeningBatchRepository, never()).save(any());
    }

    @Test
    void createBatchRejectsEmptyFileList() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);

        assertThrows(ScreeningBatchValidationException.class,
                () -> screeningBatchService.createBatch("Batch", List.of(), authentication));

        verify(screeningBatchRepository, never()).save(any());
    }

    @Test
    void createBatchRejectsMoreThanFiftyFiles() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        List<MultipartFile> tooManyFiles = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> (MultipartFile) new MockMultipartFile("files", "r" + i + ".pdf", "application/pdf", new byte[]{1}))
                .toList();

        assertThrows(ScreeningBatchValidationException.class,
                () -> screeningBatchService.createBatch("Batch", tooManyFiles, authentication));

        verify(screeningBatchRepository, never()).save(any());
    }

    @Test
    void getBatchReturnsResumesForOwningRecruiter() {
        ScreeningBatch batch = new ScreeningBatch(recruiter, "Batch D", Instant.now());
        assignId(batch, 104L);
        Resume resume = new Resume(recruiter, "Carol", "carol.pdf", ResumeFileType.PDF, "text", Instant.now());
        assignId(resume, 500L);

        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.findByIdFetchingOwner(104L)).thenReturn(Optional.of(batch));
        when(resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(104L)).thenReturn(List.of(resume));

        ScreeningBatchResponse response = screeningBatchService.getBatch(104L, authentication);

        assertEquals(104L, response.getId());
        assertEquals(1, response.getResumes().size());
        assertEquals("Carol", response.getResumes().get(0).candidateName());
        assertTrue(response.getFailedFiles().isEmpty());
    }

    @Test
    void getBatchRejectsWhenNotOwnedByCaller() {
        User otherRecruiter = new User("other@example.com", "$2a$10$hash", Role.RECRUITER);
        assignId(otherRecruiter, 3L);
        ScreeningBatch batch = new ScreeningBatch(otherRecruiter, "Not mine", Instant.now());
        assignId(batch, 105L);

        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.findByIdFetchingOwner(105L)).thenReturn(Optional.of(batch));

        assertThrows(ResourceNotFoundException.class, () -> screeningBatchService.getBatch(105L, authentication));
    }

    @Test
    void getBatchRejectsNonExistentId() {
        when(currentUserService.resolve(authentication)).thenReturn(recruiter);
        when(screeningBatchRepository.findByIdFetchingOwner(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> screeningBatchService.getBatch(999L, authentication));
    }

    @Test
    void getBatchRejectsCallerWhoIsNotRecruiter() {
        when(currentUserService.resolve(authentication)).thenReturn(jobSeeker);

        assertThrows(AccessDeniedException.class, () -> screeningBatchService.getBatch(1L, authentication));

        verify(screeningBatchRepository, never()).findByIdFetchingOwner(any());
    }

    private long resumeSaveCount = 0;

    /**
     * Simulates the JPA identity-generation strategy assigning an id on
     * save, mutating the same instance in place (rather than
     * reconstructing via the constructor) so any other state already set
     * on it - e.g. {@code Resume.screeningBatch} via
     * {@code assignToBatch} - is preserved.
     */
    private void assignId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
