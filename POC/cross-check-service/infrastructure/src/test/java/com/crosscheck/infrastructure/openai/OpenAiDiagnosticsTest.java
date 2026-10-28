package com.crosscheck.infrastructure.openai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OpenAiDiagnosticsTest {
    @Test void turnDiagnosticsHandleBillingMissingErrorsAndUnknownCodes() {
        var turn = OpenAiJson.MAPPER.createObjectNode().put("status", "failed");
        turn.putObject("error").put("code", "usage_limit_exceeded").put("message", "private query");
        assertEquals("usage_limit_exceeded", OpenAiTransport.diagnosticCode(turn));
        assertEquals("usage_limit_exceeded", OpenAiTransport.diagnosticCode(turn.toString().getBytes()));
        turn.putObject("error").put("code", "private query");
        assertEquals("unrecognized_provider_error", OpenAiTransport.diagnosticCode(turn));
        turn.putNull("error");
        assertEquals("unrecognized_provider_error", OpenAiTransport.diagnosticCode(turn));
        assertDoesNotThrow(() -> OpenAiTransport.logTurnFailure(turn, null, "sess_test", "turn_test"));
    }
    @Test void safeDiagnosticCodes() {
        assertEquals("insufficient_quota", OpenAiTransport.diagnosticCode("{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"secret\"}}".getBytes()));
        assertEquals("unrecognized_provider_error", OpenAiTransport.diagnosticCode("{\"error\":{\"code\":\"secret\"}}".getBytes()));
        assertEquals("unreadable_provider_error", OpenAiTransport.diagnosticCode("bad json".getBytes()));
        assertEquals("unavailable", OpenAiTransport.diagnosticId("id\nsecret"));
        assertEquals("req_123", OpenAiTransport.diagnosticId("req_123"));
    }
}