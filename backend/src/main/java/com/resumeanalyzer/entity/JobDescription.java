package com.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Job Description submitted (pasted) as plain text by either a Job
 * Seeker or an HR/Recruiter.
 *
 * Phase 2 scope: stores only the raw submitted text. Named Entity
 * Recognition (OpenNLP) and TF-IDF keyword/keyphrase extraction (Lucene)
 * are deliberately deferred to the later matching/analysis pipeline,
 * where that structured output is actually consumed.
 */
@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected JobDescription() {
        // required by JPA
    }

    public JobDescription(User owner, String content, Instant submittedAt) {
        this.owner = owner;
        this.content = content;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getContent() {
        return content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
