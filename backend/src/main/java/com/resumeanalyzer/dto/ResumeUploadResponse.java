package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.ResumeFileType;

import java.time.Instant;

/**
 * Response payload for a successful POST /api/resumes.
 *
 * Exposes the persisted, owner-scoped record and its raw extracted text.
 * No section segmentation or skill/keyword data is included in Phase 2.
 */
public class ResumeUploadResponse {

    private final Long id;
    private final String originalFilename;
    private final ResumeFileType fileType;
    private final String candidateName;
    private final Instant uploadedAt;
    private final String extractedText;

    public ResumeUploadResponse(Long id,
                                 String originalFilename,
                                 ResumeFileType fileType,
                                 String candidateName,
                                 Instant uploadedAt,
                                 String extractedText) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.candidateName = candidateName;
        this.uploadedAt = uploadedAt;
        this.extractedText = extractedText;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public ResumeFileType getFileType() {
        return fileType;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getExtractedText() {
        return extractedText;
    }
}
