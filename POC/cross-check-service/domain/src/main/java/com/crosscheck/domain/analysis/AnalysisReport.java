package com.crosscheck.domain.analysis;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Editorial result from the provider. The application adds the execution timestamp. */
public record AnalysisReport(String title, String context, String summary, List<String> summarySourceIds,
                             Verdict verdict, List<Evidence> sources,
                             PublicationPositions publicationPositions, List<String> limitations,
                             LocalDate asOf) {
    public AnalysisReport {
        title = ReportChecks.text(title, "title");
        context = ReportChecks.text(context, "context");
        summary = ReportChecks.text(summary, "summary");
        summarySourceIds = ReportChecks.ids(summarySourceIds, "summarySourceIds");
        verdict = ReportChecks.required(verdict, "verdict");
        sources = ReportChecks.list(sources, "sources");
        publicationPositions = ReportChecks.required(publicationPositions, "publicationPositions");
        limitations = ReportChecks.texts(limitations, "limitations");

        Set<String> sourceIds = new HashSet<>();
        for (var source : sources) {
            ReportChecks.require(sourceIds.add(source.id()), "Identificador de fuente duplicado");
        }
        checkReferences(sourceIds, summarySourceIds);
        checkReferences(sourceIds, verdict.sourceIds());

        Set<String> unitIds = new HashSet<>();
        Set<String> classifiedSources = new HashSet<>();
        for (var unit : publicationPositions.units()) {
            ReportChecks.require(unitIds.add(unit.id()), "Identificador de unidad duplicado");
            checkReferences(sourceIds, unit.sourceIds());
            for (var sourceId : unit.sourceIds()) {
                ReportChecks.require(classifiedSources.add(sourceId), "Fuente clasificada en más de una unidad");
            }
        }
        Set<String> excludedSources = new HashSet<>();
        for (var excluded : publicationPositions.excluded()) {
            checkReferences(sourceIds, List.of(excluded.sourceId()));
            ReportChecks.require(excludedSources.add(excluded.sourceId()), "Fuente excluida duplicada");
            ReportChecks.require(!classifiedSources.contains(excluded.sourceId()),
                    "Una fuente no puede estar clasificada y excluida");
        }
    }

    private static void checkReferences(Set<String> known, List<String> references) {
        ReportChecks.require(known.containsAll(references), "Referencia a una fuente inexistente");
    }
}
