package com.crosscheck.application.features.analysis.common.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PublicationPositionsResult(String availability, String reason, String proposition,
                                         PeriodResult period, Instant consultedAt, String selectionCriteria,
                                         List<UnitResult> units, List<ExcludedResult> excluded) {
    public PublicationPositionsResult {
        units = List.copyOf(units);
        excluded = List.copyOf(excluded);
    }

    public record PeriodResult(LocalDate from, LocalDate to) {}
    public record UnitResult(String id, List<String> sourceIds, String position, String explanation, PublicationTraceResult trace) {
        public UnitResult { sourceIds = List.copyOf(sourceIds); }
    }
    public record ExcludedResult(String sourceId, String reason, PublicationTraceResult trace) {}
}
