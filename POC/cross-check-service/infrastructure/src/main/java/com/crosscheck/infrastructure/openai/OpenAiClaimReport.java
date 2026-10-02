package com.crosscheck.infrastructure.openai;

import com.crosscheck.domain.analysis.*;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import java.time.LocalDate;
import java.util.List;

/** Opt-in provider v3: no provider-supplied global verdict. */
record OpenAiClaimReport(String schemaVersion, String title, String context, String summary,
        List<String> summarySourceIds, ClaimAnalysis claimAnalysis, List<Evidence> sources,
        OpenAiTracedReport.Positions publicationPositions, List<String> limitations, LocalDate asOf) {
    AnalysisReport toReport(String input) {
        return toReport(java.util.List.of(input));
    }
    AnalysisReport toReport(java.util.List<String> inputs) {
        if (!"3".equals(schemaVersion) || claimAnalysis == null) throw new InvalidAnalysisOutputException();
        claimAnalysis.validateInputs(inputs);
        var report = new OpenAiTracedReport("2", title, context, summary, summarySourceIds,
                claimAnalysis.aggregate(), sources, publicationPositions, limitations, asOf).toReport();
        return new AnalysisReport(report.title(), report.context(), report.summary(), report.summarySourceIds(),
                report.verdict(), report.sources(), report.publicationPositions(), report.limitations(), report.asOf(), claimAnalysis);
    }
}
