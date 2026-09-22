package com.crosscheck.domain.analysis;

public record ExcludedPublication(String sourceId, String reason) {
    public ExcludedPublication {
        sourceId = ReportChecks.text(sourceId, "excluded.sourceId");
        reason = ReportChecks.text(reason, "excluded.reason");
    }
}
