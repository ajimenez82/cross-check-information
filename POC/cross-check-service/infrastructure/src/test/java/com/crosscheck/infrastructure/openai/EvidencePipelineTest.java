package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.domain.analysis.*;
import com.crosscheck.domain.analysis.evidence.EvidenceDossier.*;
import com.crosscheck.infrastructure.evidence.SourceCapture;
import java.net.URI;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;

class EvidencePipelineTest {
    static String fixture(String name) {
        try (var input = EvidencePipelineTest.class.getResourceAsStream("/openai/evidence/" + name + ".json")) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
    static final AiAnalysisInput INPUT = new AiAnalysisInput("¿Los límites reducen la oferta? Analiza 2023–2025.", AnalysisCategory.POLITICAL_ANALYSIS, "political-v11", null);
    static SourceCapture.Document document(URI uri) {
        String dateText = "Published: 2024-10-16";
        String content = dateText + " The report observes fewer rental listings after the policy change. Unselected appendix that must not be sent to synthesis.";
        var date = new PublicationDate(LocalDate.of(2024,10,16), DatePrecision.DAY, DateKind.PUBLICATION, DateReview.CONFIRMED,
                dateText, new Fragment(0, dateText.length(), dateText, "Header"));
        return new SourceCapture.Document(new Capture(uri, Instant.parse("2026-09-30T00:00:00Z"), "fixture-v1", content), date, "Captured document");
    }
    EvidencePipeline.State start(String raw) {
        return EvidencePipeline.start(MAPPER.readValue(raw, EvidencePipeline.ResearchResponse.class).research(), INPUT);
    }
    EvidencePipeline.State captured() {
        var state = start(fixture("research")); EvidencePipeline.captureNext(state, EvidencePipelineTest::document); return state;
    }
    EvidencePipeline.Synthesis synthesis(String raw) { return MAPPER.readValue(raw, EvidencePipeline.Synthesis.class); }

    @Test void roundTripRetainsEvidenceAndAssemblesServerOwnedMetadataAndAnchor() {
        var state = captured();
        var restored = MAPPER.readValue(MAPPER.writeValueAsString(state), EvidencePipeline.State.class);
        assertEquals(state.dossier(), restored.dossier());
        var report = EvidencePipeline.assemble(restored, synthesis(fixture("synthesis")));
        assertEquals(INPUT.text(), report.claimAnalysis().claims().getFirst().inputExcerpt());
        assertEquals("Captured document", report.sources().getFirst().title());
        assertEquals(LocalDate.of(2024,10,16), report.sources().getFirst().publishedAt());
        assertNotNull(report.sources().getFirst().consultedAt());
        assertEquals("S1", report.publicationPositions().units().getFirst().trace().arguments().getFirst().sourceId());
    }
    @Test void synthesisReceivesOnlyAcceptedEvidenceNotEntireCapturedDocuments() {
        String packet = MAPPER.writeValueAsString(EvidencePipeline.synthesisInput(captured()));
        assertTrue(packet.contains("F1_1")); assertTrue(packet.contains("contentHash"));
        assertFalse(packet.contains("Unselected appendix"));
    }
    @Test void inventedQuoteIsDiscardedWithExplicitIssue() {
        var state = start(fixture("research").replace("The report observes fewer rental listings after the policy change.", "This quotation does not exist in the captured document."));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertTrue(state.findings.isEmpty()); assertFalse(state.issues.isEmpty());
        assertNotNull(state.sources.getFirst().limitation());
    }
    @Test void inaccessibleSourceCannotContributeFindings() {
        var state = start(fixture("research"));
        EvidencePipeline.captureNext(state, uri -> { throw new IllegalArgumentException("Blocked"); });
        assertTrue(state.findings.isEmpty()); assertEquals(Access.INACCESSIBLE, state.sources.getFirst().access());
    }
    @Test void cannotInventFindingOrCrossSourceReference() {
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(captured(), synthesis(fixture("synthesis").replace("F1_1", "F9_1"))));
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(captured(), synthesis(fixture("synthesis").replace("\"S1\"", "\"S2\""))));
    }
    @Test void legalEvidenceCannotBecomeEditorialVote() {
        var state = start(fixture("research").replace("\"ANALYSIS\"", "\"LEGAL\""));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(state, synthesis(fixture("synthesis"))));
    }
    @Test void cautionRecordedAsContextCannotBeCountedAsOpposition() {
        var state = start(fixture("research").replace("\"relation\": \"SUPPORTS\"", "\"relation\": \"CONTEXT\""));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(state, synthesis(fixture("synthesis").replace("\"SUPPORTS\"", "\"QUESTIONS\""))));
    }
    @Test void partialScopeCannotBePromotedToCountBySynthesis() {
        var state = start(fixture("research").replace("\"MATCH\"", "\"PARTIAL\""));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(state, synthesis(fixture("synthesis"))));
    }
    @Test void cutoffExcludesLaterOrUndatedDocumentsBeforeSynthesis() {
        var state = start(fixture("research").replace("\"asOf\": null", "\"asOf\": \"2024-01-01\""));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertTrue(state.findings.isEmpty()); assertFalse(state.issues.isEmpty());
    }
    @Test void invalidAnchorOptionIsNotSilentlyRepaired() {
        assertThrows(RuntimeException.class, () -> start(fixture("research").replace("\"A0\"", "\"invented\"")));
    }

    @Test void repeatedExactUrlIsCapturedOnceAndDivergentAuthorNamesRemainUnconfirmed() {
        var raw = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("research"));
        var sources = (tools.jackson.databind.node.ArrayNode) raw.path("research").path("sources");
        var duplicate = sources.get(0).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) duplicate).putArray("authors").add("Alternative author spelling");
        sources.add(duplicate);
        var state = start(raw.toString());
        assertEquals(1, state.research.sources().size());
        assertTrue(state.research.sources().getFirst().authors().isEmpty());
        assertFalse(state.issues.isEmpty());
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        assertEquals(1, state.sources.size()); assertEquals(1, state.findings.size());
    }

    @Test void repeatedUrlWithIncompatibleStanceMetadataIsRejected() {
        var raw = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("research"));
        var sources = (tools.jackson.databind.node.ArrayNode) raw.path("research").path("sources");
        var duplicate = sources.get(0).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) duplicate).put("stanceOwner", "THIRD_PARTY");
        sources.add(duplicate);
        assertThrows(RuntimeException.class, () -> start(raw.toString()));
    }

    @Test void inaccessiblePublicationIsALimitationNotAnAssessmentCandidate() {
        var raw = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("research"));
        var sources = (tools.jackson.databind.node.ArrayNode) raw.path("research").path("sources");
        var second = (tools.jackson.databind.node.ObjectNode) sources.get(0).deepCopy();
        second.put("url", "https://unavailable.example/report"); sources.add(second);
        var state = start(raw.toString());
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        EvidencePipeline.captureNext(state, uri -> { throw new IllegalArgumentException("Unavailable"); });
        var packet = MAPPER.valueToTree(EvidencePipeline.synthesisInput(state)).path("acceptedEvidence");
        assertEquals(1, packet.path("sources").size());
        assertEquals(1, packet.path("publicationCandidates").size());
        assertEquals("S2", packet.path("omittedSources").get(0).path("sourceId").asString());
        assertFalse(packet.path("omittedSources").get(0).path("assessmentAllowed").asBoolean());
        assertFalse(packet.path("omittedSources").get(0).path("metadataVerified").asBoolean());
        assertTrue(packet.path("omittedSources").get(0).path("url").asString().contains("unavailable.example"));
        assertTrue(EvidencePipeline.limitations(state).stream().anyMatch(value -> value.contains("unavailable.example")));
        assertFalse(EvidencePipeline.limitations(state).stream().anyMatch(value -> value.startsWith("S2:")));
        var output = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("synthesis"));
        var assessment = (tools.jackson.databind.node.ObjectNode) output.path("publicationPositions").path("assessments").get(0).deepCopy();
        assessment.put("sourceId", "S2"); assessment.put("decision", "EXCLUDE"); assessment.putNull("position");
        assessment.putArray("findingIds").add("S2");
        ((tools.jackson.databind.node.ArrayNode) output.path("publicationPositions").path("assessments")).add(assessment);
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(state, synthesis(output.toString())));
        assertEquals(1, EvidencePipeline.assemble(state, synthesis(fixture("synthesis"))).sources().size());
    }

    @Test void backgroundEvidenceRemainsCitableButCannotBecomeAnEditorialCount() {
        var state = start(fixture("research").replace("REQUESTED_PERIOD", "BACKGROUND"));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        var packet = MAPPER.valueToTree(EvidencePipeline.synthesisInput(state)).path("acceptedEvidence");
        var candidate = packet.path("publicationCandidates").get(0);
        assertEquals("F1_1", packet.path("allowedFindingIds").get(0).asString());
        assertEquals("EXCLUDE", candidate.path("allowedDecisions").get(0).asString());
        assertEquals(1, candidate.path("allowedDecisions").size());
        assertTrue(candidate.path("countEligibleFindingIds").isEmpty());
        assertTrue(candidate.path("restrictions").toString().contains("BACKGROUND"));
        assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(state, synthesis(fixture("synthesis"))));
        var output = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("synthesis"));
        var assessment = (tools.jackson.databind.node.ObjectNode) output.path("publicationPositions").path("assessments").get(0);
        assessment.put("decision", "EXCLUDE"); assessment.putNull("position");
        assessment.put("explanation", "Fuente de contexto fuera del periodo solicitado.");
        ((tools.jackson.databind.node.ObjectNode) output.path("publicationPositions")).put("reason", "Todas las publicaciones evaluadas quedan fuera del periodo solicitado.");
        var report = EvidencePipeline.assemble(state, synthesis(output.toString()));
        assertEquals(VerdictStatus.INSUFFICIENT_EVIDENCE, report.verdict().status());
        assertTrue(report.publicationPositions().units().isEmpty());
        assertEquals(1, report.sources().size());
    }

    @Test void eligiblePublicationReceivesOnlyItsAcceptedFindingIds() {
        var packet = MAPPER.valueToTree(EvidencePipeline.synthesisInput(captured())).path("acceptedEvidence");
        var candidate = packet.path("publicationCandidates").get(0);
        assertEquals("COUNT", candidate.path("allowedDecisions").get(0).asString());
        assertEquals("F1_1", candidate.path("countEligibleFindingIds").get(0).asString());
        assertEquals(candidate.path("allowedFindingIds"), candidate.path("requiredDirectionalFindingIds"));
    }

    @Test void synthesisRequiresAnExplanationEvenWhenPublicationsCount() {
        for (String reason : new String[] {null, "", "   "}) {
            var output = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("synthesis"));
            ((tools.jackson.databind.node.ObjectNode) output.path("publicationPositions")).put("reason", reason);
            assertThrows(RuntimeException.class, () -> EvidencePipeline.assemble(captured(), synthesis(output.toString())));
        }
        assertNotNull(EvidencePipeline.assemble(captured(), synthesis(fixture("synthesis"))));
    }

    @Test void unverifiedResearchImpressionsAreNotAutomaticallyPublishedAsFacts() {
        var raw = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("research"));
        ((tools.jackson.databind.node.ObjectNode) raw.path("research")).putArray("limitations").add("UNVERIFIED_RESEARCH_IMPRESSION");
        var state = start(raw.toString());
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        var packet = MAPPER.valueToTree(EvidencePipeline.synthesisInput(state)).path("acceptedEvidence");
        assertEquals("UNVERIFIED_RESEARCH_IMPRESSION", packet.path("unverifiedResearchNotes").get(0).asString());
        assertFalse(packet.path("limitations").toString().contains("UNVERIFIED_RESEARCH_IMPRESSION"));
        var report = EvidencePipeline.assemble(state, synthesis(fixture("synthesis")));
        assertFalse(report.limitations().contains("UNVERIFIED_RESEARCH_IMPRESSION"));
    }

    @Test void publicSelectionCriteriaCannotBeRewrittenByTheProvider() {
        var output = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(fixture("synthesis"));
        ((tools.jackson.databind.node.ObjectNode) output.path("publicationPositions")).put("selectionCriteria",
                "Solo publicaciones con posición propia elegibles para COUNT.");
        var report = EvidencePipeline.assemble(captured(), synthesis(output.toString()));
        assertEquals(EvidencePipeline.PUBLICATION_SELECTION_CRITERIA, report.publicationPositions().selectionCriteria());
        assertFalse(report.publicationPositions().selectionCriteria().contains("COUNT"));
        assertTrue(report.publicationPositions().selectionCriteria().contains("no expresar una posición explícita"));
    }

    @Test void eligibleSourceWithoutOwnStanceCountsAsNoExplicitPosition() {
        var state = start(fixture("research").replace("\"stanceOwner\": \"AUTHOR\"", "\"stanceOwner\": \"NONE\"")
                .replace("\"relation\": \"SUPPORTS\"", "\"relation\": \"CONTEXT\""));
        EvidencePipeline.captureNext(state, EvidencePipelineTest::document);
        var report = EvidencePipeline.assemble(state, synthesis(fixture("synthesis").replace("\"position\": \"SUPPORTS\"", "\"position\": \"NO_EXPLICIT_POSITION\"")));
        assertEquals(1, report.publicationPositions().units().size());
        assertEquals(PublicationPosition.NO_EXPLICIT_POSITION, report.publicationPositions().units().getFirst().position());
        assertEquals(VerdictStatus.INSUFFICIENT_EVIDENCE, report.verdict().status());
    }
}
