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
            var result = MAPPER.readValue(json, AnalysisReport.class);
            if (result == null) throw new InvalidAnalysisOutputException();
            return result;
        } catch (RuntimeException invalid) {
            // Never retain parsing exceptions: they can contain the model output.
            throw new InvalidAnalysisOutputException();
        }
    }
}
