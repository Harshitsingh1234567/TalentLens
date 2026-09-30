package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ScreeningBatchAnalyzeRequest;
import com.resumeanalyzer.dto.ScreeningBatchRankingResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.ScreeningBatch;
import com.resumeanalyzer.entity.ScreeningBatchRanking;
import com.resumeanalyzer.entity.ScreeningBatchRankingEntry;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningBatchRankingServiceTest {

    @Mock
    private ScreeningBatchService screeningBatchService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @Mock
    private ScreeningBatchRankingRepository rankingRepository;

    @Mock
    private ScreeningBatchRankingEntryRepository rankingEntryRepository;

    @Mock
    private MatchScoringEngine matchScoringEngine;

    @Mock
    private Authentication authentication;

    private ScreeningBatchRankingService rankingService;

    private User recruiter;
    private User otherRecruiter;
    private ScreeningBatch batch;
    private JobDescription jobDescription;

    @BeforeEach
    void setUp() {
        rankingService = new ScreeningBatchRankingService(
                screeningBatchService, resumeRepository, jobDescriptionRepository,
                rankingRepository, rankingEntryRepository, matchScoringEngine);

        recruiter = new User("recruiter@example.com", "$2a$10$hash", Role.RECRUITER);
        setId(recruiter, 1L);

        otherRecruiter = new User("other-recruiter@example.com", "$2a$10$hash", Role.RECRUITER);
        setId(otherRecruiter, 2L);

        batch = new ScreeningBatch(recruiter, "Backend hiring", Instant.now());
        setId(batch, 100L);

        jobDescription = new JobDescription(recruiter, "Java Spring engineer needed", Instant.now());
        setId(jobDescription, 200L);
    }

    @Test
    void analyzeScoresAndRanksResumesHighestScoreFirst() {
        Resume weakMatch = resume(recruiter, "Alice", "Only knows HTML", 11L);
        Resume strongMatch = resume(recruiter, "Bob", "Expert Java Spring engineer", 12L);

        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(200L)).thenReturn(Optional.of(jobDescription));
        when(resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(weakMatch, strongMatch));
        when(matchScoringEngine.score("Only knows HTML", jobDescription.getContent()))
                .thenReturn(new MatchScoringEngine.MatchScoreResult(Set.of(), Set.of("java"), 0.0, 0.1, 0.1, 0.1, "weak"));
        when(matchScoringEngine.score("Expert Java Spring engineer", jobDescription.getContent()))
                .thenReturn(new MatchScoringEngine.MatchScoreResult(Set.of("java", "spring"), Set.of(), 1.0, 0.8, 0.9, 0.92, "strong"));
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.empty());
        when(rankingRepository.save(any(ScreeningBatchRanking.class))).thenAnswer(invocation -> {
            ScreeningBatchRanking ranking = invocation.getArgument(0);
            setId(ranking, 500L);
            return ranking;
        });
        when(rankingEntryRepository.save(any(ScreeningBatchRankingEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningBatchRankingResponse response =
                rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(200L), authentication);

        assertEquals(100L, response.screeningBatchId());
        assertEquals(200L, response.jobDescriptionId());
        assertEquals(2, response.candidatesRanked());
        assertEquals(2, response.rankings().size());
        assertEquals(12L, response.rankings().get(0).resumeId());
        assertEquals("Bob", response.rankings().get(0).candidateName());
        assertEquals(0.92, response.rankings().get(0).finalScore(), 0.0001);
        assertEquals(1, response.rankings().get(0).rank());
        assertEquals(11L, response.rankings().get(1).resumeId());
        assertEquals(2, response.rankings().get(1).rank());
        assertEquals(Set.of("java", "spring"), response.rankings().get(0).matchedSkills());
    }

    @Test
    void analyzeBreaksTiedScoresByAscendingResumeId() {
        Resume first = resume(recruiter, "Resume A", "text a", 30L);
        Resume second = resume(recruiter, "Resume B", "text b", 20L);

        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(200L)).thenReturn(Optional.of(jobDescription));
        when(resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(first, second));
        MatchScoringEngine.MatchScoreResult tiedResult =
                new MatchScoringEngine.MatchScoreResult(Set.of(), Set.of(), 0.5, 0.5, 0.5, 0.5, "tied");
        when(matchScoringEngine.score(any(), any())).thenReturn(tiedResult);
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.empty());
        when(rankingRepository.save(any(ScreeningBatchRanking.class))).thenAnswer(invocation -> {
            ScreeningBatchRanking ranking = invocation.getArgument(0);
            setId(ranking, 501L);
            return ranking;
        });
        when(rankingEntryRepository.save(any(ScreeningBatchRankingEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningBatchRankingResponse response =
                rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(200L), authentication);

        // Equal final scores (0.5 each) must break the tie by ascending resume id: 20L before 30L.
        assertEquals(20L, response.rankings().get(0).resumeId());
        assertEquals(30L, response.rankings().get(1).resumeId());
    }

    @Test
    void analyzeOnBatchWithNoResumesProducesEmptyDeterministicRanking() {
        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(200L)).thenReturn(Optional.of(jobDescription));
        when(resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(100L)).thenReturn(List.of());
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.empty());
        when(rankingRepository.save(any(ScreeningBatchRanking.class))).thenAnswer(invocation -> {
            ScreeningBatchRanking ranking = invocation.getArgument(0);
            setId(ranking, 502L);
            return ranking;
        });

        ScreeningBatchRankingResponse response =
                rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(200L), authentication);

        assertEquals(0, response.candidatesRanked());
        assertTrue(response.rankings().isEmpty());
        verify(rankingEntryRepository, never()).save(any());
    }

    @Test
    void analyzeReplacesAnyPreviousRankingForTheSameBatch() {
        Resume resume = resume(recruiter, "Alice", "Java engineer", 11L);
        ScreeningBatchRanking existingRanking = new ScreeningBatchRanking(batch, jobDescription, Instant.now(), 3);
        setId(existingRanking, 900L);

        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(200L)).thenReturn(Optional.of(jobDescription));
        when(resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(100L)).thenReturn(List.of(resume));
        when(matchScoringEngine.score(any(), any()))
                .thenReturn(new MatchScoringEngine.MatchScoreResult(Set.of(), Set.of(), 0.5, 0.5, 0.5, 0.5, "x"));
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.of(existingRanking));
        when(rankingRepository.save(any(ScreeningBatchRanking.class))).thenAnswer(invocation -> {
            ScreeningBatchRanking ranking = invocation.getArgument(0);
            setId(ranking, 501L);
            return ranking;
        });
        when(rankingEntryRepository.save(any(ScreeningBatchRankingEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(200L), authentication);

        // Regression coverage for the re-analysis persistence-ordering bug:
        // the previous ranking's entries and header must be deleted AND
        // flushed before the new header is inserted, or the unique
        // one-ranking-per-batch constraint can be violated transiently
        // (Hibernate does not guarantee DML executes in call order within
        // a flush). Verifying both the calls and their relative order
        // guards against a regression even though Mockito itself does not
        // enforce a real unique constraint.
        InOrder inOrder = inOrder(rankingEntryRepository, rankingRepository);
        inOrder.verify(rankingEntryRepository).deleteByRankingId(900L);
        inOrder.verify(rankingRepository).delete(existingRanking);
        inOrder.verify(rankingRepository).flush();
        inOrder.verify(rankingRepository).save(any(ScreeningBatchRanking.class));
    }

    @Test
    void analyzeRejectsJobDescriptionNotOwnedByBatchOwner() {
        JobDescription foreignJobDescription = new JobDescription(otherRecruiter, "content", Instant.now());
        setId(foreignJobDescription, 201L);

        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(201L)).thenReturn(Optional.of(foreignJobDescription));

        assertThrows(ResourceNotFoundException.class,
                () -> rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(201L), authentication));

        verify(rankingRepository, never()).save(any());
    }

    @Test
    void analyzeRejectsNonExistentJobDescription() {
        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(jobDescriptionRepository.findByIdFetchingOwner(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(999L), authentication));
    }

    @Test
    void analyzePropagatesRejectionFromBatchOwnershipResolution() {
        when(screeningBatchService.resolveOwnedBatch(999L, authentication))
                .thenThrow(new ResourceNotFoundException("No screening batch found for the given id"));

        assertThrows(ResourceNotFoundException.class,
                () -> rankingService.analyze(999L, new ScreeningBatchAnalyzeRequest(200L), authentication));

        verify(jobDescriptionRepository, never()).findByIdFetchingOwner(any());
    }

    @Test
    void analyzePropagatesRoleRejectionFromBatchOwnershipResolution() {
        when(screeningBatchService.resolveOwnedBatch(100L, authentication))
                .thenThrow(new AccessDeniedException("Only recruiters may create or manage screening batches"));

        assertThrows(AccessDeniedException.class,
                () -> rankingService.analyze(100L, new ScreeningBatchAnalyzeRequest(200L), authentication));
    }

    @Test
    void getRankingReturnsPersistedEntriesInStoredRankOrder() {
        ScreeningBatchRanking ranking = new ScreeningBatchRanking(batch, jobDescription, Instant.now(), 2);
        setId(ranking, 500L);

        Resume topResume = resume(recruiter, "Bob", "text", 12L);
        Resume secondResume = resume(recruiter, "Alice", "text", 11L);

        ScreeningBatchRankingEntry first = new ScreeningBatchRankingEntry(
                ranking, topResume, "Bob", 1, Set.of("java"), Set.of(), 1.0, 0.8, 0.9, 0.92);
        ScreeningBatchRankingEntry second = new ScreeningBatchRankingEntry(
                ranking, secondResume, "Alice", 2, Set.of(), Set.of("java"), 0.0, 0.1, 0.1, 0.1);

        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.of(ranking));
        when(rankingEntryRepository.findByRankingIdOrderByRankPositionAsc(500L))
                .thenReturn(List.of(first, second));

        ScreeningBatchRankingResponse response = rankingService.getRanking(100L, authentication);

        assertEquals(100L, response.screeningBatchId());
        assertEquals(200L, response.jobDescriptionId());
        assertEquals(2, response.candidatesRanked());
        assertEquals("Bob", response.rankings().get(0).candidateName());
        assertEquals("Alice", response.rankings().get(1).candidateName());
    }

    @Test
    void getRankingThrowsNotFoundWhenBatchHasNeverBeenAnalyzed() {
        when(screeningBatchService.resolveOwnedBatch(100L, authentication)).thenReturn(batch);
        when(rankingRepository.findByScreeningBatchId(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> rankingService.getRanking(100L, authentication));

        verify(rankingEntryRepository, never()).findByRankingIdOrderByRankPositionAsc(any());
    }

    @Test
    void getRankingPropagatesRejectionFromBatchOwnershipResolution() {
        when(screeningBatchService.resolveOwnedBatch(100L, authentication))
                .thenThrow(new ResourceNotFoundException("No screening batch found for the given id"));

        assertThrows(ResourceNotFoundException.class,
                () -> rankingService.getRanking(100L, authentication));

        verify(rankingRepository, never()).findByScreeningBatchId(any());
    }

    private Resume resume(User owner, String candidateName, String extractedText, Long id) {
        Resume resume = new Resume(owner, candidateName, id + "-resume.pdf", ResumeFileType.PDF, extractedText, Instant.now());
        setId(resume, id);
        return resume;
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
