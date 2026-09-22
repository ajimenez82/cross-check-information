package com.crosscheck.presentation.api.analysis.start;

import jakarta.validation.constraints.NotBlank;

public record StartAnalysisRequest(@NotBlank String text, String conversationToken) {
    @Override
    public String toString() {
        return "StartAnalysisRequest[redacted]";
    }
}
