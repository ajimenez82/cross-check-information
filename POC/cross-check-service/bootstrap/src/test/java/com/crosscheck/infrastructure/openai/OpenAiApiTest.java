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
        properties = {"OPENAI_API_KEY=offline-test-key", "crosscheck.openai.political-analysis-agent-id=agent_test"})
@ActiveProfiles("openai")
@Import(OpenAiApiTest.LocalProvider.class)
class OpenAiApiTest {
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
                exchange.getRequestBody().readAllBytes();
                String path = exchange.getRequestURI().getPath();
                Object response;
                if ("POST".equals(exchange.getRequestMethod())) {
                    turn.incrementAndGet();
                    response = Map.of("id", "sess_local");
                } else if (path.endsWith("/turns")) {
                    var current = MAPPER.createObjectNode().put("id", "turn_" + turn.get())
                            .put("status", "completed").put("session_id", "sess_local").putNull("subagent_id");
                    response = Map.of("data", List.of(current));
                } else if (path.endsWith("/items")) {
                    var report = new DevelopmentPoliticalAnalysisService(DevelopmentScenario.CLASSIFIED, Clock.systemUTC())
                            .analyze(new AiAnalysisInput("Fixture", AnalysisCategory.POLITICAL_ANALYSIS, "test", null)).report();
                    response = Map.of("has_more", false, "data", List.of(Map.of("type", "message",
                            "role", "assistant", "phase", "final_answer", "status", "completed",
                            "turn_id", "turn_" + turn.get(), "content", List.of(Map.of("type", "output_text",
                                    "text", MAPPER.writeValueAsString(report))))));
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

    @Test void openAiProfileServesApiAndEncryptsStableRemoteSession() throws Exception {
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
                token = MAPPER.readTree(response.body()).path("conversationToken").asString();
                assertEquals("sess_local", codec.decode(token).sessionId());
                assertEquals("political-v1", codec.decode(token).agentRevision());
            }
            assertEquals(2, turn.get());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalProvider {
        @Bean @Primary
        AiPoliticalAnalysisService localOpenAiProvider(HttpClient openAiHttpClient) {
            return new OpenAiPoliticalAnalysisService(openAiHttpClient,
                    URI.create("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1/"),
                    "offline-test-key", "agent_test", Duration.ofSeconds(5), Duration.ofMillis(10), 2);
        }
    }
}
