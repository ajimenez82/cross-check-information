package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.error.AnalysisProviderException;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.model.AiAnalysisTurn;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import tools.jackson.databind.JsonNode;
import static com.crosscheck.application.error.AnalysisProviderException.Reason.*;
import static com.crosscheck.application.error.AnalysisProviderException.ExecutionState.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;

/** Uses saved Agents API configuration and stable remote sessions without a local conversation store. */
@Slf4j
public final class OpenAiPoliticalAnalysisService implements AiPoliticalAnalysisService {
    private final OpenAiTransport transport;
    private final String agentId;
    private final Duration timeout;
    private final Duration pollInterval;
    private final Semaphore capacity;
    private final int reportSchemaVersion;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public OpenAiPoliticalAnalysisService(HttpClient client, String apiKey, String agentId,
            Duration timeout, Duration pollInterval, int maxConcurrentRequests) {
        this(client, URI.create("https://api.openai.com/v1/"), apiKey, agentId,
                timeout, pollInterval, maxConcurrentRequests);
    }

    public OpenAiPoliticalAnalysisService(HttpClient client, String apiKey, String agentId,
            Duration timeout, Duration pollInterval, int maxConcurrentRequests, int reportSchemaVersion) {
        this(client, URI.create("https://api.openai.com/v1/"), apiKey, agentId,
                timeout, pollInterval, maxConcurrentRequests, reportSchemaVersion);
    }

    // Endpoint injection is package-private and used only by local HTTP contract tests.
    OpenAiPoliticalAnalysisService(HttpClient client, URI baseUri, String apiKey, String agentId,
            Duration timeout, Duration pollInterval, int maxConcurrentRequests) {
        this(client, baseUri, apiKey, agentId, timeout, pollInterval, maxConcurrentRequests, 1);
    }

    OpenAiPoliticalAnalysisService(HttpClient client, URI baseUri, String apiKey, String agentId,
            Duration timeout, Duration pollInterval, int maxConcurrentRequests, int reportSchemaVersion) {
        if (reportSchemaVersion < 1 || reportSchemaVersion > 3) {
            throw new IllegalArgumentException("Unsupported OpenAI report schema version.");
        }
        this.reportSchemaVersion = reportSchemaVersion;
        if (apiKey == null || apiKey.isBlank() || apiKey.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException("Configure a non-empty OPENAI_API_KEY for the openai profile.");
        }
        if (agentId == null || !agentId.matches("[A-Za-z0-9_-]{1,200}")) {
            throw new IllegalArgumentException("Configure OPENAI_POLITICAL_ANALYSIS_AGENT_ID for the openai profile.");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()
                || timeout.compareTo(Duration.ofMinutes(5)) > 0 || pollInterval == null
                || pollInterval.isNegative() || pollInterval.isZero() || pollInterval.compareTo(timeout) >= 0
                || maxConcurrentRequests < 1 || maxConcurrentRequests > 20) {
            throw new IllegalArgumentException("Invalid OpenAI timeout, poll interval or concurrency configuration.");
        }
        this.transport = new OpenAiTransport(client, baseUri, apiKey);
        this.agentId = agentId;
        this.timeout = timeout;
        this.pollInterval = pollInterval;
        this.capacity = new Semaphore(maxConcurrentRequests);
    }

    public com.crosscheck.application.features.analysis.jobs.JobProvider jobProvider(String revision) {
        if (reportSchemaVersion != 3) throw new IllegalArgumentException("Async analysis requires schema version 3");
        return new OpenAiJobProvider(transport, agentId, revision, this);
    }

    public com.crosscheck.application.features.analysis.jobs.JobProvider evidenceJobProvider(String revision, java.time.Clock clock) {
        if (reportSchemaVersion != 3) throw new IllegalArgumentException("Evidence pipeline requires public schema version 3");
        return new OpenAiEvidenceJobProvider(transport, this, new com.crosscheck.infrastructure.evidence.PublicSourceCapture(clock), agentId, revision);
    }

    @Override
    public AiAnalysisTurn analyze(AiAnalysisInput input) {
        long started = System.nanoTime();
        String sessionId = input.sessionId();
        if (sessionId != null && !sessionId.matches("[A-Za-z0-9_-]{1,200}")) {
            throw new AnalysisProviderException(SESSION_UNAVAILABLE, NOT_STARTED);
        }
        if (!capacity.tryAcquire()) throw new AnalysisProviderException(UNAVAILABLE, NOT_STARTED);
        boolean locked = false;
        try {
            if (sessionId != null) {
                locked = activeSessions.add(sessionId);
                if (!locked) throw new AnalysisProviderException(CONFLICT, NOT_STARTED);
            }
            log.info("Analysis provider started: requestId={}, followUp={}", MDC.get("requestId"), sessionId != null);
            return execute(input, System.nanoTime() + timeout.toNanos());
        } catch (RuntimeException failure) {
            log.warn("Analysis provider failed: requestId={}, type={}, elapsedMs={}",
                    MDC.get("requestId"), failure.getClass().getSimpleName(), (System.nanoTime() - started) / 1_000_000);
            throw failure;
        } finally {
            log.info("Analysis provider finished: requestId={}, elapsedMs={}",
                    MDC.get("requestId"), (System.nanoTime() - started) / 1_000_000);
            if (locked) activeSessions.remove(sessionId);
            capacity.release();
        }
    }

