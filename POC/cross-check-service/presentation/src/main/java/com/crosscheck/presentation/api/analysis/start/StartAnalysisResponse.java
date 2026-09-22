package com.crosscheck.presentation.api.analysis.start;

import com.crosscheck.application.features.analysis.common.dto.AnalysisResult;

public record StartAnalysisResponse(String conversationToken, AnalysisResult analysis) {
    @Override
    public String toString() {
        return "StartAnalysisResponse[redacted]";
    }
}
