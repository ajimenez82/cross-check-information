package com.crosscheck.application.features.analysis.start;

/** An absent or null token starts a conversation; an empty token is invalid. */
public record StartAnalysisCommand(String text, String conversationToken,
        com.crosscheck.domain.analysis.AnalysisCategory category) {
    public StartAnalysisCommand(String text, String conversationToken) {
        this(text, conversationToken, null);
    }
    @Override
    public String toString() {
        return "StartAnalysisCommand[redacted]";
    }
}
