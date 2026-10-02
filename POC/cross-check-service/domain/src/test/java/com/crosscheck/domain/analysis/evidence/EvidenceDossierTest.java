package com.crosscheck.domain.analysis.evidence;

import com.crosscheck.domain.analysis.PublicationTrace;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.domain.analysis.evidence.EvidenceDossier.*;

class EvidenceDossierTest {
    private static final String INPUT = "¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España? "
            + "Analiza el periodo 2023–2025 y contrasta publicaciones con distintas posiciones.";
    private static final String ANCHOR = INPUT.substring(0, INPUT.indexOf('?') + 1);
    private static final Instant CAPTURED_AT = Instant.parse("2026-09-30T17:00:00Z");

    private Target target() {
        return new Target("C1", "Rental price limits reduce rental supply in Spain during 2023–2025.",
                0, 0, ANCHOR.length(), ANCHOR);
    }

    private PublicationDate unknownDate() {
        return new PublicationDate(null, DatePrecision.UNKNOWN, DateKind.UNKNOWN, DateReview.DECLARED,
                "No verified publication date", null);
    }

    private Source captured(String id, String content) {
        return new Source(id, URI.create("https://example.org/" + id), "Document " + id, "Publisher",
                List.of("Author"), Access.CAPTURED,
                new Capture(URI.create("https://example.org/" + id), CAPTURED_AT, "fixture-v1", content),
                unknownDate(), null);
    }

    private Finding finding(String id, String sourceId, String content) {
        return new Finding(id, sourceId, "C1", new Fragment(0, content.length(), content, "Fixture section 1"),
                "A limited descriptive finding", PublicationTrace.Attribution.AUTHOR, Method.DESCRIPTIVE,
                new PublicationTrace.Scope("Listings", "Spain", "2023–2025", "Rental limits",
                        PublicationTrace.Match.MATCH, "Fixture scope"),
                PublicationTrace.TemporalRelation.REQUESTED_PERIOD, PublicationTrace.Relation.CONTEXT);
    }

    private EvidenceDossier dossier(List<Source> sources, List<Finding> findings) {
        return new EvidenceDossier(VERSION, List.of(INPUT), List.of(target()), sources, findings);
    }

    private Source dated(String id, String content, LocalDate date, DateKind kind, DateReview review) {
        var base = captured(id, content);
        return new Source(base.id(), base.requestedUrl(), base.title(), base.publisher(), base.authors(),
                base.access(), base.capture(), new PublicationDate(date, DatePrecision.DAY, kind, review,
                "Reviewed fixture metadata", new Fragment(0, content.length(), content, "Publication metadata")), null);
    }

