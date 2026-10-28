package com.crosscheck.infrastructure.openai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OpenAiDiagnosticsTest {
    @Test void safeDiagnosticCodes() {
        assertEquals("insufficient_quota", OpenAiTransport.diagnosticCode("{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"secret\"}}".getBytes()));
        assertEquals("unrecognized_provider_error", OpenAiTransport.diagnosticCode("{\"error\":{\"code\":\"secret\"}}".getBytes()));
        assertEquals("unreadable_provider_error", OpenAiTransport.diagnosticCode("bad json".getBytes()));
        assertEquals("unavailable", OpenAiTransport.diagnosticId("id\nsecret"));
        assertEquals("req_123", OpenAiTransport.diagnosticId("req_123"));
    }
}