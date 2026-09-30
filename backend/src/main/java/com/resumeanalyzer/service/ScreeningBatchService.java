package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ScreeningBatchFileFailure;
import com.resumeanalyzer.dto.ScreeningBatchResponse;
import com.resumeanalyzer.dto.ScreeningBatchResumeSummary;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.ScreeningBatch;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.exception.ResumeProcessingException;
import com.resumeanalyzer.exception.ScreeningBatchValidationException;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6 - Recruiter Screening Batch: creating a named, Recruiter-owned
 * batch together with a bulk resume upload, and retrieving one.
 *
 * This service only creates the batch and associates uploaded
 * {@link Resume} rows with it - see the locked design note on
 * {@link ScreeningBatch} for why no separate Candidate entity or
 * JobDescription association exists. Candidate ranking/matching against a
 * job description (Phase 7) is handled by the separate
 * {@code ScreeningBatchRankingService}, which reuses
 * {@link #resolveOwnedBatch} rather than duplicating this class's
 * role/ownership logic.
 *
 * <h2>Bulk-upload transactional design</h2>
 * A batch-create request is rejected outright (HTTP 400, nothing
 * persisted) only for request-level problems that make the request as a
 * whole meaningless: a blank/oversized name, zero files, or more than
 * {@link #MAX_FILES_PER_BATCH} files. Once past that gate, the batch is
 * always created, and every file is then attempted independently: a file
 * that fails to parse (empty, unsupported extension, corrupted, no
 * extractable text - i.e. exactly what {@link ResumeParsingService}
 * already validates for the single-resume Phase 2 upload) is recorded as
 * a {@link ScreeningBatchFileFailure} and simply not persisted, rather
 * than aborting the whole batch and discarding every other, valid file in
 * the same request. This was a deliberate choice over strict
 * all-or-nothing: a single corrupted resume out of, say, fifty should not
 * cost a Recruiter the other forty-nine. Nothing is ever silently
 * dropped - every file's outcome (persisted resume, or failure with a
 * reason) is reported in the response - and nothing is silently
 * mis-associated, since a resume is only ever linked to the batch after
 * it has successfully parsed.
 */
@Service
public class ScreeningBatchService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningBatchService.class);

    /**
     * Defensive upper bound on files per request. Not a rate limiter or
     * queueing mechanism - just a sanity bound on a single multipart
     * request, matching the multipart size limits in application.yml.
     */
    static final int MAX_FILES_PER_BATCH = 50;

    private final ScreeningBatchRepository screeningBatchRepository;
    private final ResumeRepository resumeRepository;
    private final CurrentUserService currentUserService;
    private final ResumeParsingService resumeParsingService;

    public ScreeningBatchService(ScreeningBatchRepository screeningBatchRepository,
                                  ResumeRepository resumeRepository,
                                  CurrentUserService currentUserService,
                                  ResumeParsingService resumeParsingService) {
        this.screeningBatchRepository = screeningBatchRepository;
        this.resumeRepository = resumeRepository;
        this.currentUserService = currentUserService;
        this.resumeParsingService = resumeParsingService;
    }

    @Transactional
    public ScreeningBatchResponse createBatch(String name, List<MultipartFile> files, Authentication authentication) {
        User owner = requireRecruiter(authentication);

        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new ScreeningBatchValidationException("Batch name must not be blank");
        }
        if (trimmedName.length() > 255) {
            throw new ScreeningBatchValidationException("Batch name must not exceed 255 characters");
        }
        if (files == null || files.isEmpty()) {
            throw new ScreeningBatchValidationException("At least one resume file must be provided");
        }
        if (files.size() > MAX_FILES_PER_BATCH) {
            throw new ScreeningBatchValidationException(
                    "A screening batch cannot contain more than " + MAX_FILES_PER_BATCH + " files in a single request");
        }

        ScreeningBatch batch = screeningBatchRepository.save(new ScreeningBatch(owner, trimmedName, Instant.now()));

        List<ScreeningBatchResumeSummary> resumeSummaries = new ArrayList<>();
        List<ScreeningBatchFileFailure> failures = new ArrayList<>();

        for (MultipartFile file : files) {
            String displayName = displayName(file.getOriginalFilename());
            try {
                ResumeParsingService.ParsedResume parsed = resumeParsingService.parse(file);
                Resume resume = new Resume(
                        owner,
                        deriveCandidateName(file.getOriginalFilename()),
                        file.getOriginalFilename(),
                        parsed.fileType(),
                        parsed.extractedText(),
                        Instant.now());
                resume.assignToBatch(batch);
                resumeSummaries.add(toSummary(resumeRepository.save(resume)));
            } catch (ResumeProcessingException ex) {
                log.info("Screening batch {}: file '{}' rejected ({})", batch.getId(), displayName, ex.getMessage());
                failures.add(new ScreeningBatchFileFailure(displayName, ex.getMessage()));
            }
        }

        return toResponse(batch, resumeSummaries, failures);
    }

    @Transactional(readOnly = true)
    public ScreeningBatchResponse getBatch(Long batchId, Authentication authentication) {
        ScreeningBatch batch = resolveOwnedBatch(batchId, authentication);

        List<ScreeningBatchResumeSummary> resumes = resumeRepository
                .findByScreeningBatchIdOrderByUploadedAtAsc(batch.getId())
                .stream()
                .map(this::toSummary)
                .toList();

        return toResponse(batch, resumes, List.of());
    }

    /**
     * Package-private (not private) so {@code ScreeningBatchRankingService}
     * (Phase 7) can reuse the exact same role-check + ownership-resolution
     * logic for POST .../analyze and GET .../ranking, rather than
     * duplicating it - the same "reuse, don't duplicate" principle already
     * applied to {@code MatchScoringEngine}. No behavior change: this is a
     * visibility-only edit to an otherwise untouched, already-tested
     * method.
     */
    ScreeningBatch resolveOwnedBatch(Long batchId, Authentication authentication) {
        User caller = requireRecruiter(authentication);

        return screeningBatchRepository.findByIdFetchingOwner(batchId)
                .filter(batch -> batch.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No screening batch found for the given id"));
    }

    /**
     * Only a Recruiter may create or manage (read/write) screening
     * batches. Checked before any batch lookup, so a Job Seeker's request
     * is rejected uniformly regardless of which batch id they pass -
     * there is never a scenario in which a Job Seeker legitimately
     * interacts with this resource type.
     *
     * Package-private for the same reason as {@link #resolveOwnedBatch} -
     * reused directly by {@code ScreeningBatchRankingService} (Phase 7).
     */
    User requireRecruiter(Authentication authentication) {
        User caller = currentUserService.resolve(authentication);
        if (caller.getRole() != Role.RECRUITER) {
            throw new AccessDeniedException("Only recruiters may create or manage screening batches");
        }
        return caller;
    }

    /**
     * Best-effort candidate label derived from the uploaded filename
     * (extension stripped) when no other name is available - a
     * screening-batch overview of resumes named only "resume1.pdf",
     * "resume2.pdf", etc. would otherwise be of little use to the
     * Recruiter. Never fails: an unusable filename simply yields a null
     * candidate name, exactly as an unset one from Phase 2 already does.
     */
    private String deriveCandidateName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        int lastDot = originalFilename.lastIndexOf('.');
        String base = lastDot > 0 ? originalFilename.substring(0, lastDot) : originalFilename;
        return base.isBlank() ? null : base.trim();
    }

    private String displayName(String originalFilename) {
        return (originalFilename == null || originalFilename.isBlank()) ? "(unnamed file)" : originalFilename;
    }

    private ScreeningBatchResumeSummary toSummary(Resume resume) {
        return new ScreeningBatchResumeSummary(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getFileType(),
                resume.getCandidateName(),
                resume.getUploadedAt());
    }

    private ScreeningBatchResponse toResponse(ScreeningBatch batch,
                                               List<ScreeningBatchResumeSummary> resumes,
                                               List<ScreeningBatchFileFailure> failures) {
        return new ScreeningBatchResponse(batch.getId(), batch.getName(), batch.getCreatedAt(), resumes, failures);
    }
}
