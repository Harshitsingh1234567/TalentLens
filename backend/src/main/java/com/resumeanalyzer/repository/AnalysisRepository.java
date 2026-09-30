package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for Analysis entities.
 *
 * {@link #findByIdFetchingOwner(Long)} mirrors the same pattern already
 * established for Resume/JobDescription (Phase 2): {@code Analysis.owner}
 * is a LAZY association, so any caller that needs it populated after the
 * loading transaction has ended must fetch it explicitly via JOIN FETCH.
 */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    @Query("SELECT a FROM Analysis a JOIN FETCH a.owner WHERE a.id = :id")
    Optional<Analysis> findByIdFetchingOwner(@Param("id") Long id);
}
