package com.resumeanalyzer.service.analysis.ai;

/**
 * Produces AI-generated, context-aware analysis insights (advanced ATS
 * issues, formatting/readability insights, content suggestions, and
 * interview questions) for one Resume/JobDescription pairing, on top of
 * the deterministic Phase 3/4 results.
 *
 * This is the abstraction {@code AnalysisService} depends on. It never
 * names Gemini, and it never throws: any provider-level problem (missing
 * configuration, timeout, API failure, malformed response) is caught by
 * the implementation and turned into {@link AiEnhancementResult#fallback()},
 * so a Job Seeker's analysis is always created successfully from the
 * deterministic data even when the AI layer is degraded or disabled - see
 * {@link GeminiAnalysisEnhancer}, the only implementation.
 */
public interface AiAnalysisEnhancer {

    AiEnhancementResult enhance(AiEnhancementRequest request);
}
