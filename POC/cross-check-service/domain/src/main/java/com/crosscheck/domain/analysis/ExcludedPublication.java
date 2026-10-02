package com.crosscheck.domain.analysis;

public record ExcludedPublication(String sourceId, String reason, PublicationTrace trace) {
    public ExcludedPublication(String sourceId, String reason) { this(sourceId, reason, null); }
    public ExcludedPublication {
        sourceId = ReportChecks.text(sourceId, "excluded.sourceId");
        reason = ReportChecks.text(reason, "excluded.reason");
    }
}
