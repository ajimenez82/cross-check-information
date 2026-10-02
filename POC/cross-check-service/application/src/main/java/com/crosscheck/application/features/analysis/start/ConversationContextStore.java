package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.model.ConversationContext;
import java.time.*;
import java.util.*;

/** Bounded, expiring POC storage. Each token points at an immutable context snapshot. */
public final class ConversationContextStore implements com.crosscheck.application.contracts.ConversationContexts {
    private record Entry(String sessionId, ConversationContext context, Instant expiresAt) {}
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    private final Clock clock;
    private final int capacity;
    public ConversationContextStore(Clock clock, int capacity) {
        this.clock = Objects.requireNonNull(clock);
        if (capacity < 1) throw new IllegalArgumentException("Invalid context capacity");
        this.capacity = capacity;
    }
    private void purge() { entries.values().removeIf(entry -> !entry.expiresAt().isAfter(clock.instant())); }
    public synchronized String put(String sessionId, ConversationContext context, Instant expiresAt) {
        purge();
        while (entries.size() >= capacity) entries.remove(entries.keySet().iterator().next());
        String id = UUID.randomUUID().toString();
        entries.put(id, new Entry(sessionId, context, expiresAt));
        return id;
    }
    public synchronized ConversationContext get(String id, String sessionId) {
        purge();
        var entry = entries.get(id);
        return entry != null && entry.sessionId().equals(sessionId) ? entry.context() : null;
    }
}
