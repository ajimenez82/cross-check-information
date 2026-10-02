package com.crosscheck.domain.analysis;

import java.time.Instant;
import java.util.List;

public record PublicationPositions(ClassificationAvailability availability, String reason,
                                   String proposition, PublicationPeriod period, Instant consultedAt,
                                   String selectionCriteria, List<PublicationAssessment> units,
                                   List<ExcludedPublication> excluded) {
    public PublicationPositions {
        availability = ReportChecks.required(availability, "positions.availability");
        reason = ReportChecks.optionalText(reason, "positions.reason");
        proposition = ReportChecks.optionalText(proposition, "positions.proposition");
        selectionCriteria = ReportChecks.optionalText(selectionCriteria, "positions.selectionCriteria");
        units = ReportChecks.list(units, "positions.units");
        excluded = ReportChecks.list(excluded, "positions.excluded");
        if (availability == ClassificationAvailability.UNAVAILABLE) {
            ReportChecks.text(reason, "positions.reason");
            ReportChecks.require(units.isEmpty(), "Una clasificación no disponible no puede tener unidades");
        } else {
            ReportChecks.text(proposition, "positions.proposition");
            ReportChecks.text(selectionCriteria, "positions.selectionCriteria");
            // Classification can be available without an exact access timestamp.
            if (units.isEmpty()) {
                ReportChecks.text(reason, "positions.reason");
            }
        }
    }
}
