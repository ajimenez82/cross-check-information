package com.crosscheck.application.model;

import com.crosscheck.domain.analysis.AnalysisCategory;
import java.time.Instant;
import java.util.Objects;

public record ConversationReference(String sessionId, AnalysisCategory category,
                                    String agentRevision, Instant expiresAt, String contextId) {
    public ConversationReference(String sessionId, AnalysisCategory category, String agentRevision, Instant expiresAt) {
        this(sessionId, category, agentRevision, expiresAt, null);
    }
    public ConversationReference {
        if (sessionId == null || sessionId.isBlank() || agentRevision == null || agentRevision.isBlank()) {
            throw new IllegalArgumentException("La referencia requiere sesión y revisión");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (contextId != null && !contextId.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid context id");
    }

    @Override
    public String toString() {
        return "ConversationReference[redacted]";
    }
}
