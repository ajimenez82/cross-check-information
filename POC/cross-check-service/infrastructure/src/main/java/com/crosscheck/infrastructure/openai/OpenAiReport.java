package com.crosscheck.infrastructure.openai;

import com.crosscheck.domain.analysis.*;
import java.time.LocalDate;
import java.util.List;

/** Provider fields are parsed strictly before server-owned metadata is assigned. */
record OpenAiReport(String title, String context, String summary, List<String> summarySourceIds,
                    Verdict verdict, List<Evidence> sources, PublicationPositions publicationPositions,
                    List<String> limitations, LocalDate asOf) {
    AnalysisReport toReport() {
        // This adapter has no verified per-source access records. Model timestamps are not evidence.
        var evidence = sources.stream().map(source -> new Evidence(source.id(), source.title(), source.url(),
                source.publisher(), source.publishedAt(), null, source.contribution(), source.type())).toList();
        var positions = new PublicationPositions(publicationPositions.availability(), publicationPositions.reason(),
                publicationPositions.proposition(), null, null, publicationPositions.selectionCriteria(),
                publicationPositions.units(), publicationPositions.excluded());
        // Validate references, uniqueness and the evidence cutoff before deriving the sample window.
        var validated = new AnalysisReport(title, context, summary, summarySourceIds, verdict,
                evidence, positions, limitations, asOf);
        var ids = positions.units().stream().flatMap(unit -> unit.sourceIds().stream())
                .collect(java.util.stream.Collectors.toSet());
        var sample = evidence.stream().filter(source -> ids.contains(source.id())).toList();
        PublicationPeriod period = null;
        if (!sample.isEmpty() && sample.stream().allMatch(source -> source.publishedAt() != null)) {
            var dates = sample.stream().map(Evidence::publishedAt).sorted().toList();
            period = new PublicationPeriod(dates.getFirst(), dates.getLast());
        }
        var derived = new PublicationPositions(positions.availability(), positions.reason(), positions.proposition(),
                period, null, positions.selectionCriteria(), positions.units(), positions.excluded());
        return new AnalysisReport(validated.title(), validated.context(), validated.summary(),
                validated.summarySourceIds(), validated.verdict(), validated.sources(), derived,
                validated.limitations(), validated.asOf());
    }
}
