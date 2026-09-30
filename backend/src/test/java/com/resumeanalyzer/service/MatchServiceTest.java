package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.MatchRequest;
import com.resumeanalyzer.dto.MatchResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MatchScoringEngine matchScoringEngine;

    @Mock
    private Authentication authentication;

    private MatchService matchService;

    private User caller;
    private User otherUser;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(resumeRepository, jobDescriptionRepository, currentUserService, matchScoringEngine);

        caller = new User("caller@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        caller.setId(1L);

        otherUser = new User("other@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        otherUser.setId(2L);
    }

    @Test
    void matchReturnsScoreForOwnedResumeAndJobDescription() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "Java developer", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(caller, "Java engineer needed", Instant.now());
        setId(jobDescription, 20L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resume));
        when(jobDescriptionRepository.findByIdFetchingOwner(20L)).thenReturn(Optional.of(jobDescription));
        when(matchScoringEngine.score("Java developer", "Java engineer needed"))
                .thenReturn(new MatchScoringEngine.MatchScoreResult(
                        Set.of("java"), Set.of(), 1.0, 0.5, 0.5, 0.66, "explanation"));

        MatchResponse response = matchService.match(new MatchRequest(10L, 20L), authentication);

        assertEquals(10L, response.getResumeId());
        assertEquals(20L, response.getJobDescriptionId());
        assertEquals(0.66, response.getFinalScore(), 0.0001);
        assertEquals(Set.of("java"), response.getMatchedSkills());
    }

    @Test
    void matchRejectsResumeNotOwnedByCaller() {
        Resume resumeOwnedByOther = new Resume(otherUser, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resumeOwnedByOther, 10L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resumeOwnedByOther));

        assertThrows(ResourceNotFoundException.class,
                () -> matchService.match(new MatchRequest(10L, 20L), authentication));
    }

    @Test
    void matchRejectsNonExistentResume() {
        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> matchService.match(new MatchRequest(999L, 20L), authentication));
    }

    @Test
    void matchRejectsJobDescriptionNotOwnedByCaller() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);
        JobDescription jdOwnedByOther = new JobDescription(otherUser, "content", Instant.now());
        setId(jdOwnedByOther, 20L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resume));
        when(jobDescriptionRepository.findByIdFetchingOwner(20L)).thenReturn(Optional.of(jdOwnedByOther));

        assertThrows(ResourceNotFoundException.class,
                () -> matchService.match(new MatchRequest(10L, 20L), authentication));
    }

    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
