package com.crosscheck.application.model;

import com.crosscheck.domain.analysis.AnalysisCategory;
import java.time.Instant;
import java.util.Objects;

public record ConversationReference(String sessionId, AnalysisCategory category,
                                    String agentRevision, Instant expiresAt) {
    public ConversationReference {
        if (sessionId == null || sessionId.isBlank() || agentRevision == null || agentRevision.isBlank()) {
            throw new IllegalArgumentException("La referencia requiere sesión y revisión");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @Override
    public String toString() {
        return "ConversationReference[redacted]";
    }
}
