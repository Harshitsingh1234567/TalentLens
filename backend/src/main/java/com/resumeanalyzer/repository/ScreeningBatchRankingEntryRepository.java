package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.ScreeningBatchRankingEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence access for {@link ScreeningBatchRankingEntry} rows
 * (Phase 7): the ranked candidates belonging to one
 * {@code ScreeningBatchRanking}.
 *
 * {@link #findByRankingIdOrderByRankPositionAsc(Long)} backs
 * GET /api/screening-batches/{id}/ranking - a single query returns every
 * entry for the current ranking, already in rank order, with no join on
 * {@code resume} needed (the response mapper only reads
 * {@code resume.getId()}, always safe on a lazy proxy - see
 * {@code ScreeningBatchRankingService}), so there is no N+1 risk.
 *
 * {@link #deleteByRankingId(Long)} lets {@code ScreeningBatchRankingService}
 * explicitly clear out a previous ranking's entries before replacing it,
 * consistent with this codebase's one-directional-FK style (no cascade
 * is declared on the entity, so deletion is always explicit and
 * intentional rather than implicit).
 */
public interface ScreeningBatchRankingEntryRepository extends JpaRepository<ScreeningBatchRankingEntry, Long> {

    List<ScreeningBatchRankingEntry> findByRankingIdOrderByRankPositionAsc(Long rankingId);

    void deleteByRankingId(Long rankingId);
}
