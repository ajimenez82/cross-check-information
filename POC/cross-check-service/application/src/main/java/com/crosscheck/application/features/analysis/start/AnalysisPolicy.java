package com.crosscheck.application.features.analysis.start;

import java.time.Duration;
import java.util.Objects;

/** Server-supplied values; production limits are not fixed in this phase. */
public record AnalysisPolicy(int maxInputLength, int maxTokenLength,
                             String agentRevision, Duration conversationTtl) {
    public AnalysisPolicy {
        if (maxInputLength <= 0 || maxTokenLength <= 0) {
            throw new IllegalArgumentException("Los límites deben ser positivos");
        }
        if (agentRevision == null || agentRevision.isBlank()) {
            throw new IllegalArgumentException("La revisión es obligatoria");
        }
        Objects.requireNonNull(conversationTtl, "conversationTtl");
        if (conversationTtl.isNegative() || conversationTtl.isZero()) {
            throw new IllegalArgumentException("La duración debe ser positiva");
        }
    }
}
