package com.crosscheck.bootstrap;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.model.AiAnalysisTurn;
import com.crosscheck.application.model.ConversationReference;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentScenario;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "crosscheck.analysis.max-input-length=100", "crosscheck.analysis.max-token-length=512"})
@ActiveProfiles("dev")
@Import(AnalysisApiTest.ProviderTestConfiguration.class)
class AnalysisApiTest {
    private static final String TEST_SECRET = java.util.Base64.getEncoder().encodeToString(
            new java.security.SecureRandom().generateSeed(32));

    @org.springframework.test.context.DynamicPropertySource
    static void secret(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("CONVERSATION_TOKEN_SECRET", () -> TEST_SECRET);
    }
    @LocalServerPort int port;
    @Autowired JsonMapper mapper;
    @Autowired ControlledProvider provider;
    @Autowired ConversationReferenceCodec codec;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @AfterEach
    void closeClient() {
        client.close();
    }

    @BeforeEach
    void resetProvider() {
        provider.calls.set(0);
        provider.scenario = DevelopmentScenario.INSUFFICIENT_EVIDENCE;
        provider.failure = null;
        provider.lastInput = null;
    }

    @Test
    void newQuerySerializesCompleteContractAndDoesNotCacheToken() throws Exception {
        Instant before = Instant.now();
        var response = post("{\"text\":\"Consulta\"}");
        assertEquals(200, response.statusCode());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        var payload = body(response);
        assertEquals(Set.of("conversationToken", "analysis"), payload.keySet());
        assertEquals(5, ((String) payload.get("conversationToken")).split("\\.").length);
        var analysis = child(payload, "analysis");
        assertEquals(Set.of("title", "context", "summary", "summarySourceIds", "verdict", "sources",
                "publicationPositions", "limitations", "analyzedAt", "asOf"), analysis.keySet());
        assertNull(analysis.get("asOf"));
        assertFalse(Instant.parse((String) analysis.get("analyzedAt")).isBefore(before));
        assertEquals("INSUFFICIENT_EVIDENCE", child(analysis, "verdict").get("status"));
        var positions = child(analysis, "publicationPositions");
        assertEquals("UNAVAILABLE", positions.get("availability"));
        assertNull(positions.get("consultedAt"));
        assertNull(positions.get("period"));
        assertTrue(response.body().contains("simulado"));
        assertEquals(1, provider.calls.get());
        assertNull(provider.lastInput.sessionId());
        assertNotNull(UUID.fromString(response.headers().firstValue("X-Request-Id").orElseThrow()));
    }

    @Test
    void nullTokenStartsNewSessionAndFollowUpReusesIt() throws Exception {
        var first = post("{\"text\":\"Consulta\",\"conversationToken\":null}");
        String firstToken = (String) body(first).get("conversationToken");
        var session = codec.decode(firstToken).sessionId();
        var followUp = post(mapper.writeValueAsString(Map.of("text", "Seguimiento", "conversationToken", firstToken)));
        assertEquals(200, followUp.statusCode());
        assertEquals(session, provider.lastInput.sessionId());
        assertTrue(followUp.body().contains("Seguimiento simulado"));
        String nextToken = (String) body(followUp).get("conversationToken");
        assertNotEquals(firstToken, nextToken);
        assertEquals(session, codec.decode(nextToken).sessionId());
        var separate = post("{\"text\":\"Nueva conversación\"}");
        assertNotEquals(session, codec.decode((String) body(separate).get("conversationToken")).sessionId());
    }

    @Test
    void classifiedResultContainsSourceMetadataUnitsAndExclusions() throws Exception {
        provider.scenario = DevelopmentScenario.CLASSIFIED;
        var response = post("{\"text\":\"Consulta\"}");
        assertEquals(200, response.statusCode());
        var analysis = child(body(response), "analysis");
        var positions = child(analysis, "publicationPositions");
        assertEquals("AVAILABLE", positions.get("availability"));
        assertNotNull(Instant.parse((String) positions.get("consultedAt")));
        var units = (java.util.List<?>) positions.get("units");
        assertEquals(2, units.size());
        assertEquals(Set.of("id", "sourceIds", "position", "explanation"), ((Map<?, ?>) units.getFirst()).keySet());
        assertEquals(2, ((java.util.List<?>) ((Map<?, ?>) units.getFirst()).get("sourceIds")).size());
        assertEquals(1, ((java.util.List<?>) positions.get("excluded")).size());
        var sources = (java.util.List<?>) analysis.get("sources");
        assertEquals(4, sources.size());
        assertEquals(Set.of("id", "title", "url", "publisher", "publishedAt", "consultedAt", "contribution", "type"),
                ((Map<?, ?>) sources.getFirst()).keySet());
        assertNull(((Map<?, ?>) sources.getFirst()).get("publishedAt"));
        assertFalse(positions.containsKey("percentage"));
    }