    private AiAnalysisTurn execute(AiAnalysisInput input, long deadline) {
        String sessionId = input.sessionId();
        String previousTurn = null;
        if (reportSchemaVersion == 3 && sessionId != null && input.context() == null) {
            return new AiAnalysisTurn(sessionId, null, new com.crosscheck.application.model.Clarification(
                    "Escribe la consulta completa para recuperar el contexto del análisis.",
                    com.crosscheck.application.model.Clarification.Reason.CONTEXT_UNAVAILABLE));
        }
        String requestText = input.text();
        if (reportSchemaVersion == 3) {
            var envelope = new java.util.LinkedHashMap<String, Object>();
            envelope.put("currentInput", input.text());
            envelope.put("previousContext", input.context());
            requestText = MAPPER.writeValueAsString(envelope);
        }
        if (sessionId == null) {
            var session = transport.request("agents/sessions", Map.of("agent_id", agentId,
                    "environment", Map.of("type", "none"), "input", requestText, "stream", false),
                    deadline, false, false);
            sessionId = id(session, "id");
        } else {
            var session = transport.request("agents/sessions/" + sessionId, null, deadline, true, false);
            if (!sessionId.equals(id(session, "id"))) throw new InvalidAnalysisOutputException();
            String status = text(session, "status");
            if (!"idle".equals(status)) {
                throw new AnalysisProviderException("failed".equals(status) ? SESSION_UNAVAILABLE : CONFLICT,
                        NOT_STARTED);
            }
            var latest = latestTurn(sessionId, deadline, false);
            previousTurn = latest == null ? null : id(latest, "id");
            if (latest != null && !Set.of("completed", "failed", "cancelled").contains(text(latest, "status"))) {
                throw new AnalysisProviderException(CONFLICT, NOT_STARTED);
            }
            transport.request("agents/sessions/" + sessionId + "/events",
                    Map.of("events", List.of(Map.of("type", "agent.session.input.message", "input",
                            List.of(Map.of("role", "user", "content",
                                    List.of(Map.of("type", "input_text", "text", requestText))))))),
                    deadline, true, false);
        }

        log.info("Analysis input submitted: requestId={}", MDC.get("requestId"));
        // Poll saved turn state, never infer success from an idle session or an old final message.
        while (true) {
            var turn = latestTurn(sessionId, deadline, true);
            if (turn != null && !id(turn, "id").equals(previousTurn)) {
                if (!sessionId.equals(text(turn, "session_id")) || !turn.path("subagent_id").isNull()) {
                    throw new InvalidAnalysisOutputException();
                }
                switch (text(turn, "status")) {
                    case "completed" -> {
                        log.info("Analysis remote turn completed: requestId={}", MDC.get("requestId"));
                        var report = readReport(sessionId, id(turn, "id"), deadline, input);
                        log.info("Analysis report validated: requestId={}", MDC.get("requestId"));
                        var usage = turn.path("usage");
                        if (usage.path("input_tokens").isIntegralNumber()
                                && usage.path("output_tokens").isIntegralNumber()) {
                            log.info("OpenAI analysis completed: inputTokens={}, outputTokens={}",
                                    usage.path("input_tokens").asLong(), usage.path("output_tokens").asLong());
                        }
                        return report;
                    }
                    case "failed", "cancelled", "waiting" -> throw new AnalysisProviderException(UNAVAILABLE, UNKNOWN);
                    case "queued", "in_progress" -> { }
                    default -> throw new InvalidAnalysisOutputException();
                }
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new AnalysisProviderException(TIMEOUT, UNKNOWN);
            try {
                Thread.sleep(Duration.ofNanos(Math.min(remaining, pollInterval.toNanos())));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AnalysisProviderException(UNAVAILABLE, UNKNOWN);
            }
        }
    }

    private JsonNode latestTurn(String sessionId, long deadline, boolean submitted) {
        var turns = data(transport.request("agents/sessions/" + sessionId + "/turns?order=desc&limit=1",
                null, deadline, true, submitted));
        return turns.isEmpty() ? null : turns.get(0);
    }

    AiAnalysisTurn readReport(String sessionId, String turnId, long deadline, AiAnalysisInput input) {
        var result = readOutput(sessionId, turnId, deadline);
        return reportSchemaVersion == 3 ? claimTurn(result, input, sessionId)
                : new AiAnalysisTurn(sessionId, reportSchemaVersion == 2 ? tracedReport(result) : report(result));
    }

    String readOutput(String sessionId, String turnId, long deadline) {
        String cursor = null;
        Set<String> cursors = new HashSet<>();
        String result = null;
        for (int page = 0; page < 20; page++) {
            var response = transport.request("agents/sessions/" + sessionId + "/items?order=desc&limit=100"
                    + (cursor == null ? "" : "&after=" + cursor), null, deadline, true, true);
            for (var item : data(response)) {
                if (!turnId.equals(item.path("turn_id").asString())
                        || !"message".equals(item.path("type").asString())
                        || !"assistant".equals(item.path("role").asString())
                        || !"final_answer".equals(item.path("phase").asString())) continue;
                if (!"completed".equals(item.path("status").asString()) || result != null
                        || !item.path("content").isArray()) throw new InvalidAnalysisOutputException();
                var output = new StringBuilder();
                for (var part : item.path("content")) {
                    if (!"output_text".equals(part.path("type").asString())) throw new InvalidAnalysisOutputException();
                    output.append(text(part, "text"));
                }
                result = output.toString();
            }
            if (!response.path("has_more").isBoolean()) throw new InvalidAnalysisOutputException();
            if (!response.path("has_more").asBoolean()) {
                if (result == null || result.length() > 250_000) throw new InvalidAnalysisOutputException();
                return result;
            }
            cursor = id(response, "last_id");
            if (!cursors.add(cursor)) throw new InvalidAnalysisOutputException();
        }
        throw new InvalidAnalysisOutputException();
    }
}
