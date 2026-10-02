package com.crosscheck.domain.analysis;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Editorial result from the provider. The application adds the execution timestamp. */
public record AnalysisReport(String title, String context, String summary, List<String> summarySourceIds,
                             Verdict verdict, List<Evidence> sources,
                             PublicationPositions publicationPositions, List<String> limitations,
                             LocalDate asOf, ClaimAnalysis claimAnalysis) {
    public AnalysisReport(String title, String context, String summary, List<String> summarySourceIds,
            Verdict verdict, List<Evidence> sources, PublicationPositions publicationPositions,
            List<String> limitations, LocalDate asOf) {
        this(title, context, summary, summarySourceIds, verdict, sources, publicationPositions, limitations, asOf, null);
    }
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
        if (claimAnalysis != null) {
            ReportChecks.require(verdict.equals(claimAnalysis.aggregate()), "Global verdict differs from claim aggregation");
            if (claimAnalysis.decomposition().kind() == ClaimAnalysis.Kind.MULTIPLE) {
                ReportChecks.require(publicationPositions.availability() == ClassificationAvailability.UNAVAILABLE
                        && publicationPositions.units().isEmpty() && publicationPositions.excluded().isEmpty(),
                        "Multiple claims require an unavailable publication sample");
            } else if (publicationPositions.availability() == ClassificationAvailability.AVAILABLE) {
                ReportChecks.require(claimAnalysis.claims().getFirst().proposition().equals(publicationPositions.proposition()),
                        "Publication sample must assess the same proposition");
            }
        }

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
            if (excluded.trace() != null) {
                for (var argument : excluded.trace().arguments()) checkReferences(sourceIds, List.of(argument.sourceId()));
            }
            checkReferences(sourceIds, List.of(excluded.sourceId()));
            ReportChecks.require(excludedSources.add(excluded.sourceId()), "Fuente excluida duplicada");
            ReportChecks.require(!classifiedSources.contains(excluded.sourceId()),
                    "Una fuente no puede estar clasificada y excluida");
        }
        var period = publicationPositions.period();
        for (var source : sources) {
            var publishedAt = source.publishedAt();
            if (publishedAt == null) continue;
            if (period != null && classifiedSources.contains(source.id())) {
                ReportChecks.require(!publishedAt.isBefore(period.from()) && !publishedAt.isAfter(period.to()),
                        "Publication date is outside the classified sample period");
            }
            // Editorial exclusion does not remove a source used as documentary evidence.
            boolean usedAsEvidence = !excludedSources.contains(source.id())
                    || summarySourceIds.contains(source.id()) || verdict.sourceIds().contains(source.id());
            if (asOf != null && usedAsEvidence) {
                ReportChecks.require(!publishedAt.isAfter(asOf),
                        "Evidence publication date is after the evidence cutoff");
            }
        }
    }

    private static void checkReferences(Set<String> known, List<String> references) {
        ReportChecks.require(known.containsAll(references), "Referencia a una fuente inexistente");
    }
}