    @ParameterizedTest
    @EnumSource(value = DevelopmentScenario.class, names = {
            "SUPPORTED_BY_PUBLICATIONS", "QUESTIONED_BY_PUBLICATIONS", "TIED_PUBLICATIONS"})
    void derivesFinalVerdictThroughHttpContract(DevelopmentScenario scenario) throws Exception {
        provider.scenario = scenario;
        var response = post("{\"text\":\"Consulta\"}");
        assertEquals(200, response.statusCode());
        var verdict = child(child(body(response), "analysis"), "verdict");
        assertEquals(scenario == DevelopmentScenario.TIED_PUBLICATIONS
                ? "INSUFFICIENT_EVIDENCE" : scenario.name(), verdict.get("status"));
        assertEquals("No evaluado: respuesta simulada", verdict.get("documentarySupport"));
        if (scenario != DevelopmentScenario.TIED_PUBLICATIONS) {
            assertTrue(((String) verdict.get("explanation")).startsWith(
                    "La evidencia disponible no permite respaldar ni refutar la afirmación."));
        }
    }

    @Test
    void zeroUnitsRemainsAvailableWithoutInventedPercentage() throws Exception {
        provider.scenario = DevelopmentScenario.ZERO_UNITS;
        var response = post("{\"text\":\"Consulta\"}");
        assertEquals(200, response.statusCode());
        var positions = child(child(body(response), "analysis"), "publicationPositions");
        assertEquals("AVAILABLE", positions.get("availability"));
        assertEquals(0, ((java.util.List<?>) positions.get("units")).size());
        assertNotNull(positions.get("reason"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"text\":null}", "{\"text\":\"\"}", "{\"text\":\"   \"}"})
    void invalidInputNeverCallsProvider(String json) throws Exception {
        assertError(post(json), 400, "INVALID_ANALYSIS_INPUT", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{", "{\"text\":123}", "{\"text\":true}",
            "{\"text\":1.5}", "{\"text\":[]}", "{\"text\":{}}",
            "{\"text\":\"query\",\"conversationToken\":123}",
            "{\"text\":\"query\",\"conversationToken\":true}",
            "{\"text\":\"query\",\"sessionId\":\"forged\"}",
            "{\"text\":\"query\",\"category\":\"GENERAL\"}",
            "{\"text\":\"query\",\"agentId\":\"forged\"}",
            "{\"text\":\"first\",\"text\":\"second\"}",
            "{\"text\":\"query\"} {\"text\":\"extra\"}"})
    void malformedOrAmbiguousRequestNeverCallsProvider(String json) throws Exception {
        assertError(post(json), 400, "INVALID_REQUEST", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @Test
    void configurableLimitCountsUnicodeCodePoints() throws Exception {
        assertEquals(200, post(mapper.writeValueAsString(Map.of("text", "😀".repeat(100)))).statusCode());
        provider.calls.set(0);
        assertError(post(mapper.writeValueAsString(Map.of("text", "😀".repeat(101)))),
                400, "INVALID_ANALYSIS_INPUT", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "unknown-token"})
    void invalidReferenceNeverCallsProvider(String token) throws Exception {
        assertError(post(mapper.writeValueAsString(Map.of("text", "Consulta", "conversationToken", token))),
                400, "INVALID_CONVERSATION_REFERENCE", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @Test
    void oversizedReferenceIsRejected() throws Exception {
        assertError(post(mapper.writeValueAsString(Map.of("text", "Consulta", "conversationToken", "x".repeat(513)))),
                400, "INVALID_CONVERSATION_REFERENCE", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @Test
    void expiredReferenceIsGoneWithoutCallingProvider() throws Exception {
        String token = codec.encode(new ConversationReference("session", AnalysisCategory.POLITICAL_ANALYSIS,
                "development-v1", Instant.now().minusSeconds(1)));
        assertError(post(mapper.writeValueAsString(Map.of("text", "Consulta", "conversationToken", token))),
                410, "CONVERSATION_REFERENCE_EXPIRED", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @ParameterizedTest
    @EnumSource(value = DevelopmentScenario.class, names = {
            "TIMEOUT", "UNAVAILABLE", "CONFLICT", "SESSION_UNAVAILABLE", "INVALID_OUTPUT"})
    void simulatedFailuresHaveStableHttpContractWithoutRetries(DevelopmentScenario scenario) throws Exception {
        provider.scenario = scenario;
        int status = switch (scenario) {
            case TIMEOUT -> 504;
            case UNAVAILABLE -> 503;
            case CONFLICT -> 409;
            case SESSION_UNAVAILABLE -> 410;
            default -> 502;
        };
        String code = switch (scenario) {
            case TIMEOUT -> "ANALYSIS_TIMEOUT";
            case UNAVAILABLE -> "ANALYSIS_PROVIDER_UNAVAILABLE";
            case CONFLICT -> "ANALYSIS_CONFLICT";
            case SESSION_UNAVAILABLE -> "CONVERSATION_UNAVAILABLE";
            default -> "INVALID_ANALYSIS_OUTPUT";
        };
        String state = scenario == DevelopmentScenario.TIMEOUT || scenario == DevelopmentScenario.INVALID_OUTPUT
                ? "UNKNOWN" : "NOT_STARTED";
        assertError(post("{\"text\":\"Consulta\"}"), status, code, state);
        assertEquals(1, provider.calls.get());
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() throws Exception {
        provider.failure = new IllegalStateException("SECRET_QUERY_AND_TOKEN");
        var response = post("{\"text\":\"Consulta\"}");
        assertError(response, 500, "INTERNAL_ERROR", "UNKNOWN");
        assertFalse(response.body().contains("SECRET"));
        assertFalse(response.body().contains("IllegalStateException"));
        assertEquals(1, provider.calls.get());
    }

    @Test
    void unsupportedMethodAndMediaTypeDoNotCallProvider() throws Exception {
        var get = client.send(HttpRequest.newBuilder(endpoint()).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertError(get, 405, "METHOD_NOT_ALLOWED", "NOT_STARTED");
        assertTrue(get.headers().firstValue("Allow").orElseThrow().contains("POST"));
        var plain = client.send(HttpRequest.newBuilder(endpoint()).header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("query")).build(), HttpResponse.BodyHandlers.ofString());
        assertError(plain, 415, "UNSUPPORTED_MEDIA_TYPE", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @Test
    void unsupportedResponseFormatDoesNotCallProvider() throws Exception {
        var response = client.send(HttpRequest.newBuilder(endpoint()).header("Content-Type", "application/json")
                .header("Accept", "text/plain").POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"Consulta\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertError(response, 406, "NOT_ACCEPTABLE", "NOT_STARTED");
        assertEquals(0, provider.calls.get());
    }

    @Test
    void clientCannotChooseDiagnosticIdentifier() throws Exception {
        var response = client.send(HttpRequest.newBuilder(endpoint()).header("Content-Type", "application/json")
                .header("X-Request-Id", "untrusted").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertError(response, 400, "INVALID_ANALYSIS_INPUT", "NOT_STARTED");
        assertNotEquals("untrusted", body(response).get("requestId"));
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + port + "/api/analysis/start");
    }

    private HttpResponse<String> post(String json) throws Exception {
        return client.send(HttpRequest.newBuilder(endpoint()).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private Map<?, ?> body(HttpResponse<String> response) {
        return mapper.readValue(response.body(), Map.class);
    }

    private Map<?, ?> child(Map<?, ?> map, String key) {
        return (Map<?, ?>) map.get(key);
    }

    private void assertError(HttpResponse<String> response, int status, String code, String state) {
        assertEquals(status, response.statusCode(), response.body());
        var error = body(response);
        assertEquals(Set.of("code", "message", "requestId", "executionState"), error.keySet());
        assertEquals(code, error.get("code"));
        assertEquals(state, error.get("executionState"));
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), error.get("requestId"));
        assertNotNull(UUID.fromString((String) error.get("requestId")));
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderTestConfiguration {
        @Bean
        @Primary
        ControlledProvider controlledProvider(Clock clock) {
            return new ControlledProvider(clock);
        }
    }

    static final class ControlledProvider implements AiPoliticalAnalysisService {
        final AtomicInteger calls = new AtomicInteger();
        final Clock clock;
        volatile DevelopmentScenario scenario = DevelopmentScenario.INSUFFICIENT_EVIDENCE;
        volatile RuntimeException failure;
        volatile AiAnalysisInput lastInput;

        ControlledProvider(Clock clock) { this.clock = clock; }

        public AiAnalysisTurn analyze(AiAnalysisInput input) {
            calls.incrementAndGet();
            lastInput = input;
            if (failure != null) throw failure;
            return new DevelopmentPoliticalAnalysisService(scenario, clock).analyze(input);
        }
    }
}
