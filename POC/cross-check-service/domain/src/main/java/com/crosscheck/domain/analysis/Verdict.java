package com.crosscheck.domain.analysis;

import java.util.List;

public record Verdict(VerdictStatus status, String explanation, String documentarySupport,
                      List<String> sourceIds) {
    public Verdict {
        status = ReportChecks.required(status, "verdict.status");
        explanation = ReportChecks.text(explanation, "verdict.explanation");
        documentarySupport = ReportChecks.text(documentarySupport, "verdict.documentarySupport");
        sourceIds = ReportChecks.ids(sourceIds, "verdict.sourceIds");
    }
}
