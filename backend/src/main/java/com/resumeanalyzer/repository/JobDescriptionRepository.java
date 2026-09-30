package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for JobDescription entities.
 *
 * Phase 2 only creates job descriptions (POST /api/job-descriptions) -
 * there is no read-back endpoint yet, so no query methods beyond what
 * {@link JpaRepository} already provides are defined here, except for
 * {@link #findByIdFetchingOwner(Long)}: {@code JobDescription.owner} is a
 * LAZY association (intentionally), so any caller that needs the owner
 * populated after the loading transaction has ended must fetch it
 * explicitly via a JOIN FETCH query rather than triggering lazy
 * initialization later, which would fail with a
 * LazyInitializationException.
 *
 * {@link #findByOwnerIdOrderBySubmittedAtDesc(Long)} backs
 * GET /api/job-descriptions: the caller (JobDescriptionService) already
 * knows the owner is the authenticated user, and
 * {@code JobDescriptionSummaryResponse} never touches
 * {@code jobDescription.getOwner()}, so this is a plain, unjoined,
 * owner-filtered query - no JOIN FETCH needed.
 */
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    @Query("SELECT jd FROM JobDescription jd JOIN FETCH jd.owner WHERE jd.id = :id")
    Optional<JobDescription> findByIdFetchingOwner(@Param("id") Long id);

    List<JobDescription> findByOwnerIdOrderBySubmittedAtDesc(Long ownerId);
}
