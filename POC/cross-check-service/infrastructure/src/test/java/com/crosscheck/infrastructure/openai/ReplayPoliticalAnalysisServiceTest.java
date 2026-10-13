package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.domain.analysis.AnalysisCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ReplayPoliticalAnalysisServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"PLUS_ULTRA_V19", "VOTO_OBLIGATORIO_V19", "ALQUILERES_V19", "ACLARACION_V19", "PLUS_ULTRA_V20"})
    void recordedOutputUsesProductionParser(String scenario) {
        var service = new ReplayPoliticalAnalysisService(scenario);
        var result = service.analyze(new AiAnalysisInput(service.query(),
                AnalysisCategory.POLITICAL_ANALYSIS, service.revision(), null));
        assertNotNull(result.sessionId());
        if (scenario.equals("ACLARACION_V19")) {
            assertNotNull(result.clarification());
            assertNull(result.report());
        } else {
            assertNotNull(result.report());
            assertNull(result.clarification());
            assertFalse(result.report().sources().isEmpty());
        }
    }

    @Test void rejectsUnrelatedQuestionRatherThanReusingAnAnswer() {
        var service = new ReplayPoliticalAnalysisService("PLUS_ULTRA_V19");
        assertThrows(InvalidAnalysisOutputException.class, () -> service.analyze(
                new AiAnalysisInput("Unrelated question", AnalysisCategory.POLITICAL_ANALYSIS,
                        service.revision(), null)));
    }

    @Test void rejectsUnrecordedFollowUpContext() {
        var service = new ReplayPoliticalAnalysisService("PLUS_ULTRA_V19");
        var context = new com.crosscheck.application.model.ConversationContext(
                service.query(), service.query(), java.util.List.of(service.query()), null);
        assertThrows(InvalidAnalysisOutputException.class, () -> service.analyze(
                new AiAnalysisInput(service.query(), AnalysisCategory.POLITICAL_ANALYSIS,
                        service.revision(), "replay_previous", context)));
    }

    @Test void changingScenarioChangesConversationIdentity() {
        assertNotEquals(new ReplayPoliticalAnalysisService("PLUS_ULTRA_V19").revision(),
                new ReplayPoliticalAnalysisService("VOTO_OBLIGATORIO_V19").revision());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../PLUS_ULTRA_V19", "MISSING", ""})
    void rejectsUnknownOrUnsafeScenario(String scenario) {
        assertThrows(IllegalArgumentException.class, () -> new ReplayPoliticalAnalysisService(scenario));
    }
}
