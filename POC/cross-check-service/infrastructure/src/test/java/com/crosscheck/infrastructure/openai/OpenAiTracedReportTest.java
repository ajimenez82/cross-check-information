package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.domain.analysis.PublicationPosition;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentScenario;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ObjectNode;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiTracedReportTest {
    @Test void acceptsCompleteVersionedReportExample() throws Exception {
        try (var stream = getClass().getResourceAsStream("/openai/traced-report-example.json")) {
            var result = tracedReport(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(4, result.publicationPositions().units().size());
            assertEquals(1, result.publicationPositions().excluded().size());
            assertEquals("AUTHOR", result.publicationPositions().units().getFirst().trace().stanceOwner().name());
            assertEquals("BACKGROUND", result.publicationPositions().excluded().getFirst().trace().temporalRelation().name());
            assertEquals("S5", result.publicationPositions().excluded().getFirst().trace().arguments().getFirst().sourceId());
            assertNull(result.publicationPositions().period());
        }
    }
    private ObjectNode fixture(int index) throws Exception {
        var base = new DevelopmentPoliticalAnalysisService(DevelopmentScenario.INSUFFICIENT_EVIDENCE, Clock.systemUTC())
                .analyze(new AiAnalysisInput("Fixture", AnalysisCategory.POLITICAL_ANALYSIS, "test", null)).report();
        var root = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(base));
        root.remove("claimAnalysis");
        root.put("schemaVersion", "2");
        var source = root.putArray("sources").addObject();
        source.put("id", "S1").put("title", "Fictional source").put("url", "https://example.org/source")
                .putNull("publisher").put("publishedAt", "2024-06-01").putNull("consultedAt")
                .put("contribution", "Fictional evidence").putNull("type");
        var positions = root.putObject("publicationPositions");
        positions.put("availability", "AVAILABLE").put("reason", "Reviewed sample")
                .put("proposition", "Supply proposition").put("selectionCriteria", "Fictional sample");
        try (var stream = getClass().getResourceAsStream("/openai/traced-assessment-examples.json")) {
            positions.putArray("assessments").add(MAPPER.readTree(stream).path("cases").get(index));
        }
        return root;
    }

    private ObjectNode assessment(ObjectNode root) {
        return (ObjectNode) root.path("publicationPositions").path("assessments").get(0);
    }

    @ParameterizedTest @ValueSource(strings = {"REQUESTED_PERIOD", "RETROSPECTIVE"})
    void acceptsRelevantContentWithUnknownPublicationDate(String relation) throws Exception {
        var root = fixture(0);
        ((ObjectNode) root.path("sources").get(0)).putNull("publishedAt");
        ((ObjectNode) assessment(root).path("trace")).put("temporalRelation", relation);
        var result = tracedReport(root.toString());
        assertEquals(1, result.publicationPositions().units().size());
        assertNull(result.sources().getFirst().publishedAt());
        assertNull(result.publicationPositions().period());
    }

    @Test void acceptsLaterRetrospectivePublicationWithoutHistoricalCutoff() throws Exception {
        var root = fixture(0);
        root.putNull("asOf");
        ((ObjectNode) root.path("sources").get(0)).put("publishedAt", "2026-06-01");
        var trace = (ObjectNode) assessment(root).path("trace");
        trace.put("temporalRelation", "RETROSPECTIVE");
        ((ObjectNode) trace.path("scope")).put("studyPeriod", "2023-2025");
        var result = tracedReport(root.toString());
        assertEquals(1, result.publicationPositions().units().size());
        assertEquals(java.time.LocalDate.of(2026, 6, 1), result.publicationPositions().period().from());
    }

    @Test void rejectsLaterRetrospectiveEvidenceWhenHistoricalCutoffIsExplicit() throws Exception {
        var root = fixture(0);
        root.put("asOf", "2025-12-31");
        ((ObjectNode) root.path("sources").get(0)).put("publishedAt", "2026-06-01");
        ((ObjectNode) assessment(root).path("trace")).put("temporalRelation", "RETROSPECTIVE");
        assertThrows(InvalidAnalysisOutputException.class, () -> tracedReport(root.toString()));
    }

    @Test void rejectsOriginalLiveTemporalContradictions() throws Exception {
        try (var stream = getClass().getResourceAsStream("/openai/traced-report-v7-invalid.json")) {
            var json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var parsed = MAPPER.readValue(json, OpenAiTracedReport.class);
            assertThrows(InvalidAnalysisOutputException.class, () -> parsed.publicationPositions().assessments().get(0).validate());
            assertThrows(InvalidAnalysisOutputException.class, () -> parsed.publicationPositions().assessments().get(3).validate());
            assertThrows(InvalidAnalysisOutputException.class, () -> tracedReport(json));
        }
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4})
    void mapsReviewedExamplesWithoutChangingTheirDecisions(int index) throws Exception {
        var root = fixture(index);
        var result = tracedReport(root.toString());
        if (index == 4) {
            assertTrue(result.publicationPositions().units().isEmpty());
            assertEquals("S1", result.publicationPositions().excluded().getFirst().sourceId());
            assertNull(result.publicationPositions().period());
        } else {
            assertEquals(PublicationPosition.valueOf(assessment(root).path("position").asText()),
                    result.publicationPositions().units().getFirst().position());
            assertNotNull(result.publicationPositions().period());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"third-party", "unknown-owner", "wrong-direction", "mixed-one-direction",
            "partial", "background", "unknown-source", "duplicate", "missing-trace", "unknown-field",
            "excluded-position", "no-position-own-argument", "wrong-version", "missing-version"})
    void rejectsInconsistentAssessments(String mutation) throws Exception {
        var root = fixture(0);
        var unit = assessment(root);
        var trace = (ObjectNode) unit.path("trace");
        switch (mutation) {
            case "third-party" -> trace.put("stanceOwner", "THIRD_PARTY");
            case "unknown-owner" -> trace.put("stanceOwner", "UNDETERMINED");
            case "wrong-direction" -> unit.put("position", "QUESTIONS");
            case "mixed-one-direction" -> unit.put("position", "MIXED");
            case "partial" -> ((ObjectNode) trace.path("scope")).put("match", "PARTIAL");
            case "background" -> trace.put("temporalRelation", "BACKGROUND");
            case "unknown-source" -> ((ObjectNode) trace.path("arguments").get(0)).put("sourceId", "missing");
            case "duplicate" -> ((tools.jackson.databind.node.ArrayNode) root.path("publicationPositions")
                    .path("assessments")).add(unit.deepCopy());
            case "missing-trace" -> unit.remove("trace");
            case "unknown-field" -> trace.put("invented", true);
            case "excluded-position" -> unit.put("decision", "EXCLUDE");
            case "no-position-own-argument" -> unit.put("position", "NO_EXPLICIT_POSITION");
            case "wrong-version" -> root.put("schemaVersion", "3");
            case "missing-version" -> root.remove("schemaVersion");
        }
        assertThrows(InvalidAnalysisOutputException.class, () -> tracedReport(root.toString()), mutation);
    }

    @Test void activeParserDoesNotSilentlyAcceptNewContract() throws Exception {
        var root = fixture(0);
        assertThrows(InvalidAnalysisOutputException.class, () -> report(root.toString()));
    }

    @Test void groupedExclusionExpandsWithoutEnteringTheSample() throws Exception {
        var root = fixture(4);
        var sources = (tools.jackson.databind.node.ArrayNode) root.path("sources");
        var copy = ((ObjectNode) sources.get(0)).deepCopy().put("id", "S2");
        sources.add(copy);
        ((tools.jackson.databind.node.ArrayNode) assessment(root).path("sourceIds")).add("S2");
        var result = tracedReport(root.toString());
        assertEquals(2, result.publicationPositions().excluded().size());
        assertEquals(result.publicationPositions().excluded().get(0).trace(),
                result.publicationPositions().excluded().get(1).trace());
        assertTrue(result.publicationPositions().units().isEmpty());
    }
}
