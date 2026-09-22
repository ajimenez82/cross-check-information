package com.crosscheck.bootstrap;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.presentation.api.analysis.start.StartAnalysisController;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("default")
class DefaultProfileTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;

    @Test
    void defaultProfileExposesHealthWithoutSimulatedAnalysis() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var health = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/actuator/health")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertTrue(health.body().contains("\"status\":\"UP\""));
            var analysis = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/api/analysis/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"Consulta\"}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(404, analysis.statusCode());
        }
        assertTrue(context.getBeansOfType(AiPoliticalAnalysisService.class).isEmpty());
        assertTrue(context.getBeansOfType(ConversationReferenceCodec.class).isEmpty());
        assertTrue(context.getBeansOfType(StartAnalysisController.class).isEmpty());
    }
}
