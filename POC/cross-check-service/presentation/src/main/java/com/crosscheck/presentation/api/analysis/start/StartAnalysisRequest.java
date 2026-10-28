package com.crosscheck.presentation.api.analysis.start;

import jakarta.validation.constraints.NotBlank;

public record StartAnalysisRequest(@NotBlank String text, String conversationToken,
        com.crosscheck.domain.analysis.AnalysisCategory category) {
    @Override
    public String toString() {
        return "StartAnalysisRequest[redacted]";
    }
}
