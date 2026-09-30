package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.MatchRequest;
import com.resumeanalyzer.dto.MatchResponse;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.matching.MatchScoringEngine;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for POST /api/matches.
 *
 * Resolves the referenced Resume and JobDescription, enforces that both
 * belong to the authenticated caller (reusing the same ownership model
 * introduced in Phase 2 - no separate Candidate entity, no cross-user
 * access), and delegates the actual scoring to the framework-agnostic
 * {@link MatchScoringEngine}.
 */
@Service
public class MatchService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final CurrentUserService currentUserService;
    private final MatchScoringEngine matchScoringEngine;

    public MatchService(ResumeRepository resumeRepository,
                         JobDescriptionRepository jobDescriptionRepository,
                         CurrentUserService currentUserService,
                         MatchScoringEngine matchScoringEngine) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.currentUserService = currentUserService;
        this.matchScoringEngine = matchScoringEngine;
    }

    @Transactional(readOnly = true)
    public MatchResponse match(MatchRequest request, Authentication authentication) {
        User caller = currentUserService.resolve(authentication);

        Resume resume = resumeRepository.findByIdFetchingOwner(request.getResumeId())
                .filter(r -> r.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No resume found for the given id"));

        JobDescription jobDescription = jobDescriptionRepository.findByIdFetchingOwner(request.getJobDescriptionId())
                .filter(jd -> jd.getOwner().getId().equals(caller.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No job description found for the given id"));

        MatchScoringEngine.MatchScoreResult result =
                matchScoringEngine.score(resume.getExtractedText(), jobDescription.getContent());

        return new MatchResponse(
                resume.getId(),
                jobDescription.getId(),
                result.matchedSkills(),
                result.missingSkills(),
                result.skillScore(),
                result.experienceScore(),
                result.semanticScore(),
                result.finalScore(),
                result.explanation());
    }
}
