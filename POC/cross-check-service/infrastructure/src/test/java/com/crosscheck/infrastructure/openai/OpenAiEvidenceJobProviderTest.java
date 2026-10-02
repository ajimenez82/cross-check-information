package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.model.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;

class OpenAiEvidenceJobProviderTest {
    HttpServer server;
    HttpClient client;
    OpenAiEvidenceJobProvider provider;
    AnalysisJob job;
    final Map<String,tools.jackson.databind.JsonNode> requests = new LinkedHashMap<>();
    final Map<String,String> keys = new HashMap<>();
    int posts, captures;
    String loseStage, wrongStage, researchOutput = EvidencePipelineTest.fixture("research");
    boolean inaccessible;

    @BeforeEach void open() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            String path = exchange.getRequestURI().getPath(); Object response; int status = 200;
            if (exchange.getRequestMethod().equals("POST")) {
                assertEquals("/v1/agents/sessions", path);
                var request = MAPPER.readTree(exchange.getRequestBody().readAllBytes());
                String stage = request.path("metadata").path("crosscheck_stage").asString();
                requests.put(stage, request); keys.put(stage, exchange.getRequestHeaders().getFirst("Idempotency-Key")); posts++;
                response = Map.of("id", "sess_" + stage); if (stage.equals(loseStage)) status = 500;
            } else if (path.equals("/v1/agents/sessions")) {
                var sessions = requests.entrySet().stream().map(entry -> Map.of("id", "sess_" + entry.getKey(),
                        "agent", Map.of("id", "agent_test"), "metadata", Map.of("crosscheck_job", job.id,
                                "crosscheck_stage", wrongStage == null ? entry.getKey() : wrongStage))).toList();
                response = Map.of("data", sessions, "has_more", false);
            } else {
                String stage = path.contains("sess_RESEARCH") ? "RESEARCH" : "SYNTHESIS";
                var turn = new LinkedHashMap<String,Object>();
                turn.put("id", "turn_" + stage); turn.put("session_id", "sess_" + stage); turn.put("subagent_id", null);
                turn.put("status", "completed"); turn.put("usage", Map.of("input_tokens", 100, "output_tokens", 20));
                if (path.endsWith("/turns")) response = Map.of("data", List.of(turn), "has_more", false);
                else if (path.contains("/turns/")) response = turn;
                else if (path.endsWith("/items")) response = Map.of("data", List.of(
                        Map.of("type", "message", "role", "user", "turn_id", "turn_" + stage,
                                "content", List.of(Map.of("type", "input_text", "text", requests.get(stage).path("input").asString()))),
                        Map.of("type", "message", "role", "assistant", "phase", "final_answer", "status", "completed",
                                "turn_id", "turn_" + stage, "content", List.of(Map.of("type", "output_text", "text",
                                stage.equals("RESEARCH") ? researchOutput : EvidencePipelineTest.fixture("synthesis"))))), "has_more", false);
                else response = Map.of("id", "sess_" + stage, "status", "idle");
            }
            byte[] bytes = MAPPER.writeValueAsBytes(response); exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start(); client = HttpClient.newHttpClient();
        var base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
        var service = new OpenAiPoliticalAnalysisService(client, base, "offline-key", "agent_test", Duration.ofSeconds(90), Duration.ofSeconds(1), 2, 3);
        provider = new OpenAiEvidenceJobProvider(new OpenAiTransport(client, base, "offline-key"), service,
                uri -> { captures++; if (inaccessible) throw new IllegalArgumentException(); return EvidencePipelineTest.document(uri); }, "agent_test", "political-v11");
        job = new AnalysisJob(); job.id = UUID.randomUUID().toString(); job.input = EvidencePipelineTest.INPUT;
        job.phase = AnalysisJob.Phase.CREATE_SESSION; provider.initialize(job);
    }
    @AfterEach void close() { server.stop(0); client.close(); }
    void prepare() {
        job.remoteInputHash = provider.submissionFingerprint(job);
        var remote = provider.prepare(job); job.sessionId = remote.sessionId();
        if (job.conversationSessionId == null) job.conversationSessionId = remote.sessionId();
    }
    JobProvider.Observation inspect() {
        var result = provider.inspect(job);
        if (result.sessionId() != null) job.sessionId = result.sessionId();
        if (job.conversationSessionId == null) job.conversationSessionId = job.sessionId;
        if (result.turnId() != null) job.turnId = result.turnId();
        if (result.advance() != null) {
            job.stage = result.advance().stage(); job.stagePayload = result.advance().payload();
            if (result.advance().submit()) { job.sessionId = null; job.turnId = null; }
        }
        return result;
    }
    void toSynthesis() { inspect(); inspect(); inspect(); assertEquals("SYNTHESIS", job.stage); }

    @Test void twoSessionsUseDistinctKeysAndSynthesisHasNoToolsOrSourceCaptureText() {
        prepare(); toSynthesis(); prepare(); var result = inspect();
        assertNotNull(result.result().report()); assertEquals(2, posts); assertEquals(1, captures);
        assertEquals("sess_RESEARCH", result.result().sessionId());
        assertNotEquals(keys.get("RESEARCH"), keys.get("SYNTHESIS"));
        assertEquals(1, requests.get("RESEARCH").path("agent").path("tools").size());
        assertTrue(requests.get("SYNTHESIS").path("agent").path("tools").isEmpty());
        assertFalse(requests.get("SYNTHESIS").path("input").asString().contains("Unselected appendix"));
        assertEquals("A0", requests.get("RESEARCH").path("input").isString()
                ? MAPPER.readTree(requests.get("RESEARCH").path("input").asString()).path("anchorOptions").get(0).path("id").asString() : "");
    }
    @Test void lostResearchCreationRecoversSameTurnWithoutAnotherPost() {
        loseStage = "RESEARCH";
        assertThrows(RuntimeException.class, this::prepare);
        assertEquals("CAPTURE", inspect().advance().stage()); assertEquals(1, posts);
    }
    @Test void lostSynthesisCreationRecoversWithoutRepeatingResearchOrCapture() {
        prepare(); toSynthesis(); loseStage = "SYNTHESIS";
        assertThrows(RuntimeException.class, this::prepare);
        assertNotNull(inspect().result()); assertEquals(2, posts); assertEquals(1, captures);
    }
    @Test void wrongStageMetadataDoesNotRecoverAnotherSession() {
        loseStage = "RESEARCH"; wrongStage = "SYNTHESIS";
        assertThrows(RuntimeException.class, this::prepare);
        assertNull(inspect().advance()); assertNull(job.sessionId); assertEquals(1, posts);
    }
    @Test void clarificationEndsBeforeCaptureOrSynthesis() {
        researchOutput = "{\"schemaVersion\":\"1\",\"research\":null,\"clarification\":{\"question\":\"¿Qué periodo?\",\"reason\":\"MISSING_PERIOD\"}}";
        prepare(); var result = inspect();
        assertNotNull(result.result().clarification()); assertEquals(1, posts); assertEquals(0, captures);
    }
    @Test void inaccessibleEvidenceFailsBeforePayingForSynthesis() {
        inaccessible = true; prepare(); inspect(); inspect();
        var result = inspect(); assertEquals("EVIDENCE_CAPTURE_FAILED", result.failure());
        assertEquals(1, posts); assertTrue(result.remoteTerminal());
    }
    @Test void invalidResearchFailsWithoutRepairCall() {
        researchOutput = researchOutput.replace("\"A0\"", "\"fabricated\""); prepare();
        assertEquals("INVALID_ANALYSIS_OUTPUT", inspect().failure()); assertEquals(1, posts); assertEquals(0, captures);
    }
}
