package com.resumeanalyzer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The persisted result of running the matching/scoring engine and the
 * deterministic ATS analysis for one (Resume, JobDescription) pair,
 * requested by an authenticated Job Seeker.
 *
 * No separate Candidate entity is introduced - this operates purely on
 * the existing Resume and JobDescription entities, both already scoped
 * to an owning User (Phase 2). {@code owner} here is redundant with
 * {@code resume.owner}/{@code jobDescription.owner} (which are enforced
 * to be the same authenticated caller before an Analysis is created -
 * see AnalysisService), but is stored directly for straightforward,
 * efficient ownership checks on read-back without needing to join
 * through Resume/JobDescription.
 */
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @ElementCollection
    @CollectionTable(name = "analysis_matched_skills", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill")
    private Set<String> matchedSkills = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "analysis_missing_skills", joinColumns = @JoinColumn(name = "analysis_id"))
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

    @Lob
    @Column(name = "score_explanation", nullable = false)
    private String scoreExplanation;

    @ElementCollection
    @CollectionTable(name = "analysis_ats_issues", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "issue")
    @OrderColumn(name = "issue_order")
    private List<String> atsIssues = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_formatting_issues", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "issue")
    @OrderColumn(name = "issue_order")
    private List<String> formattingIssues = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_content_suggestions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "suggestion")
    @OrderColumn(name = "suggestion_order")
    private List<String> contentSuggestions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_interview_questions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "question")
    @OrderColumn(name = "question_order")
    private List<String> interviewQuestions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // --- Phase 5: Gemini-generated enhancement (additive; Phase 3/4 fields
    // above are untouched and always populated exactly as before) ---

    @ElementCollection
    @CollectionTable(name = "analysis_ai_advanced_ats_issues", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "issue")
    @OrderColumn(name = "issue_order")
    private List<String> aiAdvancedAtsIssues = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_ai_formatting_insights", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "insight")
    @OrderColumn(name = "insight_order")
    private List<String> aiFormattingInsights = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_ai_content_suggestions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "suggestion")
    @OrderColumn(name = "suggestion_order")
    private List<String> aiContentSuggestions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "analysis_ai_interview_questions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "question")
    @OrderColumn(name = "question_order")
    private List<String> aiInterviewQuestions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_enhancement_status", nullable = false, length = 20)
    private AiEnhancementStatus aiEnhancementStatus = AiEnhancementStatus.DISABLED;

    protected Analysis() {
        // required by JPA
    }

    public Analysis(User owner,
                     Resume resume,
                     JobDescription jobDescription,
                     Set<String> matchedSkills,
                     Set<String> missingSkills,
                     double skillScore,
                     double experienceScore,
                     double semanticScore,
                     double finalScore,
                     String scoreExplanation,
                     List<String> atsIssues,
                     List<String> formattingIssues,
                     List<String> contentSuggestions,
                     List<String> interviewQuestions,
                     Instant createdAt) {
        this.owner = owner;
        this.resume = resume;
        this.jobDescription = jobDescription;
        this.matchedSkills = new LinkedHashSet<>(matchedSkills);
        this.missingSkills = new LinkedHashSet<>(missingSkills);
        this.skillScore = skillScore;
        this.experienceScore = experienceScore;
        this.semanticScore = semanticScore;
        this.finalScore = finalScore;
        this.scoreExplanation = scoreExplanation;
        this.atsIssues = new ArrayList<>(atsIssues);
        this.formattingIssues = new ArrayList<>(formattingIssues);
        this.contentSuggestions = new ArrayList<>(contentSuggestions);
        this.interviewQuestions = new ArrayList<>(interviewQuestions);
        this.createdAt = createdAt;
    }

    /**
     * Applies the Phase 5 Gemini enhancement result to this analysis.
     *
     * Kept as a separate, single-call mutator (rather than an additional
     * constructor overload) precisely so the original constructor above -
     * and every existing caller of it - is completely unaffected; the
     * AI-enhancement fields simply keep their safe {@code DISABLED}/empty
     * defaults until this is called. Intended to be called at most once,
     * by {@code AnalysisService}, before the analysis is first persisted.
     */
    public void applyAiEnhancement(List<String> aiAdvancedAtsIssues,
                                    List<String> aiFormattingInsights,
                                    List<String> aiContentSuggestions,
                                    List<String> aiInterviewQuestions,
                                    AiEnhancementStatus aiEnhancementStatus) {
        this.aiAdvancedAtsIssues = new ArrayList<>(aiAdvancedAtsIssues);
        this.aiFormattingInsights = new ArrayList<>(aiFormattingInsights);
        this.aiContentSuggestions = new ArrayList<>(aiContentSuggestions);
        this.aiInterviewQuestions = new ArrayList<>(aiInterviewQuestions);
        this.aiEnhancementStatus = aiEnhancementStatus;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Resume getResume() {
        return resume;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
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

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public List<String> getAtsIssues() {
        return atsIssues;
    }

    public List<String> getFormattingIssues() {
        return formattingIssues;
    }

    public List<String> getContentSuggestions() {
        return contentSuggestions;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getAiAdvancedAtsIssues() {
        return aiAdvancedAtsIssues;
    }

    public List<String> getAiFormattingInsights() {
        return aiFormattingInsights;
    }

    public List<String> getAiContentSuggestions() {
        return aiContentSuggestions;
    }

    public List<String> getAiInterviewQuestions() {
        return aiInterviewQuestions;
    }

    public AiEnhancementStatus getAiEnhancementStatus() {
        return aiEnhancementStatus;
    }
}
