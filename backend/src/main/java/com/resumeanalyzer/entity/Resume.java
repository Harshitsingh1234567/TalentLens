package com.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single uploaded resume (PDF or DOCX) and its raw extracted text.
 *
 * Phase 2 scope: this entity stores only the raw text produced by
 * {@link com.resumeanalyzer.service.ResumeParsingService} - it does not yet
 * hold section segmentation (Skills, Education, Experience, etc.), which
 * is deferred to the later matching/analysis pipeline.
 *
 * Every resume is tagged with the uploading (authenticated) user as its
 * owner. {@code candidateName} is an optional tag used when a resume is
 * uploaded on behalf of someone else (the recruiter batch-screening
 * workflow) - there is no separate Candidate entity; the candidate is
 * represented purely by this optional field, and, when the resume is part
 * of a Phase 6 screening batch, by its membership in that batch (see
 * {@link #screeningBatch}).
 */
@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resumes_screening_batch_id", columnList = "screening_batch_id")
})
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /**
     * Optional tag identifying the candidate this resume belongs to, when
     * uploaded on someone else's behalf. Null when a Job Seeker uploads
     * their own resume.
     */
    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private ResumeFileType fileType;

    /**
     * Raw text extracted from the uploaded file. No section segmentation
     * or skill/keyword extraction is performed on this text in Phase 2.
     *
     * Mapped with an explicit {@code columnDefinition} of LONGTEXT rather
     * than relying on {@code @Lob} alone: extracted resume text is
     * unbounded free-form text (large multi-page PDFs/DOCX files can
     * easily exceed a few hundred characters), and this field must never
     * again be silently created/left as a small-capacity text type (e.g.
     * TINYTEXT/255 chars) by schema generation - see the incident where
     * the live column had drifted to TINYTEXT, causing
     * "Data truncation: Data too long for column 'extracted_text'" on
     * ordinary, small resume uploads. Being explicit here removes any
     * dependency on dialect/Hibernate-version defaults for this critical
     * column.
     */
    @Lob
    @Column(name = "extracted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    // --- Phase 6: optional membership in a Recruiter's screening batch
    // (additive; every field/constructor above is untouched, so every
    // existing caller of the original constructor - including a Job
    // Seeker's own single-resume upload from Phase 2 - is unaffected and
    // simply leaves this null) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_batch_id")
    private ScreeningBatch screeningBatch;

    protected Resume() {
        // required by JPA
    }

    public Resume(User owner,
                  String candidateName,
                  String originalFilename,
                  ResumeFileType fileType,
                  String extractedText,
                  Instant uploadedAt) {
        this.owner = owner;
        this.candidateName = candidateName;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.extractedText = extractedText;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public ResumeFileType getFileType() {
        return fileType;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public ScreeningBatch getScreeningBatch() {
        return screeningBatch;
    }

    /**
     * Assigns this resume to a Recruiter's screening batch (Phase 6).
     *
     * A separate mutator - rather than a new constructor overload - for
     * the same reason as {@code Analysis.applyAiEnhancement} in Phase 5:
     * it keeps the original constructor, and every existing caller of it,
     * completely unaffected. Intended to be called at most once, by
     * {@code ScreeningBatchService}, before the resume is first persisted.
     */
    public void assignToBatch(ScreeningBatch screeningBatch) {
        this.screeningBatch = screeningBatch;
    }
}
