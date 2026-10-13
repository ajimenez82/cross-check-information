package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.model.AiAnalysisTurn;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Replays captured provider text through the production v3 parser without network access. */
public final class ReplayPoliticalAnalysisService implements AiPoliticalAnalysisService {
    private final String scenario;
    private final String query;
    private final String output;
    private final String digest;

    public ReplayPoliticalAnalysisService(String scenario) {
        if (scenario == null || !scenario.matches("[A-Z][A-Z0-9_]{0,63}"))
            throw new IllegalArgumentException("Invalid replay scenario");
        this.scenario = scenario;
        try {
            var metadata = OpenAiJson.MAPPER.readTree(resource(scenario, "metadata.json"));
            query = OpenAiJson.text(metadata, "query");
            output = resource(scenario, "response.json");
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(output.getBytes(StandardCharsets.UTF_8)));
            if (!scenario.equals(OpenAiJson.text(metadata, "scenario"))
                    || !digest.equals(OpenAiJson.text(metadata, "responseSha256"))
                    || !metadata.path("previousContext").isNull())
                throw new IllegalArgumentException("Replay metadata does not match response");
        } catch (Exception invalid) {
            throw new IllegalArgumentException("Cannot load replay scenario: " + scenario);
        }
    }

    public String revision() { return "replay:" + scenario + ":" + digest.substring(0, 16); }
    public String query() { return query; }

    @Override
    public AiAnalysisTurn analyze(AiAnalysisInput input) {
        // Captures currently represent independent first turns, not invented follow-ups.
        if (!query.equals(input.text()) || input.context() != null)
            throw new InvalidAnalysisOutputException();
        String sessionId = input.sessionId() == null ? "replay_" + java.util.UUID.randomUUID() : input.sessionId();
        return OpenAiJson.claimTurn(output, input, sessionId);
    }

    private static String resource(String scenario, String name) throws Exception {
        try (var stream = ReplayPoliticalAnalysisService.class.getResourceAsStream(
                "/openai/replay/" + scenario + "/" + name)) {
            if (stream == null) throw new IllegalArgumentException("Missing replay resource");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
