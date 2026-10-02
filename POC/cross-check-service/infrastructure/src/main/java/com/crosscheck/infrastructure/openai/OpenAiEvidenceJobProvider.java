package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.model.AiAnalysisTurn;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.infrastructure.evidence.SourceCapture;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.JsonNode;
import static com.crosscheck.infrastructure.openai.OpenAiJson.*;

/** Research -> independently captured evidence -> synthesis, with one durable submission per remote stage. */
final class OpenAiEvidenceJobProvider implements JobProvider {
    static final String RESEARCH = "RESEARCH", CAPTURE = "CAPTURE", SYNTHESIS = "SYNTHESIS";
    private final OpenAiTransport transport;
    private final OpenAiPoliticalAnalysisService reader;
    private final SourceCapture capture;
    private final String agentId, revision;
    OpenAiEvidenceJobProvider(OpenAiTransport transport, OpenAiPoliticalAnalysisService reader, SourceCapture capture,
            String agentId, String revision) {
        this.transport = transport; this.reader = reader; this.capture = capture; this.agentId = agentId; this.revision = revision;
    }
    public String identity() { return "openai-evidence:" + agentId + ":" + revision + ":1"; }
    public boolean submitsDuringPrepare() { return true; }
    public boolean startsNewSession(AnalysisJob job) { return true; }
    public void initialize(AnalysisJob job) { job.stage = RESEARCH; job.sessionId = null; }
    public String resultSessionId(AnalysisJob job) { return job.conversationSessionId; }
    public boolean remoteMayBeRunning(AnalysisJob job) {
        return !CAPTURE.equals(job.stage) && job.phase != AnalysisJob.Phase.NEXT_STAGE;
    }

