package com.resumeanalyzer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One ranked candidate (an existing {@link Resume} that belongs to the
 * batch) within a {@link ScreeningBatchRanking} (Phase 7).
 *
 * The per-candidate score fields mirror
 * {@link com.resumeanalyzer.service.matching.MatchScoringEngine.MatchScoreResult}
 * exactly - this entry only ever stores what that engine already
 * computed, never anything derived or re-interpreted, so the ranking is
 * fully explainable by the same engine used everywhere else in this
 * codebase.
 *
 * {@code candidateName} is copied from {@code Resume.candidateName} at
 * analysis time rather than read live through the {@code resume}
 * association on every future read. This keeps a past ranking result
 * self-contained and avoids ever needing to touch anything on
 * {@code Resume} beyond its id (see {@code ScreeningBatchRankingService}),
 * exactly the same lazy-association-id-only pattern already established
 * by {@code AnalysisService#toResponse}.
 *
 * {@code rankPosition} (1-based) is computed once, at analysis time, by
 * sorting on final score descending with resume id ascending as a
 * deterministic tie-breaker - not recomputed on every read - so
 * GET /api/screening-batches/{id}/ranking always returns entries in a
 * stable, reproducible order.
 */
@Entity
@Table(name = "screening_batch_ranking_entries", indexes = {
        @Index(name = "idx_screening_batch_ranking_entries_ranking_id", columnList = "ranking_id")
})
public class ScreeningBatchRankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ranking_id", nullable = false)
    private ScreeningBatchRanking ranking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @ElementCollection
    @CollectionTable(name = "screening_batch_ranking_entry_matched_skills", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "skill")
    private Set<String> matchedSkills = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "screening_batch_ranking_entry_missing_skills", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "skill")
    private Set<String> missingSkills = new LinkedHashSet<>();

    @Column(name = "skill_score", nullable = false)
    private double skillScore;

    @Column(name = "experience_score", nullable = false)
    private double experienceScore;

    @Column(name = "semantic_score", nullable = false)
    private double semanticScore;

    @Column(name = "final_score", nullable = false)
    private double finalScore;

    protected ScreeningBatchRankingEntry() {
        // required by JPA
    }

    public ScreeningBatchRankingEntry(ScreeningBatchRanking ranking,
                                       Resume resume,
                                       String candidateName,
                                       int rankPosition,
                                       Set<String> matchedSkills,
                                       Set<String> missingSkills,
                                       double skillScore,
                                       double experienceScore,
                                       double semanticScore,
                                       double finalScore) {
        this.ranking = ranking;
        this.resume = resume;
        this.candidateName = candidateName;
        this.rankPosition = rankPosition;
        this.matchedSkills = new LinkedHashSet<>(matchedSkills);
        this.missingSkills = new LinkedHashSet<>(missingSkills);
        this.skillScore = skillScore;
        this.experienceScore = experienceScore;
        this.semanticScore = semanticScore;
        this.finalScore = finalScore;
    }

    public Long getId() {
        return id;
    }

    public ScreeningBatchRanking getRanking() {
        return ranking;
    }

    public Resume getResume() {
        return resume;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public Set<String> getMatchedSkills() {
        return matchedSkills;
    }

    public Set<String> getMissingSkills() {
        return missingSkills;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public double getFinalScore() {
        return finalScore;
    }
}
