package com.crosscheck.application.features.analysis.common.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Output contract independent of JSON, Spring, and domain types. */
public record AnalysisResult(String title, String context, String summary, List<String> summarySourceIds,
                             VerdictResult verdict, List<SourceResult> sources,
                             PublicationPositionsResult publicationPositions, List<String> limitations,
                             Instant analyzedAt, LocalDate asOf, ClaimAnalysisResult claimAnalysis) {
    public AnalysisResult(String title, String context, String summary, List<String> summarySourceIds,
            VerdictResult verdict, List<SourceResult> sources, PublicationPositionsResult publicationPositions,
            List<String> limitations, Instant analyzedAt, LocalDate asOf) {
        this(title, context, summary, summarySourceIds, verdict, sources, publicationPositions, limitations, analyzedAt, asOf, null);
    }
    public AnalysisResult {
        summarySourceIds = List.copyOf(summarySourceIds);
        sources = List.copyOf(sources);
        limitations = List.copyOf(limitations);
    }
}
