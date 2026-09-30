package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.JobDescriptionRequest;
import com.resumeanalyzer.dto.JobDescriptionResponse;
import com.resumeanalyzer.dto.JobDescriptionSummaryResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for POST /api/job-descriptions.
 *
 * Phase 2 scope: persists the raw submitted JD text, associated with the
 * authenticated caller. No NLP/keyword extraction is performed here.
 */
@Service
public class JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final CurrentUserService currentUserService;

    public JobDescriptionService(JobDescriptionRepository jobDescriptionRepository,
                                  CurrentUserService currentUserService) {
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public JobDescriptionResponse submit(JobDescriptionRequest request, Authentication authentication) {
        User owner = currentUserService.resolve(authentication);

        JobDescription jobDescription = new JobDescription(owner, request.getContent().trim(), Instant.now());
        JobDescription saved = jobDescriptionRepository.save(jobDescription);

        return new JobDescriptionResponse(saved.getId(), saved.getContent(), saved.getSubmittedAt());
    }

    /**
     * Backs GET /api/job-descriptions: every job description owned by the
     * authenticated caller, most recently submitted first, mapped to the
     * minimal {@link JobDescriptionSummaryResponse} shape a selector UI
     * needs. Never fetches or filters another user's job descriptions -
     * the owner id used to scope the query is resolved server-side from
     * the validated JWT, exactly as {@link #submit} already does, never
     * from client-supplied input.
     */
    @Transactional(readOnly = true)
    public List<JobDescriptionSummaryResponse> listOwnJobDescriptions(Authentication authentication) {
        User owner = currentUserService.resolve(authentication);

        return jobDescriptionRepository.findByOwnerIdOrderBySubmittedAtDesc(owner.getId()).stream()
                .map(jobDescription -> new JobDescriptionSummaryResponse(
                        jobDescription.getId(),
                        buildPreview(jobDescription.getContent()),
                        jobDescription.getSubmittedAt()))
                .toList();
    }

    /**
     * A short, single-line excerpt of a job description's free-form
     * content for use in a selector list, since {@code JobDescription}
     * has no title field. Collapses all whitespace (including embedded
     * newlines from the originally pasted text) to single spaces and
     * truncates to {@value #PREVIEW_MAX_LENGTH} characters, appending an
     * ellipsis when truncated, so the list stays scannable regardless of
     * how long the original submission was.
     */
    private static final int PREVIEW_MAX_LENGTH = 140;

    private String buildPreview(String content) {
        String collapsed = content.trim().replaceAll("\\s+", " ");
        if (collapsed.length() <= PREVIEW_MAX_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, PREVIEW_MAX_LENGTH).stripTrailing() + "\u2026";
    }
}
