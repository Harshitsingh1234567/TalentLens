package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AnalysisRequest;
import com.resumeanalyzer.dto.AnalysisResponse;
import com.resumeanalyzer.dto.InterviewQuestionsResponse;
import com.resumeanalyzer.entity.Analysis;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.analysis.AtsAnalyzer;
import com.resumeanalyzer.service.analysis.ContentSuggestionGenerator;
import com.resumeanalyzer.service.analysis.InterviewQuestionGenerator;
import com.resumeanalyzer.service.analysis.ai.AiAnalysisEnhancer;
import com.resumeanalyzer.service.analysis.ai.AiEnhancementRequest;
import com.resumeanalyzer.service.analysis.ai.AiEnhancementResult;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Business logic for POST /api/analysis and GET /api/analysis/{id}.
 *
 * Reuses the Phase 3 {@link MatchScoringEngine} directly (the actual
 * reusable scoring component - see its own javadoc) rather than the
 * HTTP/DTO-shaped {@link MatchService}, so no matching/scoring logic is
 * duplicated. Ownership resolution follows the same pattern already
 * established by {@link MatchService} for consistency, since each
 * service owns its own minimal "resolve and verify" step for its own
 * inputs.
 */
@Service
public class AnalysisService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AnalysisRepository analysisRepository;
    private final CurrentUserService currentUserService;
    private final MatchScoringEngine matchScoringEngine;
    private final AtsAnalyzer atsAnalyzer;
    private final ContentSuggestionGenerator contentSuggestionGenerator;
    private final InterviewQuestionGenerator interviewQuestionGenerator;
    private final AiAnalysisEnhancer aiAnalysisEnhancer;

    public AnalysisService(ResumeRepository resumeRepository,
                            JobDescriptionRepository jobDescriptionRepository,
                            AnalysisRepository analysisRepository,
                            CurrentUserService currentUserService,
                            MatchScoringEngine matchScoringEngine,
                            AtsAnalyzer atsAnalyzer,
                            ContentSuggestionGenerator contentSuggestionGenerator,
                            InterviewQuestionGenerator interviewQuestionGenerator,
                            AiAnalysisEnhancer aiAnalysisEnhancer) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.analysisRepository = analysisRepository;
        this.currentUserService = currentUserService;
        this.matchScoringEngine = matchScoringEngine;
        this.atsAnalyzer = atsAnalyzer;
        this.contentSuggestionGenerator = contentSuggestionGenerator;
        this.interviewQuestionGenerator = interviewQuestionGenerator;
        this.aiAnalysisEnhancer = aiAnalysisEnhancer;
    }

    @Transactional
    public AnalysisResponse createAnalysis(AnalysisRequest request, Authentication authentication) {
        User caller = currentUserService.resolve(authentication);

        Resume resume = resumeRepository.findByIdFetchingOwner(request.getResumeId())
                .filter(r -> r.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No resume found for the given id"));

        JobDescription jobDescription = jobDescriptionRepository.findByIdFetchingOwner(request.getJobDescriptionId())
                .filter(jd -> jd.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No job description found for the given id"));

        MatchScoringEngine.MatchScoreResult matchResult =
                matchScoringEngine.score(resume.getExtractedText(), jobDescription.getContent());

        Set<String> requiredSkills = new HashSet<>(matchResult.matchedSkills());
        requiredSkills.addAll(matchResult.missingSkills());

        AtsAnalyzer.AtsAnalysisResult atsResult =
                atsAnalyzer.analyze(resume.getExtractedText(), requiredSkills, matchResult.matchedSkills());

        Analysis analysis = new Analysis(
                caller,
                resume,
                jobDescription,
                matchResult.matchedSkills(),
                matchResult.missingSkills(),
                matchResult.skillScore(),
                matchResult.experienceScore(),
                matchResult.semanticScore(),
                matchResult.finalScore(),
                matchResult.explanation(),
                atsResult.atsIssues(),
                atsResult.formattingIssues(),
                contentSuggestionGenerator.generate(matchResult.missingSkills()),
                interviewQuestionGenerator.generate(matchResult.matchedSkills(), matchResult.missingSkills()),
                Instant.now());

        // Phase 5: additional Gemini-generated insights on top of the
        // deterministic result above. This never fails createAnalysis - any
        // AI-provider problem yields a safe fallback (empty AI sections) -
        // see AiAnalysisEnhancer.
        AiEnhancementResult aiResult = aiAnalysisEnhancer.enhance(new AiEnhancementRequest(
                resume.getExtractedText(), jobDescription.getContent(),
                matchResult.matchedSkills(), matchResult.missingSkills()));
        analysis.applyAiEnhancement(
                aiResult.advancedAtsIssues(),
                aiResult.formattingInsights(),
                aiResult.contentSuggestions(),
                aiResult.interviewQuestions(),
                aiResult.status());

        Analysis saved = analysisRepository.save(analysis);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(Long analysisId, Authentication authentication) {
        return toResponse(resolveOwnedAnalysis(analysisId, authentication));
    }

    /**
     * Backs GET /api/analysis/{id}/interview-questions - both the
     * deterministic (Phase 4) and, when available, the AI-generated
     * (Phase 5) interview questions already stored on the analysis. Reuses
     * the same ownership resolution as {@link #getAnalysis}, so this
     * cannot be used to read another user's data either.
     */
    @Transactional(readOnly = true)
    public InterviewQuestionsResponse getInterviewQuestions(Long analysisId, Authentication authentication) {
        Analysis analysis = resolveOwnedAnalysis(analysisId, authentication);
        return new InterviewQuestionsResponse(
                analysis.getId(),
                analysis.getInterviewQuestions(),
                analysis.getAiInterviewQuestions(),
                analysis.getAiEnhancementStatus());
    }

    private Analysis resolveOwnedAnalysis(Long analysisId, Authentication authentication) {
        User caller = currentUserService.resolve(authentication);

        return analysisRepository.findByIdFetchingOwner(analysisId)
                .filter(a -> a.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No analysis found for the given id"));
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getJobDescription().getId(),
                analysis.getMatchedSkills(),
                analysis.getMissingSkills(),
                analysis.getSkillScore(),
                analysis.getExperienceScore(),
                analysis.getSemanticScore(),
                analysis.getFinalScore(),
                analysis.getScoreExplanation(),
                analysis.getAtsIssues(),
                analysis.getFormattingIssues(),
                analysis.getContentSuggestions(),
                analysis.getInterviewQuestions(),
                analysis.getCreatedAt(),
                analysis.getAiAdvancedAtsIssues(),
                analysis.getAiFormattingInsights(),
                analysis.getAiContentSuggestions(),
                analysis.getAiInterviewQuestions(),
                analysis.getAiEnhancementStatus());
    }
}
