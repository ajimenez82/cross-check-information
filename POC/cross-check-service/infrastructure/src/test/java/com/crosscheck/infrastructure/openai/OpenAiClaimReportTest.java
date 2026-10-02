package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import org.junit.jupiter.api.*;
import tools.jackson.databind.node.ObjectNode;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;

class OpenAiClaimReportTest {
    static final String INPUT = "La medida aumentó la frecuencia y redujo el precio.";
    private ObjectNode fixture() throws Exception {
        try (var stream = getClass().getResourceAsStream("/openai/claim-report-example.json")) {
            return (ObjectNode) MAPPER.readTree(stream);
        }
    }
    @TestFactory List<DynamicTest> executesReviewedDesignCasesThroughProductionParser() throws Exception {
        var tests = new ArrayList<DynamicTest>();
        try (var stream = getClass().getResourceAsStream("/openai/claim-design-cases.json")) {
            for (var item : MAPPER.readTree(stream).path("cases")) {
                tests.add(DynamicTest.dynamicTest(item.path("name").asString(), () -> {
                    var root = fixture();
                    var claims = ((ObjectNode) item.path("payload")).deepCopy();
                    claims.remove("proposalVersion");
                    root.set("claimAnalysis", claims);
                    if ("REJECT".equals(item.path("expected").asString())) {
                        assertThrows(InvalidAnalysisOutputException.class,
                                () -> claimReport(root.toString(), item.path("inputText").asString()));
                    } else {
                        var report = claimReport(root.toString(), item.path("inputText").asString());
                        assertEquals(item.path("expected").asString(), report.verdict().status().name());
                        assertNotNull(report.claimAnalysis());
                    }
                }));
            }
        }
        return tests;
    }
    @Test void rejectsProviderGlobalVerdictAndLegacyReaders() throws Exception {
        var root = fixture();
        assertThrows(InvalidAnalysisOutputException.class, () -> tracedReport(root.toString()));
        assertThrows(InvalidAnalysisOutputException.class, () -> report(root.toString()));
        root.set("verdict", root.path("claimAnalysis").path("claims").get(0).path("verdict"));
        assertThrows(InvalidAnalysisOutputException.class, () -> claimReport(root.toString(), INPUT));
    }
    @Test void bindsAnchorsToCurrentInput() throws Exception {
        assertThrows(InvalidAnalysisOutputException.class, () -> claimReport(fixture().toString(), "¿Y después?"));
    }
    @Test void refusesMixedPublicationSample() throws Exception {
        var root = fixture();
        ((ObjectNode) root.path("publicationPositions")).put("availability", "AVAILABLE")
                .put("proposition", "Combined claims").put("selectionCriteria", "Combined sample");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimReport(root.toString(), INPUT));
    }
    @Test void unionsClaimReferencesForCutoffValidation() throws Exception {
        var root = fixture();
        root.put("asOf", "2025-12-31");
        ((ObjectNode) root.path("sources").get(0)).put("publishedAt", "2026-01-01");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimReport(root.toString(), INPUT));
    }
}
