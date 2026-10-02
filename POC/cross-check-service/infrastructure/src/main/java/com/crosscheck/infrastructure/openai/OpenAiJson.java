package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.domain.analysis.AnalysisReport;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

final class OpenAiJson {
    static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .withCoercionConfig(LogicalType.Enum, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private OpenAiJson() {}

    static String text(JsonNode node, String field) {
        var value = node.path(field);
        if (!value.isString() || value.asString().isBlank()) throw new InvalidAnalysisOutputException();
        return value.asString();
    }

    static String id(JsonNode node, String field) {
        String value = text(node, field);
        if (!value.matches("[A-Za-z0-9_-]{1,200}")) throw new InvalidAnalysisOutputException();
        return value;
    }

    static JsonNode data(JsonNode node) {
        var data = node.path("data");
        if (!data.isArray()) throw new InvalidAnalysisOutputException();
        return data;
    }

    static AnalysisReport report(String json) {
        try {
            if (json == null || json.length() > 250_000) throw new InvalidAnalysisOutputException();
            var tree = MAPPER.readTree(json);
            // Legacy provider reports predate trace. Do not accept asserted trace through that contract.
            for (String field : java.util.List.of("units", "excluded")) {
                for (var item : tree.path("publicationPositions").path(field)) {
                    if (item instanceof tools.jackson.databind.node.ObjectNode object) {
                        if (object.has("trace") && !object.path("trace").isNull()) throw new InvalidAnalysisOutputException();
                        object.putNull("trace");
                    }
                }
            }
            var result = MAPPER.treeToValue(tree, OpenAiReport.class);
            if (result == null) throw new InvalidAnalysisOutputException();
            return result.toReport();
        } catch (RuntimeException invalid) {
            // Never retain parsing exceptions: they can contain the model output.
            throw new InvalidAnalysisOutputException();
        }
    }

    // Kept separate from the active parser until the remote contract is migrated explicitly.
    static AnalysisReport tracedReport(String json) {
        try {
            if (json == null || json.length() > 250_000) throw new InvalidAnalysisOutputException();
            var result = MAPPER.readValue(json, OpenAiTracedReport.class);
            if (result == null) throw new InvalidAnalysisOutputException();
            return result.toReport();
        } catch (RuntimeException invalid) {
            throw new InvalidAnalysisOutputException();
        }
    }

    static AnalysisReport claimReport(String json, String input) {
        try {
            if (json == null || json.length() > 250_000) throw new InvalidAnalysisOutputException();
            var result = MAPPER.readValue(json, OpenAiClaimReport.class);
            if (result == null) throw new InvalidAnalysisOutputException();
            return result.toReport(input);
        } catch (RuntimeException invalid) {
            throw new InvalidAnalysisOutputException();
        }
    }

    private record ClaimResponse(String schemaVersion, OpenAiClaimReport analysis,
            com.crosscheck.application.model.Clarification clarification) {}

    static com.crosscheck.application.model.AiAnalysisTurn claimTurn(String json,
            com.crosscheck.application.model.AiAnalysisInput input, String sessionId) {
        try {
            if (json == null || json.length() > 250_000) throw new InvalidAnalysisOutputException();
            var result = MAPPER.readValue(json, ClaimResponse.class);
            if (result == null || !"3".equals(result.schemaVersion())
                    || (result.analysis() == null) == (result.clarification() == null)
                    || result.clarification() != null && result.clarification().reason()
                    == com.crosscheck.application.model.Clarification.Reason.CONTEXT_UNAVAILABLE)
                throw new InvalidAnalysisOutputException();
            return new com.crosscheck.application.model.AiAnalysisTurn(sessionId,
                    result.analysis() == null ? null : result.analysis().toReport(input.anchorTexts()), result.clarification());
        } catch (RuntimeException invalid) { throw new InvalidAnalysisOutputException(); }
    }
}
