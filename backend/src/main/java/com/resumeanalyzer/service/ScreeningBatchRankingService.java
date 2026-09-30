package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.CandidateRankingEntryResponse;
import com.resumeanalyzer.dto.ScreeningBatchAnalyzeRequest;
import com.resumeanalyzer.dto.ScreeningBatchRankingResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ScreeningBatch;
import com.resumeanalyzer.entity.ScreeningBatchRanking;
import com.resumeanalyzer.entity.ScreeningBatchRankingEntry;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 7 - Recruiter Candidate Ranking: analyzing every successfully
 * uploaded {@link Resume} in a {@link ScreeningBatch} against a selected
 * {@link JobDescription}, and retrieving the resulting ranking.
 *
 * Reuses the Phase 3 {@link MatchScoringEngine} directly (the
 * framework-agnostic, reusable scoring component - see its own javadoc),
 * exactly as {@code MatchService} and {@code AnalysisService} already do,
 * so no scoring logic is duplicated. Reuses
 * {@link ScreeningBatchService#resolveOwnedBatch} directly for the same
 * reason: Recruiter-only access and strict batch ownership isolation
 * (identical 404 for both nonexistent and not-owned) must behave
 * identically here as they do for the Phase 6 endpoints, so this service
 * does not re-implement that check.
 *
 * <h2>Ranking lifecycle</h2>
 * At most one ranking exists per batch. Calling
 * {@link #analyze(Long, ScreeningBatchAnalyzeRequest, Authentication)}
 * replaces any previous ranking for that batch outright - there is no
 * versioning/history requirement, so the simplest, most predictable
 * design is for GET .../ranking to always describe exactly the most
 * recent analysis run.
 *
 * <h2>Empty batches and all-failed batches</h2>
 * A batch with zero resumes (either because none were ever successfully
 * uploaded, or all uploaded files failed to parse in Phase 6) is a valid,
 * deterministic input to analyze: the ranking is created with
 * {@code candidatesRanked == 0} and an empty entry list, exactly as it
 * would be for any other batch - this is not an error. This is
 * distinguished from "this batch has never been analyzed at all", which
 * has no ranking row and is reported as 404 by {@link #getRanking}.
 *
 * <h2>Job description ownership</h2>
 * The selected job description must belong to the same Recruiter who
 * owns the batch (already established, by the time this check runs, to
 * be the authenticated caller - see {@link ScreeningBatchService#resolveOwnedBatch}).
 * A nonexistent or foreign job description id is rejected identically
 * (404, generic message), for the same no-existence-leak reason as batch
 * resolution.
 */
@Service
public class ScreeningBatchRankingService {

    private final ScreeningBatchService screeningBatchService;
    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ScreeningBatchRankingRepository rankingRepository;
    private final ScreeningBatchRankingEntryRepository rankingEntryRepository;
    private final MatchScoringEngine matchScoringEngine;

    public ScreeningBatchRankingService(ScreeningBatchService screeningBatchService,
                                         ResumeRepository resumeRepository,
                                         JobDescriptionRepository jobDescriptionRepository,
                                         ScreeningBatchRankingRepository rankingRepository,
                                         ScreeningBatchRankingEntryRepository rankingEntryRepository,
                                         MatchScoringEngine matchScoringEngine) {
        this.screeningBatchService = screeningBatchService;
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.rankingRepository = rankingRepository;
        this.rankingEntryRepository = rankingEntryRepository;
        this.matchScoringEngine = matchScoringEngine;
    }

    @Transactional
    public ScreeningBatchRankingResponse analyze(Long batchId,
                                                   ScreeningBatchAnalyzeRequest request,
                                                   Authentication authentication) {
        ScreeningBatch batch = screeningBatchService.resolveOwnedBatch(batchId, authentication);
        JobDescription jobDescription = resolveOwnedJobDescription(request.getJobDescriptionId(), batch);

        List<Resume> resumes = resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(batch.getId());

        List<ScoredCandidate> scored = resumes.stream()
                .map(resume -> new ScoredCandidate(
                        resume,
                        matchScoringEngine.score(resume.getExtractedText(), jobDescription.getContent())))
                .sorted(Comparator
                        .comparingDouble((ScoredCandidate sc) -> sc.result().finalScore()).reversed()
                        .thenComparing(sc -> sc.resume().getId()))
                .toList();

        replaceExistingRanking(batch.getId());

        ScreeningBatchRanking ranking = rankingRepository.save(
                new ScreeningBatchRanking(batch, jobDescription, Instant.now(), scored.size()));

        List<ScreeningBatchRankingEntry> entries = saveEntries(ranking, scored);

        return toResponse(ranking, entries);
    }

    @Transactional(readOnly = true)
    public ScreeningBatchRankingResponse getRanking(Long batchId, Authentication authentication) {
        ScreeningBatch batch = screeningBatchService.resolveOwnedBatch(batchId, authentication);

        ScreeningBatchRanking ranking = rankingRepository.findByScreeningBatchId(batch.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No ranking has been generated yet for this screening batch"));

        List<ScreeningBatchRankingEntry> entries =
                rankingEntryRepository.findByRankingIdOrderByRankPositionAsc(ranking.getId());

        return toResponse(ranking, entries);
    }

    private JobDescription resolveOwnedJobDescription(Long jobDescriptionId, ScreeningBatch batch) {
        return jobDescriptionRepository.findByIdFetchingOwner(jobDescriptionId)
                .filter(jd -> jd.getOwner().getId().equals(batch.getOwner().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No job description found for the given id"));
    }

    /**
     * Clears out any ranking already recorded for this batch (header row
     * and its entries) before a fresh analysis run replaces it. Explicit,
     * ordered deletion (entries before the header) rather than a cascade
     * annotation, consistent with this codebase's one-directional-FK
     * style (see {@code ScreeningBatchRanking}).
     *
     * The deletions are flushed immediately, in the same transaction,
     * rather than left pending until the transaction later flushes on
     * its own. Hibernate does not necessarily execute pending DML in the
     * chronological order it was requested - within one flush it groups
     * actions by type (e.g. all inserts, then all deletes), so without an
     * explicit flush here, the new ranking's INSERT (further down in
     * {@link #analyze}) could be sent to the database before this
     * existing row's DELETE, transiently violating the one-ranking-per-
     * batch unique constraint even though the two statements are
     * logically sequential. Flushing here forces the DELETE to be
     * physically applied first, so the constraint is never actually
     * violated - the unique constraint itself is left exactly as-is.
     */
    private void replaceExistingRanking(Long batchId) {
        rankingRepository.findByScreeningBatchId(batchId).ifPresent(existing -> {
            rankingEntryRepository.deleteByRankingId(existing.getId());
            rankingRepository.delete(existing);
            rankingRepository.flush();
        });
    }

    private List<ScreeningBatchRankingEntry> saveEntries(ScreeningBatchRanking ranking, List<ScoredCandidate> scored) {
        List<ScreeningBatchRankingEntry> entries = new ArrayList<>(scored.size());
        int rankPosition = 1;
        for (ScoredCandidate sc : scored) {
            ScreeningBatchRankingEntry entry = new ScreeningBatchRankingEntry(
                    ranking,
                    sc.resume(),
                    sc.resume().getCandidateName(),
                    rankPosition++,
                    sc.result().matchedSkills(),
                    sc.result().missingSkills(),
                    sc.result().skillScore(),
                    sc.result().experienceScore(),
                    sc.result().semanticScore(),
                    sc.result().finalScore());
            entries.add(rankingEntryRepository.save(entry));
        }
        return entries;
    }

    private ScreeningBatchRankingResponse toResponse(ScreeningBatchRanking ranking,
                                                       List<ScreeningBatchRankingEntry> entries) {
        List<CandidateRankingEntryResponse> rankings = entries.stream()
                .map(this::toEntryResponse)
                .toList();

        return new ScreeningBatchRankingResponse(
                ranking.getScreeningBatch().getId(),
                ranking.getJobDescription().getId(),
                ranking.getAnalyzedAt(),
                ranking.getCandidatesRanked(),
                rankings);
    }

    private CandidateRankingEntryResponse toEntryResponse(ScreeningBatchRankingEntry entry) {
        return new CandidateRankingEntryResponse(
                entry.getRankPosition(),
                entry.getResume().getId(),
                entry.getCandidateName(),
                entry.getFinalScore(),
                entry.getSkillScore(),
                entry.getExperienceScore(),
                entry.getSemanticScore(),
                entry.getMatchedSkills(),
                entry.getMissingSkills());
    }

    /**
     * A resume scored against the selected job description, prior to
     * being sorted and persisted as a {@link ScreeningBatchRankingEntry}.
     */
    private record ScoredCandidate(Resume resume, MatchScoringEngine.MatchScoreResult result) {
    }
}
