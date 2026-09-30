package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ResumeSummaryResponse;
import com.resumeanalyzer.dto.ResumeUploadResponse;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.ResumeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for POST /api/resumes.
 *
 * Phase 2 scope: extracts raw text from the uploaded file (via
 * {@link ResumeParsingService}) and persists it, associated with the
 * authenticated caller. No section segmentation or skill/keyword
 * extraction is performed here.
 */
@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParsingService resumeParsingService;
    private final CurrentUserService currentUserService;

    public ResumeService(ResumeRepository resumeRepository,
                          ResumeParsingService resumeParsingService,
                          CurrentUserService currentUserService) {
        this.resumeRepository = resumeRepository;
        this.resumeParsingService = resumeParsingService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ResumeUploadResponse upload(MultipartFile file, String candidateName, Authentication authentication) {
        User owner = currentUserService.resolve(authentication);

        ResumeParsingService.ParsedResume parsed = resumeParsingService.parse(file);

        Resume resume = new Resume(
                owner,
                normalizeCandidateName(candidateName),
                file.getOriginalFilename(),
                parsed.fileType(),
                parsed.extractedText(),
                Instant.now());

        Resume saved = resumeRepository.save(resume);

        return new ResumeUploadResponse(
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getFileType(),
                saved.getCandidateName(),
                saved.getUploadedAt(),
                saved.getExtractedText());
    }

    private String normalizeCandidateName(String candidateName) {
        if (candidateName == null || candidateName.isBlank()) {
            return null;
        }
        return candidateName.trim();
    }

    /**
     * Backs GET /api/resumes: every resume owned by the authenticated
     * caller, most recently uploaded first, mapped to the minimal
     * {@link ResumeSummaryResponse} shape a selector UI needs. Never
     * fetches or filters another user's resumes - the owner id used to
     * scope the query is resolved server-side from the validated JWT,
     * exactly as every other owner-scoped operation in this service
     * does, never from client-supplied input.
     */
    @Transactional(readOnly = true)
    public List<ResumeSummaryResponse> listOwnResumes(Authentication authentication) {
        User owner = currentUserService.resolve(authentication);

        return resumeRepository.findByOwnerIdOrderByUploadedAtDesc(owner.getId()).stream()
                .map(resume -> new ResumeSummaryResponse(
                        resume.getId(),
                        resume.getOriginalFilename(),
                        resume.getUploadedAt()))
                .toList();
    }
}
