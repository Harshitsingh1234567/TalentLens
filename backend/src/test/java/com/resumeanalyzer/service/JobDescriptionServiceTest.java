package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.JobDescriptionRequest;
import com.resumeanalyzer.dto.JobDescriptionResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceTest {

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    private JobDescriptionService jobDescriptionService;

    private User owner;

    @BeforeEach
    void setUp() {
        jobDescriptionService = new JobDescriptionService(jobDescriptionRepository, currentUserService);

        owner = new User("recruiter@example.com", "$2a$10$hash", Role.RECRUITER);
        owner.setId(9L);
    }

    @Test
    void submitAssociatesJobDescriptionWithAuthenticatedOwnerAndTrimsContent() {
        JobDescriptionRequest request = new JobDescriptionRequest("  We are hiring a backend engineer.  ");

        when(currentUserService.resolve(authentication)).thenReturn(owner);
        when(jobDescriptionRepository.save(any(JobDescription.class))).thenAnswer(invocation -> {
            JobDescription toSave = invocation.getArgument(0);
            return withGeneratedId(toSave, 3L);
        });

        JobDescriptionResponse response = jobDescriptionService.submit(request, authentication);

        assertEquals(3L, response.getId());
        assertEquals("We are hiring a backend engineer.", response.getContent());

        ArgumentCaptor<JobDescription> savedCaptor = ArgumentCaptor.forClass(JobDescription.class);
        verify(jobDescriptionRepository).save(savedCaptor.capture());
        assertEquals(owner, savedCaptor.getValue().getOwner());
        assertEquals("We are hiring a backend engineer.", savedCaptor.getValue().getContent());
    }

    private JobDescription withGeneratedId(JobDescription jobDescription, Long id) {
        JobDescription withId = new JobDescription(jobDescription.getOwner(), jobDescription.getContent(),
                jobDescription.getSubmittedAt());
        try {
            var idField = JobDescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(withId, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return withId;
    }
}
