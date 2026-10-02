package com.crosscheck.application.contracts;
import com.crosscheck.application.model.ConversationContext;
import java.time.Instant;
public interface ConversationContexts {
    String put(String sessionId, ConversationContext context, Instant expiresAt);
    ConversationContext get(String id, String sessionId);
}
