package com.crosscheck.domain.analysis;

import java.util.List;

/** A deduplicated unit may group multiple reproductions of a publication. */
public record PublicationAssessment(String id, List<String> sourceIds,
                                    PublicationPosition position, String explanation, PublicationTrace trace) {
    public PublicationAssessment(String id, List<String> sourceIds, PublicationPosition position, String explanation) {
        this(id, sourceIds, position, explanation, null);
    }
    public PublicationAssessment {
        id = ReportChecks.text(id, "unit.id");
        sourceIds = ReportChecks.ids(sourceIds, "unit.sourceIds");
        ReportChecks.require(!sourceIds.isEmpty(), "La unidad necesita al menos una fuente");
        position = ReportChecks.required(position, "unit.position");
        explanation = ReportChecks.text(explanation, "unit.explanation");
        if (trace != null) {
            for (var argument : trace.arguments()) {
                ReportChecks.require(sourceIds.contains(argument.sourceId()), "Trace references another unit");
            }
        }
    }
}
