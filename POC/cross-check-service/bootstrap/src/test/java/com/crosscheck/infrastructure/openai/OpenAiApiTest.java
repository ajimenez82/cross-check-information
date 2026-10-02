package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.bootstrap.CrossCheckApplication;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentScenario;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CrossCheckApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"crosscheck.analysis.jobs.enabled=false", "crosscheck.openai.evidence-pipeline-enabled=false", "OPENAI_API_KEY=offline-test-key", "crosscheck.openai.political-analysis-agent-id=agent_test"})
@ActiveProfiles("openai")
@Import(OpenAiApiTest.LocalProvider.class)
class OpenAiApiTest {
    private static volatile boolean invalidDates;
    private static volatile int schemaVersion = 2;
    private static volatile String currentInput;
    private static volatile boolean receivedContext;
    private static final AtomicInteger turn = new AtomicInteger();
    private static final HttpServer upstream = startUpstream();
    private static final String secret = java.util.Base64.getEncoder().encodeToString(
            new java.security.SecureRandom().generateSeed(32));
    @LocalServerPort int port;
    @Autowired ConversationReferenceCodec codec;

    @DynamicPropertySource static void configuration(DynamicPropertyRegistry registry) {
        registry.add("CONVERSATION_TOKEN_SECRET", () -> secret);
    }

    private static HttpServer startUpstream() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/", exchange -> {
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                String path = exchange.getRequestURI().getPath();
                Object response;
                if ("POST".equals(exchange.getRequestMethod())) {
                    if (schemaVersion == 3) {
                        var body = MAPPER.readTree(requestBody);
                        String input = path.endsWith("/events")
                                ? body.path("events").get(0).path("input").get(0).path("content").get(0).path("text").asString()
                                : body.path("input").asString();
                        var envelope = MAPPER.readTree(input);
                        currentInput = envelope.path("currentInput").asString();
                        receivedContext = !envelope.path("previousContext").isNull();
                    }
                    invalidDates = new String(requestBody, java.nio.charset.StandardCharsets.UTF_8)
                            .contains("temporal-invalid");
                    turn.incrementAndGet();
                    response = Map.of("id", "sess_local");
                } else if (path.endsWith("/turns")) {
                    var current = MAPPER.createObjectNode().put("id", "turn_" + turn.get())
                            .put("status", "completed").put("session_id", "sess_local").putNull("subagent_id");
                    response = Map.of("data", List.of(current));
                } else if (path.endsWith("/items")) {
                    tools.jackson.databind.node.ObjectNode output;
                    try (var stream = OpenAiApiTest.class.getResourceAsStream(schemaVersion == 3
                            ? "/openai/claim-report-example.json" : "/openai/traced-report-example.json")) {
                        output = (tools.jackson.databind.node.ObjectNode) MAPPER.readTree(stream);
                    }
                    if (invalidDates) {
                        ((tools.jackson.databind.node.ObjectNode) output.path("sources").get(0))
                                .put("publishedAt", "2026-06-09");
                        output.put("asOf", "2025-12-31");
                    }
                    if (schemaVersion == 3) {
                        var envelope = MAPPER.createObjectNode().put("schemaVersion", "3");
                        if ("¿Y después?".equals(currentInput)) {
                            envelope.putNull("analysis");
                            envelope.putObject("clarification").put("question", "¿Qué periodo posterior quieres analizar?")
                                    .put("reason", "MISSING_PERIOD");
                        } else {
                            envelope.set("analysis", output);
                            envelope.putNull("clarification");
                        }
                        output = envelope;
                    }
                    response = Map.of("has_more", false, "data", List.of(Map.of("type", "message",
                            "role", "assistant", "phase", "final_answer", "status", "completed",
                            "turn_id", "turn_" + turn.get(), "content", List.of(Map.of("type", "output_text",
                                    "text", MAPPER.writeValueAsString(output))))));
                } else {
                    response = Map.of("id", "sess_local", "status", "idle");
                }
                byte[] bytes = MAPPER.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @AfterAll static void stopUpstream() { upstream.stop(0); }

