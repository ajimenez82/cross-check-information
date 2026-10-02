package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.AnalysisCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;

class OpenAiClaimResponseTest {
    private final AiAnalysisInput input = new AiAnalysisInput("¿Y después?", AnalysisCategory.POLITICAL_ANALYSIS, "test", "session",
            new ConversationContext("La medida aumentó la frecuencia y redujo el precio.", null, List.of(), null));
    private ObjectNode response() throws Exception {
        var root = MAPPER.createObjectNode().put("schemaVersion", "3");
        try (var stream = getClass().getResourceAsStream("/openai/claim-report-example.json")) {
            root.set("analysis", MAPPER.readTree(stream));
        }
        root.putNull("clarification");
        return root;
    }
    @Test void contextualAnchorMustComeFromServerInput() throws Exception {
        var json = response().toString();
        assertNotNull(claimTurn(json, input, "session").report());
        var noContext = new AiAnalysisInput(input.text(), input.category(), input.agentRevision(), input.sessionId());
        assertThrows(InvalidAnalysisOutputException.class, () -> claimTurn(json, noContext, "session"));
    }
    @Test void clarificationHasNoReportAndCannotInventServerRestartReason() throws Exception {
        var root = response();
        root.putNull("analysis");
        root.putObject("clarification").put("question", "¿Qué periodo?").put("reason", "MISSING_PERIOD");
        var turn = claimTurn(root.toString(), input, "session");
        assertNull(turn.report()); assertEquals(Clarification.Reason.MISSING_PERIOD, turn.clarification().reason());
        ((ObjectNode) root.path("clarification")).put("reason", "CONTEXT_UNAVAILABLE");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimTurn(root.toString(), input, "session"));
    }
    @Test void rejectsBothNeitherAndUnversionedResponses() throws Exception {
        var root = response();
        root.putObject("clarification").put("question", "¿Qué periodo?").put("reason", "MISSING_PERIOD");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimTurn(root.toString(), input, "session"));
        root.putNull("analysis").putNull("clarification");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimTurn(root.toString(), input, "session"));
        root.remove("schemaVersion");
        assertThrows(InvalidAnalysisOutputException.class, () -> claimTurn(root.toString(), input, "session"));
    }
}
