package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentScenario;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiReportTest {
    private ObjectNode fixture() {
        var report = new DevelopmentPoliticalAnalysisService(DevelopmentScenario.CLASSIFIED, Clock.systemUTC())
                .analyze(new AiAnalysisInput("Fixture", AnalysisCategory.POLITICAL_ANALYSIS, "test", null)).report();
        var output = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(report));
        output.remove("claimAnalysis");
        return output;
    }

    private void dates(ObjectNode output) {
        for (var source : output.path("sources")) {
            ((ObjectNode) source).put("publishedAt", "2024-06-01");
            ((ObjectNode) source).put("consultedAt", "2026-09-26T00:00:00Z");
        }
        ((ObjectNode) output.path("publicationPositions")).put("consultedAt", "2026-09-26T00:00:00Z");
    }

    @Test void derivesGroupedSampleWindowAndIgnoresExcludedDatesAndModelWindow() {
        var output = fixture();
        dates(output);
        ((ObjectNode) output.path("sources").get(0)).put("publishedAt", "2024-02-01");
        ((ObjectNode) output.path("sources").get(1)).put("publishedAt", "2024-12-01");
        ((ObjectNode) output.path("sources").get(3)).put("publishedAt", "2020-01-01");
        ((ObjectNode) output.path("publicationPositions")).putObject("period")
                .put("from", "2025-01-01").put("to", "2025-12-31");
        var result = report(output.toString());
        assertEquals(LocalDate.parse("2024-02-01"), result.publicationPositions().period().from());
        assertEquals(LocalDate.parse("2024-12-01"), result.publicationPositions().period().to());
        assertTrue(result.sources().stream().allMatch(source -> source.consultedAt() == null));
        assertNull(result.publicationPositions().consultedAt());
    }

    @Test void unknownClassifiedDateLeavesPeriodUnknown() {
        var output = fixture();
        dates(output);
        ((ObjectNode) output.path("sources").get(1)).putNull("publishedAt");
        assertNull(report(output.toString()).publicationPositions().period());
    }

    @Test void equalDatesProduceSingleDayWindow() {
        var output = fixture();
        dates(output);
        var period = report(output.toString()).publicationPositions().period();
        assertEquals(period.from(), period.to());
        assertEquals(LocalDate.parse("2024-06-01"), period.from());
    }

    @Test void emptySampleHasNoWindow() {
        var output = fixture();
        dates(output);
        var positions = (ObjectNode) output.path("publicationPositions");
        positions.putArray("units");
        positions.put("reason", "No eligible publications");
        assertNull(report(output.toString()).publicationPositions().period());
    }

    @Test void metadataDerivationDoesNotHideInvalidReferencesOrCutoff() {
        var output = fixture();
        dates(output);
        output.put("asOf", "2023-12-31");
        assertThrows(InvalidAnalysisOutputException.class, () -> report(output.toString()));
        output.putNull("asOf");
        output.putArray("summarySourceIds").add("missing");
        assertThrows(InvalidAnalysisOutputException.class, () -> report(output.toString()));
    }

    @Test void stillRejectsMissingFieldsAndMalformedProviderMetadata() {
        var output = fixture();
        ((ObjectNode) output.path("sources").get(0)).remove("consultedAt");
        assertThrows(InvalidAnalysisOutputException.class, () -> report(output.toString()));
        ((ObjectNode) output.path("sources").get(0)).put("consultedAt", "not-a-timestamp");
        assertThrows(InvalidAnalysisOutputException.class, () -> report(output.toString()));
    }
}
