package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.features.analysis.common.dto.AnalysisResult;

public record StartAnalysisResult(String conversationToken, AnalysisResult analysis) {
    @Override
    public String toString() {
        return "StartAnalysisResult[redacted]";
    }
}
