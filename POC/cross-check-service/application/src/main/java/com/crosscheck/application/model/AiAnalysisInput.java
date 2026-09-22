package com.crosscheck.application.model;

import com.crosscheck.domain.analysis.AnalysisCategory;

/** sessionId is null for a new conversation; it never comes directly from the browser request. */
public record AiAnalysisInput(String text, AnalysisCategory category, String agentRevision, String sessionId) {
    @Override
    public String toString() {
        return "AiAnalysisInput[redacted]";
    }
}
