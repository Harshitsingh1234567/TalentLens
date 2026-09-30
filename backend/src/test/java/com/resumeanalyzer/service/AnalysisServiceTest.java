package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AnalysisRequest;
import com.resumeanalyzer.dto.AnalysisResponse;
import com.resumeanalyzer.entity.Analysis;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.ResumeFileType;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.entity.AiEnhancementStatus;
import com.resumeanalyzer.service.analysis.AtsAnalyzer;
import com.resumeanalyzer.service.analysis.ContentSuggestionGenerator;
import com.resumeanalyzer.service.analysis.InterviewQuestionGenerator;
import com.resumeanalyzer.service.analysis.ai.AiAnalysisEnhancer;
import com.resumeanalyzer.service.analysis.ai.AiEnhancementResult;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobDescriptionRepository jobDescriptionRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MatchScoringEngine matchScoringEngine;

    @Mock
    private AtsAnalyzer atsAnalyzer;

    @Mock
    private ContentSuggestionGenerator contentSuggestionGenerator;

    @Mock
    private InterviewQuestionGenerator interviewQuestionGenerator;

    @Mock
    private AiAnalysisEnhancer aiAnalysisEnhancer;

    @Mock
    private Authentication authentication;

    private AnalysisService analysisService;

    private User caller;
    private User otherUser;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(resumeRepository, jobDescriptionRepository, analysisRepository,
                currentUserService, matchScoringEngine, atsAnalyzer, contentSuggestionGenerator,
                interviewQuestionGenerator, aiAnalysisEnhancer);

        caller = new User("caller@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        caller.setId(1L);

        otherUser = new User("other@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        otherUser.setId(2L);
    }

    @Test
    void createAnalysisOrchestratesMatchingAtsAndPersistence() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "Java developer", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(caller, "Java engineer needed", Instant.now());
        setId(jobDescription, 20L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resume));
        when(jobDescriptionRepository.findByIdFetchingOwner(20L)).thenReturn(Optional.of(jobDescription));

        MatchScoringEngine.MatchScoreResult matchResult = new MatchScoringEngine.MatchScoreResult(
                Set.of("java"), Set.of("aws"), 0.5, 0.5, 0.5, 0.5, "explanation");
        when(matchScoringEngine.score("Java developer", "Java engineer needed")).thenReturn(matchResult);

        when(atsAnalyzer.analyze(eq("Java developer"), anySet(), eq(Set.of("java"))))
                .thenReturn(new AtsAnalyzer.AtsAnalysisResult(List.of("issue1"), List.of("format1")));
        when(contentSuggestionGenerator.generate(Set.of("aws"))).thenReturn(List.of("suggestion1"));
        when(interviewQuestionGenerator.generate(Set.of("java"), Set.of("aws"))).thenReturn(List.of("question1"));

        when(aiAnalysisEnhancer.enhance(any())).thenReturn(new AiEnhancementResult(
                List.of("ai-issue1"), List.of("ai-format1"), List.of("ai-suggestion1"), List.of("ai-question1"),
                AiEnhancementStatus.SUCCESS));

        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis toSave = invocation.getArgument(0);
            setId(toSave, 100L);
            return toSave;
        });

        AnalysisResponse response = analysisService.createAnalysis(new AnalysisRequest(10L, 20L), authentication);

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getResumeId());
        assertEquals(20L, response.getJobDescriptionId());
        assertEquals(Set.of("java"), response.getMatchedSkills());
        assertEquals(Set.of("aws"), response.getMissingSkills());
        assertEquals(List.of("issue1"), response.getAtsIssues());
        assertEquals(List.of("format1"), response.getFormattingIssues());
        assertEquals(List.of("suggestion1"), response.getContentSuggestions());
        assertEquals(List.of("question1"), response.getInterviewQuestions());
        assertEquals(List.of("ai-issue1"), response.getAiAdvancedAtsIssues());
        assertEquals(List.of("ai-format1"), response.getAiFormattingInsights());
        assertEquals(List.of("ai-suggestion1"), response.getAiContentSuggestions());
        assertEquals(List.of("ai-question1"), response.getAiInterviewQuestions());
        assertEquals(AiEnhancementStatus.SUCCESS, response.getAiEnhancementStatus());
    }

    @Test
    void createAnalysisFallsBackSafelyWhenAiEnhancementIsDisabled() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "Java developer", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(caller, "Java engineer needed", Instant.now());
        setId(jobDescription, 20L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resume));
        when(jobDescriptionRepository.findByIdFetchingOwner(20L)).thenReturn(Optional.of(jobDescription));

        MatchScoringEngine.MatchScoreResult matchResult = new MatchScoringEngine.MatchScoreResult(
                Set.of("java"), Set.of("aws"), 0.5, 0.5, 0.5, 0.5, "explanation");
        when(matchScoringEngine.score("Java developer", "Java engineer needed")).thenReturn(matchResult);

        when(atsAnalyzer.analyze(eq("Java developer"), anySet(), eq(Set.of("java"))))
                .thenReturn(new AtsAnalyzer.AtsAnalysisResult(List.of(), List.of()));
        when(contentSuggestionGenerator.generate(Set.of("aws"))).thenReturn(List.of("suggestion1"));
        when(interviewQuestionGenerator.generate(Set.of("java"), Set.of("aws"))).thenReturn(List.of("question1"));

        when(aiAnalysisEnhancer.enhance(any())).thenReturn(AiEnhancementResult.disabled());

        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis toSave = invocation.getArgument(0);
            setId(toSave, 101L);
            return toSave;
        });

        AnalysisResponse response = analysisService.createAnalysis(new AnalysisRequest(10L, 20L), authentication);

        // Deterministic Phase 3/4 fields are populated exactly as usual...
        assertEquals(List.of("suggestion1"), response.getContentSuggestions());
        assertEquals(List.of("question1"), response.getInterviewQuestions());
        // ...while the AI sections are empty rather than causing a failure.
        assertEquals(List.of(), response.getAiAdvancedAtsIssues());
        assertEquals(List.of(), response.getAiFormattingInsights());
        assertEquals(List.of(), response.getAiContentSuggestions());
        assertEquals(List.of(), response.getAiInterviewQuestions());
        assertEquals(AiEnhancementStatus.DISABLED, response.getAiEnhancementStatus());
    }

    @Test
    void createAnalysisRejectsResumeNotOwnedByCaller() {
        Resume resumeOwnedByOther = new Resume(otherUser, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resumeOwnedByOther, 10L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resumeOwnedByOther));

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.createAnalysis(new AnalysisRequest(10L, 20L), authentication));
    }

    @Test
    void createAnalysisRejectsNonExistentJobDescription() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(resumeRepository.findByIdFetchingOwner(10L)).thenReturn(Optional.of(resume));
        when(jobDescriptionRepository.findByIdFetchingOwner(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.createAnalysis(new AnalysisRequest(10L, 20L), authentication));
    }

    @Test
    void getAnalysisReturnsResultForOwner() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(caller, "content", Instant.now());
        setId(jobDescription, 20L);
        Analysis analysis = new Analysis(caller, resume, jobDescription, Set.of("java"), Set.of(),
                1.0, 1.0, 1.0, 1.0, "explanation", List.of(), List.of(), List.of(), List.of(), Instant.now());
        setId(analysis, 100L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(analysisRepository.findByIdFetchingOwner(100L)).thenReturn(Optional.of(analysis));

        AnalysisResponse response = analysisService.getAnalysis(100L, authentication);

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getResumeId());
        assertEquals(20L, response.getJobDescriptionId());
    }

    @Test
    void getAnalysisRejectsWhenNotOwnedByCaller() {
        Resume resume = new Resume(otherUser, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(otherUser, "content", Instant.now());
        setId(jobDescription, 20L);
        Analysis analysis = new Analysis(otherUser, resume, jobDescription, Set.of(), Set.of(),
                0.0, 0.0, 0.0, 0.0, "explanation", List.of(), List.of(), List.of(), List.of(), Instant.now());
        setId(analysis, 100L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(analysisRepository.findByIdFetchingOwner(100L)).thenReturn(Optional.of(analysis));

        assertThrows(ResourceNotFoundException.class, () -> analysisService.getAnalysis(100L, authentication));
    }

    @Test
    void getAnalysisRejectsNonExistentId() {
        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(analysisRepository.findByIdFetchingOwner(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> analysisService.getAnalysis(999L, authentication));
    }

    @Test
    void getInterviewQuestionsReturnsDeterministicAndAiQuestionsForOwner() {
        Resume resume = new Resume(caller, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(caller, "content", Instant.now());
        setId(jobDescription, 20L);
        Analysis analysis = new Analysis(caller, resume, jobDescription, Set.of("java"), Set.of("aws"),
                1.0, 1.0, 1.0, 1.0, "explanation", List.of(), List.of(), List.of(), List.of("question1"),
                Instant.now());
        analysis.applyAiEnhancement(List.of(), List.of(), List.of(), List.of("ai-question1"),
                AiEnhancementStatus.SUCCESS);
        setId(analysis, 100L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(analysisRepository.findByIdFetchingOwner(100L)).thenReturn(Optional.of(analysis));

        var response = analysisService.getInterviewQuestions(100L, authentication);

        assertEquals(100L, response.getAnalysisId());
        assertEquals(List.of("question1"), response.getQuestions());
        assertEquals(List.of("ai-question1"), response.getAiQuestions());
        assertEquals(AiEnhancementStatus.SUCCESS, response.getAiEnhancementStatus());
    }

    @Test
    void getInterviewQuestionsRejectsWhenNotOwnedByCaller() {
        Resume resume = new Resume(otherUser, null, "resume.pdf", ResumeFileType.PDF, "text", Instant.now());
        setId(resume, 10L);
        JobDescription jobDescription = new JobDescription(otherUser, "content", Instant.now());
        setId(jobDescription, 20L);
        Analysis analysis = new Analysis(otherUser, resume, jobDescription, Set.of(), Set.of(),
                0.0, 0.0, 0.0, 0.0, "explanation", List.of(), List.of(), List.of(), List.of(), Instant.now());
        setId(analysis, 100L);

        when(currentUserService.resolve(authentication)).thenReturn(caller);
        when(analysisRepository.findByIdFetchingOwner(100L)).thenReturn(Optional.of(analysis));

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.getInterviewQuestions(100L, authentication));
    }

    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
