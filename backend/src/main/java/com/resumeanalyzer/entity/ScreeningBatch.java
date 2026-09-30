package com.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A named, Recruiter-owned container for a bulk resume-screening run
 * (Phase 6).
 *
 * Per the locked design, there is deliberately no separate "Candidate"
 * entity: a candidate in a batch is simply an existing {@link Resume} row
 * whose {@code screeningBatch} foreign key points at this entity (see
 * {@link Resume#assignToBatch}). This entity therefore only needs to
 * capture the batch's own identity (owner + name + creation time) - the
 * membership relationship lives entirely on the {@code Resume} side, the
 * same one-directional-FK style already used throughout this codebase
 * (e.g. {@code Analysis} -> {@code Resume}/{@code JobDescription}) to
 * avoid bidirectional collections and the N+1/eager-loading pitfalls they
 * invite.
 *
 * Deliberately excluded from Phase 6 (see class-level scope notes in the
 * service layer): any association to a {@code JobDescription}. Candidate
 * ranking/matching against a job description is explicitly a Phase 7
 * concern, and Phase 7 may need that association shaped differently (e.g.
 * supplied per ranking run rather than fixed at batch-creation time), so
 * it is not guessed at here.
 */
@Entity
@Table(name = "screening_batches", indexes = {
        @Index(name = "idx_screening_batches_user_id", columnList = "user_id")
})
public class ScreeningBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ScreeningBatch() {
        // required by JPA
    }

    public ScreeningBatch(User owner, String name, Instant createdAt) {
        this.owner = owner;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
