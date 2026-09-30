package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.ScreeningBatchRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for {@link ScreeningBatchRanking} header rows
 * (Phase 7).
 *
 * At most one ranking exists per batch (see the unique constraint on the
 * entity), so {@link #findByScreeningBatchId(Long)} is the only lookup
 * needed: it backs both "does a ranking already exist, that should be
 * replaced?" (POST .../analyze) and "what is the current ranking?"
 * (GET .../ranking). No JOIN FETCH is used here - callers only ever read
 * {@code screeningBatch}/{@code jobDescription} via {@code getId()} on
 * the resulting entity, which is always safe on a lazy proxy without
 * triggering initialization (the same pattern already used by
 * {@code AnalysisService#toResponse}), so no join is needed and no N+1
 * risk exists.
 */
public interface ScreeningBatchRankingRepository extends JpaRepository<ScreeningBatchRanking, Long> {

    Optional<ScreeningBatchRanking> findByScreeningBatchId(Long screeningBatchId);
}
