package com.crosscheck.domain.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnalysisReportTest {
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void acceptsInsufficientEvidenceWithoutInventingSourcesOrDates() {
        var report = report(List.of(), List.of(), List.of(), unavailable());
        assertEquals(VerdictStatus.INSUFFICIENT_EVIDENCE, report.verdict().status());
        assertTrue(report.sources().isEmpty());
        assertNull(report.asOf());
    }

    @ParameterizedTest
    @EnumSource(VerdictStatus.class)
    void acceptsEveryAgreedVerdictWithoutInferringEditorialRules(VerdictStatus status) {
        assertEquals(status, new Verdict(status, "Explicación", "Limitado", List.of()).status());
    }

    @ParameterizedTest
    @EnumSource(PublicationPosition.class)
    void acceptsEveryPublicationPosition(PublicationPosition position) {
        assertEquals(position, new PublicationAssessment("u1", List.of("s1"), position, "Motivo").position());
    }

    @Test
    void rejectsDuplicateSourceIds() {
        assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(source("s1"), source("s1")), List.of(), List.of(), unavailable()));
    }

    @Test
    void rejectsUnknownVerdictAndSummaryReferences() {
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(), List.of("missing"), List.of(), unavailable())),
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(), List.of(), List.of("missing"), unavailable()))
        );
    }

    @Test
    void rejectsDuplicateReferences() {
        assertThrows(InvalidAnalysisReportException.class,
                () -> new Verdict(VerdictStatus.OPINION, "Explicación", "Limitado", List.of("s1", "s1")));
        assertThrows(InvalidAnalysisReportException.class,
                () -> new PublicationAssessment("u", List.of("s1", "s1"), PublicationPosition.MIXED, "Motivo"));
    }

    @Test
    void acceptsGroupedReproductionsAsOneUnit() {
        var positions = available(List.of(unit("u1", "s1", "s2")), List.of());
        var report = report(List.of(source("s1"), source("s2")), List.of("s1"), List.of("s2"), positions);
        assertEquals(1, report.publicationPositions().units().size());
        assertEquals(2, report.publicationPositions().units().getFirst().sourceIds().size());
    }

    @Test
    void rejectsDuplicateUnitsAndSourcesCountedTwice() {
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(source("s1"), source("s2")), List.of(), List.of(),
                    available(List.of(unit("u", "s1"), unit("u", "s2")), List.of()))),
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(source("s1")), List.of(), List.of(),
                    available(List.of(unit("u1", "s1"), unit("u2", "s1")), List.of())))
        );
    }

    @Test
    void rejectsUnknownClassifiedOrExcludedSources() {
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(), List.of(), List.of(), available(List.of(unit("u", "missing")), List.of()))),
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(), List.of(), List.of(), available(List.of(),
                    List.of(new ExcludedPublication("missing", "Inaccesible")))))
        );
    }

    @Test
    void rejectsDuplicateExclusionsAndClassifiedExclusions() {
        var excluded = new ExcludedPublication("s1", "Inaccesible");
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(source("s1")), List.of(), List.of(),
                    available(List.of(), List.of(excluded, excluded)))),
            () -> assertThrows(InvalidAnalysisReportException.class,
                () -> report(List.of(source("s1")), List.of(), List.of(),
                    available(List.of(unit("u1", "s1")), List.of(excluded))))
        );
    }

    @Test
    void zeroClassifiedUnitsIsDistinctFromUnavailable() {
        var positions = available(List.of(), List.of(new ExcludedPublication("s1", "No accesible")));
        var report = report(List.of(source("s1")), List.of(), List.of(), positions);
        assertEquals(ClassificationAvailability.AVAILABLE, report.publicationPositions().availability());
        assertTrue(report.publicationPositions().units().isEmpty());
        assertNotNull(report.publicationPositions().reason());
    }

    @Test
    void unavailableCannotContainClassifiedUnits() {
        assertThrows(InvalidAnalysisReportException.class, () -> new PublicationPositions(
                ClassificationAvailability.UNAVAILABLE, "No realizada", null, null, null, null,
                List.of(unit("u1", "s1")), List.of()));
    }

    @Test
    void availableRequiresPropositionCriteriaAndConsultationDate() {
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class, () -> new PublicationPositions(
                ClassificationAvailability.AVAILABLE, null, null, null, NOW, "Búsqueda",
                List.of(unit("u1", "s1")), List.of())),
            () -> assertThrows(InvalidAnalysisReportException.class, () -> new PublicationPositions(
                ClassificationAvailability.AVAILABLE, null, "Tesis", null, NOW, null,
                List.of(unit("u1", "s1")), List.of())),
            () -> assertThrows(InvalidAnalysisReportException.class, () -> new PublicationPositions(
                ClassificationAvailability.AVAILABLE, null, "Tesis", null, null, "Búsqueda",
                List.of(unit("u1", "s1")), List.of()))
        );
    }

    @Test
    void emptyOrUnavailableClassificationNeedsReason() {
        for (var availability : ClassificationAvailability.values()) {
            assertThrows(InvalidAnalysisReportException.class, () -> new PublicationPositions(
                    availability, null, "Tesis", null, NOW, "Búsqueda", List.of(), List.of()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///secret", "/relative", "https://user:pass@example.org"})
    void rejectsUnsafeOrRelativeSourceUrls(String url) {
        assertThrows(InvalidAnalysisReportException.class, () ->
                new Evidence("s", "Título", URI.create(url), null, null, NOW, "Aporta contexto", null));
    }

    @Test
    void rejectsInvertedPeriodsAndMissingRequiredFields() {
        assertAll(
            () -> assertThrows(InvalidAnalysisReportException.class, () ->
                new PublicationPeriod(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 16))),
            () -> assertThrows(InvalidAnalysisReportException.class, () ->
                new Verdict(null, "Explicación", "Insuficiente", List.of())),
            () -> assertThrows(InvalidAnalysisReportException.class, () ->
                new AnalysisReport(" ", "Contexto", "Resumen", List.of(), verdict(List.of()),
                    List.of(), unavailable(), List.of(), null)),
            () -> assertThrows(InvalidAnalysisReportException.class, () ->
                new PublicationAssessment("u", List.of(), PublicationPosition.MIXED, "Motivo"))
        );
    }

    @Test
    void collectionsAreDefensivelyCopiedAndUnmodifiable() {
        var sources = new ArrayList<>(List.of(source("s1")));
        var refs = new ArrayList<>(List.of("s1"));
        var report = report(sources, refs, refs, unavailable());
        sources.clear();
        refs.clear();
        assertEquals(1, report.sources().size());
        assertEquals(List.of("s1"), report.summarySourceIds());
        assertEquals(List.of("s1"), report.verdict().sourceIds());
        assertThrows(UnsupportedOperationException.class, () -> report.sources().clear());
    }

    private static Evidence source(String id) {
        return new Evidence(id, "Publicación ilustrativa", URI.create("https://example.org/" + id),
                null, null, NOW, "Aportación ilustrativa", null);
    }

    private static Verdict verdict(List<String> ids) {
        return new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE, "No hay base suficiente", "Insuficiente", ids);
    }

    private static PublicationAssessment unit(String id, String... ids) {
        return new PublicationAssessment(id, List.of(ids), PublicationPosition.QUESTIONS, "Justificación");
    }

    private static PublicationPositions unavailable() {
        return new PublicationPositions(ClassificationAvailability.UNAVAILABLE, "No realizada",
                null, null, null, null, List.of(), List.of());
    }

    private static PublicationPositions available(List<PublicationAssessment> units,
                                                  List<ExcludedPublication> excluded) {
        return new PublicationPositions(ClassificationAvailability.AVAILABLE,
                units.isEmpty() ? "Ninguna publicación accesible" : null, "Tesis",
                null, NOW, "Búsqueda documentada", units, excluded);
    }

    private static AnalysisReport report(List<Evidence> sources, List<String> verdictIds,
                                         List<String> summaryIds, PublicationPositions positions) {
        return new AnalysisReport("Título", "Contexto", "Resumen", summaryIds, verdict(verdictIds),
                sources, positions, List.of(), null);
    }
}
