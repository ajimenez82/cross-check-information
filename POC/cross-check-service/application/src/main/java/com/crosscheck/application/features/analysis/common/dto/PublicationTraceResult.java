package com.crosscheck.application.features.analysis.common.dto;

import java.util.List;

public record PublicationTraceResult(String stanceOwner, String stanceHolder, ScopeResult scope,
                                     String temporalRelation, List<ArgumentResult> arguments) {
    public PublicationTraceResult { arguments = List.copyOf(arguments); }
    public record ScopeResult(String outcome, String geography, String studyPeriod, String policy,
                              String match, String explanation) {}
    public record ArgumentResult(String sourceId, String locator, String paraphrase,
                                 String attribution, String relation) {}
}
