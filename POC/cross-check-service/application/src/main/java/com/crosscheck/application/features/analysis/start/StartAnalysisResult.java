package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.features.analysis.common.dto.AnalysisResult;

public record StartAnalysisResult(String conversationToken, AnalysisResult analysis,
        com.crosscheck.application.model.Clarification clarification) {
    public StartAnalysisResult(String conversationToken, AnalysisResult analysis) { this(conversationToken, analysis, null); }
    @Override
    public String toString() {
        return "StartAnalysisResult[redacted]";
    }
}
