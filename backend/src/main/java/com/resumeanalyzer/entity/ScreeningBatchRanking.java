package com.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * The result of running Recruiter candidate ranking (Phase 7) for one
 * {@link ScreeningBatch} against one {@link JobDescription}.
 *
 * There is deliberately no separate "Candidate" entity, per the Phase 6
 * design note on {@link ScreeningBatch}: a ranked candidate is simply an
 * existing {@link Resume} that belongs to the batch, scored against the
 * job description. The per-candidate results live on
 * {@link ScreeningBatchRankingEntry}, one-directionally linked back to
 * this header row (the same one-directional-FK style already used
 * throughout this codebase - e.g. {@code Analysis} -> {@code Resume} -
 * to avoid bidirectional collections and eager-loading pitfalls).
 *
 * At most one ranking exists per batch at a time (see the unique
 * constraint on {@code screening_batch_id}): re-running
 * POST /api/screening-batches/{id}/analyze against the same batch simply
 * replaces the previous ranking (old header + entries are deleted first
 * by {@code ScreeningBatchRankingService}), rather than accumulating a
 * history. Nothing in Phase 7's requirements calls for ranking history,
 * and always exposing exactly the latest result keeps
 * GET /api/screening-batches/{id}/ranking's behavior simple and
 * deterministic - there is never more than one "current" answer to
 * "what does the ranking currently look like".
 *
 * {@code candidatesRanked} is stored directly (rather than always
 * derived by counting entries) so it remains correct and cheap to read
 * even for an empty batch/zero-successfully-processed-resumes result (an
 * explicit, valid outcome - see the service javadoc - not an error), and
 * so a caller can distinguish "analysis was run but produced no ranked
 * candidates" (this row exists, {@code candidatesRanked == 0}) from
 * "analysis was never run for this batch" (no row exists -> 404).
 */
@Entity
@Table(name = "screening_batch_rankings",
        uniqueConstraints = @UniqueConstraint(name = "uk_screening_batch_rankings_batch_id", columnNames = "screening_batch_id"))
public class ScreeningBatchRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_batch_id", nullable = false)
    private ScreeningBatch screeningBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @Column(name = "candidates_ranked", nullable = false)
    private int candidatesRanked;

    protected ScreeningBatchRanking() {
        // required by JPA
    }

    public ScreeningBatchRanking(ScreeningBatch screeningBatch,
                                  JobDescription jobDescription,
                                  Instant analyzedAt,
                                  int candidatesRanked) {
        this.screeningBatch = screeningBatch;
        this.jobDescription = jobDescription;
        this.analyzedAt = analyzedAt;
        this.candidatesRanked = candidatesRanked;
    }

    public Long getId() {
        return id;
    }

    public ScreeningBatch getScreeningBatch() {
        return screeningBatch;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public int getCandidatesRanked() {
        return candidatesRanked;
    }
}
