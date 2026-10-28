package com.crosscheck.infrastructure.openai;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One agent execution owns research, sources and the complete report. */
final class UnifiedAgentConfiguration {
    static Map<String, Object> value() {
        return value(com.crosscheck.domain.analysis.AnalysisCategory.POLITICAL_ANALYSIS);
    }
    static Map<String, Object> value(com.crosscheck.domain.analysis.AnalysisCategory category) {
        String instructions = switch (category) {
            case POLITICAL_ANALYSIS -> resource("instructions.md");
            case SPORTS_ANALYSIS -> resource("sports-instructions.md");
        };
        return Map.of("model", "gpt-5.4", "service_tier", "default", "reasoning", Map.of("effort", "medium"),
                "instructions", instructions, "tools", List.of(Map.of("type", "web_search", "mode", "live")),
                "multi_agent", Map.of("enabled", false),
                "text", Map.of("format", Map.of("type", "json_schema", "schema", OpenAiJson.MAPPER.readTree(resource("response.schema.json")))));
    }
    private static String resource(String name) {
        try (var input = UnifiedAgentConfiguration.class.getResourceAsStream("/openai/unified/" + name)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception failure) { throw new IllegalStateException("Missing unified agent configuration", failure); }
    }
}
