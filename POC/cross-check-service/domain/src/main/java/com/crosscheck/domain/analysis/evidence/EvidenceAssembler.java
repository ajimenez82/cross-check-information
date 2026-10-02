package com.crosscheck.domain.analysis.evidence;

import com.crosscheck.domain.analysis.Evidence;
import java.time.LocalDate;
import java.util.*;
import static com.crosscheck.domain.analysis.evidence.EvidenceChecks.*;

/** Projects accepted evidence; it does not certify conclusions or create publication votes. */
public final class EvidenceAssembler {
    private EvidenceAssembler() {}

    public record Reference(String sourceId, String findingId) {
        public Reference {
            sourceId = text(sourceId, "reference.sourceId");
            findingId = text(findingId, "reference.findingId");
        }
    }

    public static List<String> sourceIds(EvidenceDossier dossier, String targetId, List<Reference> references) {
        required(dossier, "dossier");
        require(dossier.targets().stream().anyMatch(target -> target.id().equals(targetId)), "Unknown target");
        var findingMap = new HashMap<String, EvidenceDossier.Finding>();
        dossier.findings().forEach(finding -> findingMap.put(finding.id(), finding));
        var result = new LinkedHashSet<String>();
        var seen = new HashSet<String>();
        for (var reference : list(references, "references")) {
            var finding = findingMap.get(reference.findingId());
            require(finding != null, "Unknown finding reference");
            require(seen.add(finding.id()), "Duplicate finding reference");
            require(finding.sourceId().equals(reference.sourceId()), "Source identity mismatch");
            require(finding.targetId().equals(targetId), "Finding belongs to another target");
            result.add(finding.sourceId());
        }
        return List.copyOf(result);
    }

    public static Evidence source(EvidenceDossier dossier, String sourceId) {
        required(dossier, "dossier");
        var source = dossier.sources().stream().filter(value -> value.id().equals(sourceId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown source"));
        var contributions = dossier.findings().stream().filter(finding -> finding.sourceId().equals(sourceId))
                .map(EvidenceDossier.Finding::paraphrase).toList();
        String contribution = contributions.isEmpty()
                ? source.limitation() == null ? "Sin hallazgos aceptados en el expediente." : source.limitation()
                : String.join("\n", contributions);
        return new Evidence(source.id(), source.title(), source.requestedUrl(), source.publisher(),
                source.publicationDate().publishedAt(), source.capture() == null ? null : source.capture().retrievedAt(),
                contribution, null);
    }

    /** For compatibility checks only; final metadata is always assembled from the dossier. */
    public static void validateProposedDate(EvidenceDossier dossier, String sourceId, LocalDate proposedDate) {
        require(Objects.equals(source(dossier, sourceId).publishedAt(), proposedDate), "Publication date mismatch");
    }
}
