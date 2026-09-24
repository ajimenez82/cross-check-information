package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.domain.analysis.*;
import java.util.EnumMap;

/** Derives an editorial qualification without upgrading the documentary evidence. */
final class FinalVerdictPolicy {
    private FinalVerdictPolicy() {}

    static Verdict resolve(Verdict verdict, PublicationPositions positions) {
        if (verdict.status() == VerdictStatus.SUPPORTED_BY_PUBLICATIONS
                || verdict.status() == VerdictStatus.QUESTIONED_BY_PUBLICATIONS) {
            // Providers must supply a documentary verdict; only this policy derives final states.
            throw new InvalidAnalysisOutputException();
        }
        if (verdict.status() != VerdictStatus.INSUFFICIENT_EVIDENCE
                || positions.availability() != ClassificationAvailability.AVAILABLE
                || positions.units().isEmpty()) return verdict;

        var counts = new EnumMap<PublicationPosition, Integer>(PublicationPosition.class);
        for (var position : PublicationPosition.values()) counts.put(position, 0);
        for (var unit : positions.units()) counts.merge(unit.position(), 1, Integer::sum);
        int maximum = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var leaders = counts.entrySet().stream().filter(entry -> entry.getValue() == maximum).toList();
        if (leaders.size() != 1) return verdict;
        var status = switch (leaders.getFirst().getKey()) {
            case SUPPORTS -> VerdictStatus.SUPPORTED_BY_PUBLICATIONS;
            case QUESTIONS -> VerdictStatus.QUESTIONED_BY_PUBLICATIONS;
            default -> VerdictStatus.INSUFFICIENT_EVIDENCE;
        };
        if (status == VerdictStatus.INSUFFICIENT_EVIDENCE) return verdict;
        String qualification = status == VerdictStatus.SUPPORTED_BY_PUBLICATIONS
                ? "Predomina el respaldo en la muestra de publicaciones analizadas."
                : "Predomina el cuestionamiento en la muestra de publicaciones analizadas.";
        return new Verdict(status,
                "La evidencia disponible no permite respaldar ni refutar la afirmación. "
                        + qualification + " " + verdict.explanation(),
                verdict.documentarySupport(), verdict.sourceIds());
    }
}
