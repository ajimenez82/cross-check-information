package com.crosscheck.application.contracts;

import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.model.AiAnalysisTurn;

/**
 * One call per attempt, with no implicit retries.
 * The adapter must map failures to AnalysisProviderException or InvalidAnalysisOutputException.
 */
@FunctionalInterface
public interface AiPoliticalAnalysisService {
    AiAnalysisTurn analyze(AiAnalysisInput input);
}
