package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.ScreeningBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for ScreeningBatch entities.
 *
 * {@code owner} is a LAZY association (same rationale as {@code Resume.owner}
 * - see {@link ResumeRepository}), so {@link #findByIdFetchingOwner(Long)}
 * fetches it explicitly for the ownership check performed on every
 * batch read/write in {@code ScreeningBatchService}.
 */
public interface ScreeningBatchRepository extends JpaRepository<ScreeningBatch, Long> {

    @Query("SELECT b FROM ScreeningBatch b JOIN FETCH b.owner WHERE b.id = :id")
    Optional<ScreeningBatch> findByIdFetchingOwner(@Param("id") Long id);
}
