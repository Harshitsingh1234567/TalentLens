package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for Resume entities.
 *
 * Phase 2 only creates resumes (POST /api/resumes) - there is no
 * read-back endpoint yet, so no query methods beyond what
 * {@link JpaRepository} already provides are defined here, except for
 * {@link #findByIdFetchingOwner(Long)}: {@code Resume.owner} is a LAZY
 * association (intentionally, to avoid an unnecessary join on every
 * resume load), so any caller that needs the owner populated after the
 * loading transaction has ended must fetch it explicitly via a JOIN
 * FETCH query rather than triggering lazy initialization later, which
 * would fail with a LazyInitializationException.
 *
 * {@link #findByScreeningBatchIdOrderByUploadedAtAsc(Long)} (Phase 6)
 * backs GET /api/screening-batches/{id}. It intentionally does not fetch
 * {@code owner} - the caller already knows the owner is the batch's own
 * owner, and the response mapping never touches
 * {@code resume.getOwner()} - so no join is needed and no N+1 risk
 * exists (a single query returns every resume in the batch).
 *
 * {@link #findByOwnerIdOrderByUploadedAtDesc(Long)} backs
 * GET /api/resumes, for the same reason: the caller (ResumeService)
 * already knows the owner is the authenticated user, and
 * {@code ResumeSummaryResponse} never touches {@code resume.getOwner()},
 * so this is a plain, unjoined, owner-filtered query.
 */
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    @Query("SELECT r FROM Resume r JOIN FETCH r.owner WHERE r.id = :id")
    Optional<Resume> findByIdFetchingOwner(@Param("id") Long id);

    List<Resume> findByScreeningBatchIdOrderByUploadedAtAsc(Long screeningBatchId);

    List<Resume> findByOwnerIdOrderByUploadedAtDesc(Long ownerId);
}
