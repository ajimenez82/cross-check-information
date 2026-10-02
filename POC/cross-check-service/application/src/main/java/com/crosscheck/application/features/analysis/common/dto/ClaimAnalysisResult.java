package com.crosscheck.application.features.analysis.common.dto;
import java.util.List;

public record ClaimAnalysisResult(String analysisTarget, DecompositionResult decomposition, List<ClaimResult> claims) {
    public ClaimAnalysisResult { claims = List.copyOf(claims); }
    public record DecompositionResult(String kind, String basis, String explanation) {}
    public record ClaimResult(String id, String inputExcerpt, String proposition, VerdictResult verdict) {}
}
