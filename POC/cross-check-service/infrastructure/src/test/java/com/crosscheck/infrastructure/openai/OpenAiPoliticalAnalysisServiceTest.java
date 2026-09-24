package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.AnalysisProviderException;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.infrastructure.development.DevelopmentPoliticalAnalysisService;
import com.crosscheck.infrastructure.development.DevelopmentScenario;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;

class OpenAiPoliticalAnalysisServiceTest {
    private HttpServer server;
    private HttpClient client;
    private java.util.concurrent.ExecutorService executor;
    private final ConcurrentLinkedQueue<Reply> replies = new ConcurrentLinkedQueue<>();
    private final List<Request> requests = java.util.Collections.synchronizedList(new ArrayList<>());
    private final CountDownLatch received = new CountDownLatch(1);
    private URI endpoint;

    record Reply(int status, String body, long delay) {}
    record Request(String path, String method, String authorization, String beta, JsonNode body) {}

    @BeforeEach void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/v1/", exchange -> {
            var bytes = exchange.getRequestBody().readAllBytes();
            requests.add(new Request(exchange.getRequestURI().toString(), exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("OpenAI-Beta"),
                    bytes.length == 0 ? null : MAPPER.readTree(bytes)));
            received.countDown();
            Reply reply = replies.poll();
            if (reply == null) reply = new Reply(500, "{}", 0);
            try {
                if (reply.delay() > 0) Thread.sleep(reply.delay());
                byte[] response = reply.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Location", "/v1/redirected");
                exchange.sendResponseHeaders(reply.status(), response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @AfterEach void stopServer() {
        server.stop(0);
        client.close();
        executor.shutdownNow();
    }

    private OpenAiPoliticalAnalysisService provider() {
        return provider(Duration.ofSeconds(3), 2);
    }

    private OpenAiPoliticalAnalysisService provider(Duration timeout, int concurrency) {
        return new OpenAiPoliticalAnalysisService(client, endpoint, "test-key-not-a-credential", "agent_test",
                timeout, Duration.ofMillis(10), concurrency);
    }

    private AiAnalysisInput input(String session) {
        return new AiAnalysisInput("Consulta de prueba", AnalysisCategory.POLITICAL_ANALYSIS, "political-v1", session);
    }

    private void reply(Object value) { replies.add(new Reply(200, MAPPER.writeValueAsString(value), 0)); }

    private ObjectNode turn(String id, String status) {
        var node = MAPPER.createObjectNode();
        node.put("id", id).put("status", status).put("session_id", "sess_test").putNull("subagent_id");
        return node;
    }

    private String report() {
        return MAPPER.writeValueAsString(new DevelopmentPoliticalAnalysisService(
                DevelopmentScenario.CLASSIFIED, Clock.systemUTC()).analyze(input(null)).report());
    }

    private Map<String, Object> message(String turnId, String output) {
        return Map.of("id", "msg_final", "type", "message", "role", "assistant", "phase", "final_answer",
                "status", "completed", "turn_id", turnId,
                "content", List.of(Map.of("type", "output_text", "text", output)));
    }

    private void completed(String output) {
        reply(Map.of("id", "sess_test"));
        reply(Map.of("data", List.of(turn("turn_new", "completed"))));
        reply(Map.of("data", List.of(message("turn_new", output)), "has_more", false));
    }

    @Test void createsSessionUsingSavedAgentAndMapsReport() {
        completed(report());
        var result = provider().analyze(input(null));
        assertEquals("sess_test", result.sessionId());
        assertEquals(2, result.report().publicationPositions().units().size());
        assertEquals(3, requests.size());
        var request = requests.getFirst();
        assertEquals("/v1/agents/sessions", request.path());
        assertEquals("POST", request.method());
        assertEquals("Bearer test-key-not-a-credential", request.authorization());
        assertEquals("agents=v1", request.beta());
        assertEquals("agent_test", request.body().path("agent_id").asString());
        assertEquals("none", request.body().path("environment").path("type").asString());
        assertEquals("Consulta de prueba", request.body().path("input").asString());
        assertFalse(request.body().has("agent"));
        assertFalse(request.body().path("stream").asBoolean());
    }

    @Test void followUpWaitsForNewTurnAndDoesNotReturnPreviousReport() {
        reply(Map.of("id", "sess_test", "status", "idle"));
        reply(Map.of("data", List.of(turn("turn_old", "completed"))));
        reply(Map.of());
        reply(Map.of("data", List.of(turn("turn_old", "completed"))));
        reply(Map.of("data", List.of(turn("turn_new", "in_progress"))));
        reply(Map.of("data", List.of(turn("turn_new", "completed"))));
        reply(Map.of("data", List.of(message("turn_old", "not JSON"), message("turn_new", report())), "has_more", false));
        assertEquals("sess_test", provider().analyze(input("sess_test")).sessionId());
        var submission = requests.get(2);
        assertEquals("/v1/agents/sessions/sess_test/events", submission.path());
        assertEquals("agent.session.input.message", submission.body().path("events").get(0).path("type").asString());
        assertEquals("Consulta de prueba", submission.body().path("events").get(0).path("input").get(0)
                .path("content").get(0).path("text").asString());
        assertEquals(1, requests.stream().filter(r -> r.method().equals("POST")).count());
    }

    @Test void readsPaginatedItemsAndIgnoresCommentary() {
        reply(Map.of("id", "sess_test"));
        reply(Map.of("data", List.of(turn("turn_new", "completed"))));
        var commentary = new java.util.HashMap<>(message("turn_new", "Working..."));
        commentary.put("phase", "commentary");
        reply(Map.of("data", List.of(commentary), "has_more", true, "last_id", "msg_cursor"));
        reply(Map.of("data", List.of(message("turn_new", report())), "has_more", false));
        assertNotNull(provider().analyze(input(null)).report());
        assertTrue(requests.getLast().path().endsWith("&after=msg_cursor"));
    }

    @ParameterizedTest @ValueSource(strings = {"failed", "cancelled", "waiting"})
    void rejectsUnsuccessfulTurns(String status) {
        reply(Map.of("id", "sess_test"));
        reply(Map.of("data", List.of(turn("turn_new", status))));
        var failure = assertThrows(AnalysisProviderException.class, () -> provider().analyze(input(null)));
        assertEquals(AnalysisProviderException.ExecutionState.UNKNOWN, failure.executionState());
        assertEquals(2, requests.size());
    }

    @ParameterizedTest @ValueSource(ints = {301, 400, 401, 403, 404, 409, 429, 500, 504})
    void mapsHttpErrorsWithoutLeakingProviderDetailsOrRetrying(int status) {
        replies.add(new Reply(status, "{\"error\":\"private-provider-detail\"}", 0));
        var failure = assertThrows(AnalysisProviderException.class, () -> provider().analyze(input(null)));
        assertFalse(failure.toString().contains("private-provider-detail"));
        assertNull(failure.getCause());
        assertEquals(1, requests.size());
        assertEquals(status >= 500 ? AnalysisProviderException.ExecutionState.UNKNOWN
                : AnalysisProviderException.ExecutionState.NOT_STARTED, failure.executionState());
        assertEquals(status == 504 ? AnalysisProviderException.Reason.TIMEOUT
                : status == 409 ? AnalysisProviderException.Reason.CONFLICT
                : AnalysisProviderException.Reason.UNAVAILABLE, failure.reason());
    }

    @Test void mapsMissingSessionBeforeSubmittingFollowUp() {
        replies.add(new Reply(404, "{}", 0));
        var failure = assertThrows(AnalysisProviderException.class, () -> provider().analyze(input("sess_test")));
        assertEquals(AnalysisProviderException.Reason.SESSION_UNAVAILABLE, failure.reason());
        assertEquals("GET", requests.getFirst().method());
    }

    @Test void rejectsBusyRemoteSessionWithoutSteeringIt() {
        reply(Map.of("id", "sess_test", "status", "in_progress"));
        var failure = assertThrows(AnalysisProviderException.class, () -> provider().analyze(input("sess_test")));
        assertEquals(AnalysisProviderException.Reason.CONFLICT, failure.reason());
        assertEquals(1, requests.size());
    }

    @Test void boundsTotalWaitWithoutRetry() {
        replies.add(new Reply(200, "{\"id\":\"sess_test\"}", 600));
        var failure = assertThrows(AnalysisProviderException.class,
                () -> provider(Duration.ofMillis(150), 1).analyze(input(null)));
        assertEquals(AnalysisProviderException.Reason.TIMEOUT, failure.reason());
        assertEquals(AnalysisProviderException.ExecutionState.UNKNOWN, failure.executionState());
        assertEquals(1, requests.size());
    }

    @Test void limitsConcurrentRequestsAndReleasesCapacityAfterFailure() throws Exception {
        replies.add(new Reply(401, "{}", 300));
        var service = provider(Duration.ofSeconds(2), 1);
        var running = executor.submit(() -> assertThrows(AnalysisProviderException.class, () -> service.analyze(input(null))));
        assertTrue(received.await(1, TimeUnit.SECONDS));
        var busy = assertThrows(AnalysisProviderException.class, () -> service.analyze(input(null)));
        assertEquals(AnalysisProviderException.ExecutionState.NOT_STARTED, busy.executionState());
        running.get(2, TimeUnit.SECONDS);
        completed(report());
        assertNotNull(service.analyze(input(null)));
    }

    @ParameterizedTest @ValueSource(strings = {"not JSON", "null", "{}", "{\"title\":1}", "[]"})
    void rejectsMalformedModelOutput(String output) {
        completed(output);
        assertThrows(InvalidAnalysisOutputException.class, () -> provider().analyze(input(null)));
    }

    @Test void rejectsUnknownFieldsUnsafeLinksAndBrokenReferences() {
        for (String mutation : List.of("unknown", "url", "reference", "enum", "duplicate")) {
            var output = (ObjectNode) MAPPER.readTree(report());
            switch (mutation) {
                case "unknown" -> output.put("unexpected", true);
                case "url" -> ((ObjectNode) output.path("sources").get(0)).put("url", "javascript:alert(1)");
                case "reference" -> output.putArray("summarySourceIds").add("missing");
                case "enum" -> ((ObjectNode) output.path("verdict")).put("status", 0);
                case "duplicate" -> ((ObjectNode) output.path("sources").get(1)).put("id", "s1");
            }
            completed(MAPPER.writeValueAsString(output));
            assertThrows(InvalidAnalysisOutputException.class, () -> provider().analyze(input(null)), mutation);
        }
    }

    @Test void rejectsOversizedHttpBody() {
        replies.add(new Reply(200, " ".repeat(2_000_001), 0));
        assertThrows(InvalidAnalysisOutputException.class, () -> provider().analyze(input(null)));
    }

    @Test void rejectsSessionIdPathInjectionBeforeNetwork() {
        assertThrows(AnalysisProviderException.class, () -> provider().analyze(input("../other")));
        assertTrue(requests.isEmpty());
    }
}