    static Map<String,Object> configuration(String stage) {
        String name = RESEARCH.equals(stage) ? "research" : "synthesis";
        return Map.of("model", "gpt-5.4-mini", "service_tier", "default", "reasoning", Map.of("effort", "low"),
                "instructions", resource(name + ".md"), "tools", RESEARCH.equals(stage)
                        ? List.of(Map.of("type", "web_search", "mode", "live")) : List.of(),
                "multi_agent", Map.of("enabled", false),
                "text", Map.of("format", Map.of("type", "json_schema",
                        "schema", MAPPER.readTree(resource(name + ".schema.json")))));
    }
    private static String resource(String name) {
        try (var input = OpenAiEvidenceJobProvider.class.getResourceAsStream("/openai/evidence/" + name)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception failure) { throw new IllegalStateException("Missing evidence pipeline configuration"); }
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private String inputText(AnalysisJob job) {
        var input = new LinkedHashMap<String,Object>();
        input.put("analysisRequestId", job.id); input.put("stage", job.stage);
        if (RESEARCH.equals(job.stage)) {
            input.put("currentInput", job.input.text()); input.put("previousContext", job.input.context());
            input.put("anchorOptions", EvidencePipeline.anchors(job.input));
        } else if (SYNTHESIS.equals(job.stage)) input.putAll((Map<String,Object>) EvidencePipeline.synthesisInput(state(job)));
        else throw new IllegalStateException("No remote input in capture stage");
        return MAPPER.writeValueAsString(input);
    }
    public String submissionFingerprint(AnalysisJob job) { return hash(inputText(job)); }
    private static EvidencePipeline.State state(AnalysisJob job) {
        return MAPPER.readValue(job.stagePayload, EvidencePipeline.State.class);
    }
    private long deadline(AnalysisJob job) {
        long seconds = job.terminal() || job.deadline == null ? 15 : Math.min(15, Math.max(1, Duration.between(java.time.Instant.now(), job.deadline).toSeconds()));
        return System.nanoTime() + Duration.ofSeconds(seconds).toNanos();
    }
    private JsonNode get(String path, long deadline) { return transport.request(path, null, deadline, true, true); }
    public Remote prepare(AnalysisJob job) {
        var session = transport.request("agents/sessions", Map.of("agent_id", agentId, "agent", configuration(job.stage),
                "environment", Map.of("type", "none"), "metadata", Map.of("crosscheck_job", job.id, "crosscheck_stage", job.stage),
                "input", inputText(job), "stream", false), deadline(job), false, false, "crosscheck-" + job.id + "-" + job.stage);
        return new Remote(id(session, "id"), null);
    }
    public void submit(AnalysisJob job) { throw new IllegalStateException("Evidence stages submit only during session creation"); }

    private List<JsonNode> pages(String path, long end) {
        var result = new ArrayList<JsonNode>(); var cursors = new HashSet<String>(); String cursor = null;
        for (int page = 0; page < 20; page++) {
            var response = get(path + (cursor == null ? "" : "&after=" + cursor), end);
            data(response).forEach(result::add);
            if (!response.path("has_more").isBoolean()) throw new InvalidAnalysisOutputException();
            if (!response.path("has_more").asBoolean()) return result;
            cursor = id(response, "last_id"); if (!cursors.add(cursor)) throw new InvalidAnalysisOutputException();
        }
        throw new InvalidAnalysisOutputException();
    }

    public Observation inspect(AnalysisJob job) {
        if (CAPTURE.equals(job.stage)) {
            if (job.terminal()) return new Observation(job.sessionId, job.turnId, null, null, true);
            try {
                var state = state(job);
                if (state.nextSource < state.research.sources().size()) {
                    EvidencePipeline.captureNext(state, capture);
                    return new Observation(job.sessionId, job.turnId, null, null, true,
                            new Advance(CAPTURE, MAPPER.writeValueAsString(state), false));
                }
                if (state.findings.isEmpty()) return new Observation(job.sessionId, job.turnId, null, "EVIDENCE_CAPTURE_FAILED", true);
                return new Observation(job.sessionId, job.turnId, null, null, true, new Advance(SYNTHESIS, job.stagePayload, true));
            } catch (RuntimeException invalid) {
                return new Observation(job.sessionId, job.turnId, null, "INVALID_EVIDENCE_OUTPUT", true);
            }
        }
        long end = deadline(job); String session = job.sessionId, turn = job.turnId;
        if (session == null) {
            var matches = pages("agents/sessions?order=desc&limit=100", end).stream().filter(value ->
                    job.id.equals(value.path("metadata").path("crosscheck_job").asString())
                    && job.stage.equals(value.path("metadata").path("crosscheck_stage").asString())
                    && agentId.equals(value.path("agent").path("id").asString())).toList();
            if (matches.size() != 1) return Observation.pending(null, null);
            session = id(matches.getFirst(), "id");
        }
        if (turn == null) {
            var turns = pages("agents/sessions/" + session + "/turns?order=desc&limit=100", end);
            var items = pages("agents/sessions/" + session + "/items?order=desc&limit=100", end);
            var candidates = new ArrayList<String>();
            for (var candidate : turns) {
                String candidateId = id(candidate, "id");
                if (!session.equals(candidate.path("session_id").asString()) || !candidate.path("subagent_id").isNull()) continue;
                if (items.stream().anyMatch(item -> candidateId.equals(item.path("turn_id").asString())
                        && "user".equals(item.path("role").asString()) && item.path("content").size() == 1
                        && Objects.equals(job.remoteInputHash, hash(item.path("content").get(0).path("text").asString())))) candidates.add(candidateId);
            }
            if (candidates.size() != 1) return Observation.pending(session, null);
            turn = candidates.getFirst();
        }
        var remote = get("agents/sessions/" + session + "/turns/" + turn, end);
        if (!session.equals(remote.path("session_id").asString()) || !turn.equals(id(remote, "id")) || !remote.path("subagent_id").isNull())
            throw new InvalidAnalysisOutputException();
        String status = text(remote, "status");
        if (Set.of("failed", "cancelled").contains(status)) return new Observation(session, turn, null, "ANALYSIS_PROVIDER_FAILED", true);
        if (status.equals("waiting")) return new Observation(session, turn, null, "ANALYSIS_PROVIDER_ACTION_REQUIRED", false);
        if (!status.equals("completed")) return Observation.pending(session, turn);
        if (job.terminal()) return new Observation(session, turn, null, null, true);
        try {
            String output = reader.readOutput(session, turn, end);
            if (RESEARCH.equals(job.stage)) {
                var result = MAPPER.readValue(output, EvidencePipeline.ResearchResponse.class);
                EvidencePipeline.require("1".equals(result.schemaVersion()) && (result.research() == null) != (result.clarification() == null));
                if (result.clarification() != null) {
                    EvidencePipeline.require(result.clarification().reason() != com.crosscheck.application.model.Clarification.Reason.CONTEXT_UNAVAILABLE);
                    return new Observation(session, turn, new AiAnalysisTurn(logicalSession(job, session), null, result.clarification()), null, true, null, remote.path("usage").toString());
                }
                var state = EvidencePipeline.start(result.research(), job.input);
                state.researchUsage = remote.path("usage").toString();
                return new Observation(session, turn, null, null, true, new Advance(CAPTURE, MAPPER.writeValueAsString(state), false), remote.path("usage").toString());
            }
            var result = EvidencePipeline.assemble(state(job), MAPPER.readValue(output, EvidencePipeline.Synthesis.class));
            return new Observation(session, turn, new AiAnalysisTurn(logicalSession(job, session), result), null, true, null, remote.path("usage").toString());
        } catch (RuntimeException invalid) {
            return new Observation(session, turn, null, "INVALID_ANALYSIS_OUTPUT", true, null, remote.path("usage").toString());
        }
    }
    private static String logicalSession(AnalysisJob job, String remoteSession) {
        return job.conversationSessionId == null ? remoteSession : job.conversationSessionId;
    }
}