    @Test void rejectsObservedV10NonLiteralAnchor() {
        var rewritten = "¿Los límites al precio del alquiler reducen la oferta de vivienda en alquiler en España durante 2023–2025?";
        var invalid = new Target("C1", target().proposition(), 0, 0, rewritten.length(), rewritten);
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new EvidenceDossier(VERSION, List.of(INPUT), List.of(invalid), List.of(), List.of()));
        assertEquals("Non-literal input anchor", exception.getMessage());
    }

    @Test void acceptsLiteralAnchorAndPreservesResolvedProposition() {
        var result = dossier(List.of(), List.of());
        assertEquals(ANCHOR, result.targets().getFirst().excerpt());
        assertEquals(target().proposition(), result.targets().getFirst().proposition());
    }

    @Test void acceptsExplicitPreviousContextAnchorWithoutRewritingIt() {
        var previous = "Supply in Spain during 2023–2025";
        var target = new Target("C1", "Resolved proposition", 1, 0, previous.length(), previous);
        var result = new EvidenceDossier(VERSION, List.of("¿Y en Cataluña?", previous), List.of(target), List.of(), List.of());
        assertEquals(previous, result.targets().getFirst().excerpt());
    }

    @Test void rejectsAmbiguousAndOverlappingAnchors() {
        var repeated = new Target("C1", "Proposition", 0, 0, 4, "Rent");
        assertThrows(IllegalArgumentException.class,
                () -> new EvidenceDossier(VERSION, List.of("Rent Rent"), List.of(repeated), List.of(), List.of()));
        var first = new Target("C1", "First", 0, 0, 6, "Rental");
        var second = new Target("C2", "Second", 0, 3, 8, "tal s");
        assertThrows(IllegalArgumentException.class,
                () -> new EvidenceDossier(VERSION, List.of("Rental supply"), List.of(first, second), List.of(), List.of()));
    }

    @Test void preservesFedeaIdentityInsteadOfAcceptingBoeReference() {
        var boe = captured("S1", "A legal provision");
        var fedea = captured("S2", "Listings declined in the studied territory");
        var result = dossier(List.of(boe, fedea), List.of(finding("F1", "S2", fedea.capture().content())));
        assertEquals(List.of("S2"), EvidenceAssembler.sourceIds(result, "C1",
                List.of(new EvidenceAssembler.Reference("S2", "F1"))));
        assertThrows(IllegalArgumentException.class, () -> EvidenceAssembler.sourceIds(result, "C1",
                List.of(new EvidenceAssembler.Reference("S1", "F1"))));
        assertThrows(IllegalArgumentException.class,
                () -> dossier(List.of(boe, fedea), List.of(finding("F1", "S1", fedea.capture().content()))));
    }

    @Test void rejectsUnknownAndDuplicateReferencesButDeduplicatesSourceIds() {
        var source = captured("S1", "Evidence");
        var result = dossier(List.of(source), List.of(finding("F1", "S1", "Evidence"), finding("F2", "S1", "Evidence")));
        var reference = new EvidenceAssembler.Reference("S1", "F1");
        assertEquals(List.of("S1"), EvidenceAssembler.sourceIds(result, "C1",
                List.of(reference, new EvidenceAssembler.Reference("S1", "F2"))));
        assertThrows(IllegalArgumentException.class, () -> EvidenceAssembler.sourceIds(result, "C1", List.of(reference, reference)));
        assertThrows(IllegalArgumentException.class, () -> EvidenceAssembler.sourceIds(result, "C1",
                List.of(new EvidenceAssembler.Reference("S1", "missing"))));
        assertThrows(IllegalArgumentException.class, () -> EvidenceAssembler.sourceIds(result, "C2", List.of(reference)));
    }

    @Test void rejectsV10BoeAndBankDatesAgainstReviewedMetadata() {
        // Minimal, manually reviewed metadata fixtures, not a live retrieval or an automatic date parser.
        var boe = dated("S1", "Publicado en BOE: 15 de marzo de 2024", LocalDate.of(2024, 3, 15),
                DateKind.PUBLICATION, DateReview.CONFIRMED);
        var bank = dated("S4", "16/10/2024", LocalDate.of(2024, 10, 16), DateKind.PUBLICATION, DateReview.CONFIRMED);
        var result = dossier(List.of(boe, bank), List.of());
        assertEquals(LocalDate.of(2024, 3, 15), EvidenceAssembler.source(result, "S1").publishedAt());
        assertEquals(LocalDate.of(2024, 10, 16), EvidenceAssembler.source(result, "S4").publishedAt());
        assertThrows(IllegalArgumentException.class,
                () -> EvidenceAssembler.validateProposedDate(result, "S1", LocalDate.of(2024, 3, 14)));
        assertThrows(IllegalArgumentException.class,
                () -> EvidenceAssembler.validateProposedDate(result, "S4", LocalDate.of(2024, 1, 1)));
        EvidenceAssembler.validateProposedDate(result, "S1", LocalDate.of(2024, 3, 15));
    }

    @Test void doesNotPublishEnactmentUpdateOrUnreviewedDates() {
        for (var kind : List.of(DateKind.ENACTMENT, DateKind.UPDATE, DateKind.UNKNOWN)) {
            var result = dossier(List.of(dated("S1", "2024-03-14", LocalDate.of(2024, 3, 14), kind, DateReview.CONFIRMED)), List.of());
            assertNull(EvidenceAssembler.source(result, "S1").publishedAt());
        }
        var result = dossier(List.of(dated("S1", "2024-03-14", LocalDate.of(2024, 3, 14),
                DateKind.PUBLICATION, DateReview.DECLARED)), List.of());
        assertNull(EvidenceAssembler.source(result, "S1").publishedAt());
    }

    @Test void preservesPartialDatesWithoutInventingDay() {
        var date = new PublicationDate(null, DatePrecision.MONTH, DateKind.PUBLICATION, DateReview.CONFIRMED,
                "March 2024", new Fragment(0, 10, "March 2024", "Cover"));
        var base = captured("S1", "March 2024");
        var source = new Source("S1", base.requestedUrl(), base.title(), null, List.of(), Access.CAPTURED, base.capture(), date, null);
        assertNull(EvidenceAssembler.source(dossier(List.of(source), List.of()), "S1").publishedAt());
        assertEquals("March 2024", source.publicationDate().label());
        assertThrows(IllegalArgumentException.class, () -> new PublicationDate(LocalDate.of(2024, 3, 1),
                DatePrecision.MONTH, DateKind.PUBLICATION, DateReview.CONFIRMED, "March 2024", date.evidence()));
    }

    @Test void confirmedDateRequiresEvidenceAndExactCapturedFragment() {
        assertThrows(IllegalArgumentException.class, () -> new PublicationDate(LocalDate.of(2024, 3, 15),
                DatePrecision.DAY, DateKind.PUBLICATION, DateReview.CONFIRMED, "15 March 2024", null));
        var base = captured("S1", "Different metadata");
        var date = new PublicationDate(LocalDate.of(2024, 3, 15), DatePrecision.DAY, DateKind.PUBLICATION,
                DateReview.CONFIRMED, "15 March 2024", new Fragment(0, 13, "15 March 2024", "Header"));
        assertThrows(IllegalArgumentException.class, () -> new Source("S1", base.requestedUrl(), base.title(),
                null, List.of(), Access.CAPTURED, base.capture(), date, null));
    }

    @Test void unverifiedOrInaccessibleContentCannotSupplyFindings() {
        for (var access : List.of(Access.INACCESSIBLE, Access.MODEL_EXCERPT_ONLY)) {
            var source = new Source("S1", URI.create("https://example.org/S1"), "Unverified source", null,
                    List.of(), access, null, unknownDate(), "Content not independently acquired");
            var result = dossier(List.of(source), List.of());
            assertNull(EvidenceAssembler.source(result, "S1").consultedAt());
            assertEquals(source.limitation(), EvidenceAssembler.source(result, "S1").contribution());
            assertThrows(IllegalArgumentException.class, () -> dossier(List.of(source), List.of(finding("F1", "S1", "Invented"))));
        }
    }

    @Test void alteredCaptureCannotReuseOldFragmentAndHashChanges() {
        var original = captured("S1", "Original evidence");
        var altered = captured("S1", "Replaced evidence");
        assertNotEquals(original.capture().contentHash(), altered.capture().contentHash());
        assertThrows(IllegalArgumentException.class,
                () -> dossier(List.of(altered), List.of(finding("F1", "S1", original.capture().content()))));
        assertEquals(CAPTURED_AT, EvidenceAssembler.source(dossier(List.of(original), List.of()), "S1").consultedAt());
    }

    @Test void rejectsDuplicateIdentitiesAndMissingOwners() {
        var source = captured("S1", "Evidence");
        var finding = finding("F1", "S1", "Evidence");
        assertThrows(IllegalArgumentException.class, () -> dossier(List.of(source, source), List.of()));
        assertThrows(IllegalArgumentException.class, () -> dossier(List.of(source), List.of(finding, finding)));
        assertThrows(IllegalArgumentException.class, () -> dossier(List.of(), List.of(finding)));
        assertThrows(IllegalArgumentException.class,
                () -> new EvidenceDossier(2, List.of(INPUT), List.of(target()), List.of(), List.of()));
    }

    @Test void freezesCallerOwnedCollections() {
        var sources = new ArrayList<>(List.of(captured("S1", "Evidence")));
        var findings = new ArrayList<>(List.of(finding("F1", "S1", "Evidence")));
        var result = dossier(sources, findings);
        sources.clear(); findings.clear();
        assertEquals(1, result.sources().size());
        assertEquals(1, result.findings().size());
        assertThrows(UnsupportedOperationException.class, () -> result.sources().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.targets().clear());
    }

    @Test void cannotReuseEvidenceForAnotherExistingTarget() {
        var input = "Rental prices and rental supply";
        var first = new Target("C1", "Prices", 0, 0, 13, "Rental prices");
        var second = new Target("C2", "Supply", 0, 18, 31, "rental supply");
        var source = captured("S1", "Evidence about prices");
        var result = new EvidenceDossier(VERSION, List.of(input), List.of(first, second), List.of(source),
                List.of(finding("F1", "S1", source.capture().content())));
        var reference = new EvidenceAssembler.Reference("S1", "F1");
        assertEquals(List.of("S1"), EvidenceAssembler.sourceIds(result, "C1", List.of(reference)));
        assertThrows(IllegalArgumentException.class,
                () -> EvidenceAssembler.sourceIds(result, "C2", List.of(reference)));
    }
}