    @Test void claimContractClarifiesAndResumesWithAuthenticatedContext() throws Exception {
        schemaVersion = 3;
        try (var client = HttpClient.newHttpClient()) {
            String token = null;
            for (String query : List.of("La medida aumentó la frecuencia y redujo el precio.", "¿Y después?", "Entre 2026 y 2027")) {
                var input = MAPPER.createObjectNode().put("text", query);
                if (token == null) input.putNull("conversationToken"); else input.put("conversationToken", token);
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analysis/start"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(input.toString()))
                        .build(), HttpResponse.BodyHandlers.ofString());
                var body = MAPPER.readTree(response.body());
                if (token == null) {
                    assertEquals(200, response.statusCode());
                    var report = body.path("analysis");
                    assertEquals("NO_SINGLE_VERDICT", report.path("verdict").path("status").asString());
                    assertEquals(2, report.path("claimAnalysis").path("claims").size());
                    assertEquals("UNAVAILABLE", report.path("publicationPositions").path("availability").asString());
                    token = body.path("conversationToken").asString();
                    assertNotNull(codec.decode(token).contextId());
                    assertFalse(receivedContext);
                } else if (query.equals("¿Y después?")) {
                    assertEquals(200, response.statusCode());
                    assertTrue(body.path("analysis").isNull());
                    assertEquals("MISSING_PERIOD", body.path("clarification").path("reason").asString());
                    token = body.path("conversationToken").asString();
                    assertTrue(receivedContext);
                } else {
                    assertEquals(200, response.statusCode());
                    assertTrue(body.path("clarification").isNull());
                    assertEquals(2, body.path("analysis").path("claimAnalysis").path("claims").size());
                    assertTrue(receivedContext);
                }
            }
        } finally { schemaVersion = 2; }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "browser.claims", matches = "true")
    void browserDisplaysClaimResultsFromRealApi() throws Exception {
        schemaVersion = 3;
        try {
            var script = java.nio.file.Path.of("../../webapp/scripts/verify-claims.cjs").toAbsolutePath();
            var builder = new ProcessBuilder("node", script.toString()).inheritIO();
            builder.environment().put("TEST_API_URL", "http://127.0.0.1:" + port);
            var process = builder.start();
            if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly(); fail("Browser verification timed out");
            }
            assertEquals(0, process.exitValue());
        } finally { schemaVersion = 2; }
    }

    @Test void openAiProfileServesApiAndEncryptsStableRemoteSession() throws Exception {
        int before = turn.get();
        try (var client = HttpClient.newHttpClient()) {
            String token = null;
            for (int index = 0; index < 2; index++) {
                var body = MAPPER.createObjectNode().put("text", "Consulta " + index);
                if (token == null) body.putNull("conversationToken"); else body.put("conversationToken", token);
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analysis/start"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertFalse(response.body().contains("sess_local"));
                assertFalse(response.body().contains("offline-test-key"));
                var analysis = MAPPER.readTree(response.body()).path("analysis");
                assertEquals("AUTHOR", analysis.path("publicationPositions").path("units").get(0)
                        .path("trace").path("stanceOwner").asText());
                assertEquals("BACKGROUND", analysis.path("publicationPositions").path("excluded").get(0)
                        .path("trace").path("temporalRelation").asText());
                token = MAPPER.readTree(response.body()).path("conversationToken").asString();
                assertEquals("sess_local", codec.decode(token).sessionId());
                assertEquals("political-v15", codec.decode(token).agentRevision());
            }
            assertEquals(before + 2, turn.get());
        }
    }

    @Test void temporalFailureAllowsNewQueryWithoutAutomaticRetry() throws Exception {
        int before = turn.get();
        try (var client = HttpClient.newHttpClient()) {
            for (String query : List.of("temporal-invalid", "valid-new-query")) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analysis/start"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"" + query + "\",\"conversationToken\":null}"))
                        .build(), HttpResponse.BodyHandlers.ofString());
                var body = MAPPER.readTree(response.body());
                if (query.equals("temporal-invalid")) {
                    assertEquals(502, response.statusCode());
                    assertEquals("INVALID_ANALYSIS_OUTPUT", body.path("code").asText());
                    assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), body.path("requestId").asText());
                    assertFalse(body.has("analysis"));
                    assertFalse(body.has("conversationToken"));
                    assertEquals(before + 1, turn.get());
                } else {
                    assertEquals(200, response.statusCode());
                    assertTrue(body.has("analysis"));
                    assertEquals(before + 2, turn.get());
                }
            }
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "browser.temporal", matches = "true")
    void browserHandlesTemporalFailureAndRecovery() throws Exception {
        int before = turn.get();
        var script = java.nio.file.Path.of("../../webapp/scripts/verify-temporal-error.cjs").toAbsolutePath();
        var builder = new ProcessBuilder("node", script.toString()).inheritIO();
        builder.environment().put("TEST_API_URL", "http://127.0.0.1:" + port);
        var process = builder.start();
        if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Browser verification timed out");
        }
        assertEquals(0, process.exitValue());
        assertEquals(before + 2, turn.get());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalProvider {
        @Bean @Primary
        AiPoliticalAnalysisService localOpenAiProvider(HttpClient openAiHttpClient) {
            var legacy = new OpenAiPoliticalAnalysisService(openAiHttpClient,
                    URI.create("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1/"),
                    "offline-test-key", "agent_test", Duration.ofSeconds(5), Duration.ofMillis(10), 2, 2);
            var claims = new OpenAiPoliticalAnalysisService(openAiHttpClient,
                    URI.create("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1/"),
                    "offline-test-key", "agent_test", Duration.ofSeconds(5), Duration.ofMillis(10), 2, 3);
            return input -> (schemaVersion == 3 ? claims : legacy).analyze(input);
        }
    }
}
