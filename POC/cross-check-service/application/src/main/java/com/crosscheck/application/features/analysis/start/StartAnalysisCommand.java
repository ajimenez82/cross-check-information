package com.crosscheck.application.features.analysis.start;

/** An absent or null token starts a conversation; an empty token is invalid. */
public record StartAnalysisCommand(String text, String conversationToken) {
    @Override
    public String toString() {
        return "StartAnalysisCommand[redacted]";
    }
}
