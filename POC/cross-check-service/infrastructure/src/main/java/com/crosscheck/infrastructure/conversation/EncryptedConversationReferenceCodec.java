package com.crosscheck.infrastructure.conversation;

import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.error.InvalidConversationReferenceException;
import com.crosscheck.application.error.ExpiredConversationReferenceException;
import com.crosscheck.application.model.ConversationReference;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Stateless authenticated references using compact JWE with a fixed algorithm policy. */
public final class EncryptedConversationReferenceCodec implements ConversationReferenceCodec {
    private static final JOSEObjectType TYPE = new JOSEObjectType("crosscheck-conversation-v1");
    private final byte[] key;
    private final Clock clock;
    private final int maxTokenLength;

    public EncryptedConversationReferenceCodec(String encodedKey, Clock clock, int maxTokenLength) {
        try {
            key = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Conversation token secret must be Base64 encoding 32 random bytes");
        }
        if (key.length != 32) {
            throw new IllegalArgumentException("Conversation token secret must be Base64 encoding 32 random bytes");
        }
        this.clock = Objects.requireNonNull(clock);
        if (maxTokenLength < 512) {
            throw new IllegalArgumentException("Maximum token length must be at least 512");
        }
        this.maxTokenLength = maxTokenLength;
    }

    @Override
    public String encode(ConversationReference reference) {
        Objects.requireNonNull(reference);
        try {
            var header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM).type(TYPE).build();
            var payload = new Payload(Map.of("sessionId", reference.sessionId(),
                    "category", reference.category().name(), "agentRevision", reference.agentRevision(),
                    "expiresAt", reference.expiresAt().toString()));
            var token = new JWEObject(header, payload);
            token.encrypt(new DirectEncrypter(key));
            String serialized = token.serialize();
            if (serialized.length() > maxTokenLength) {
                throw new IllegalStateException("Encoded conversation reference exceeds the configured limit");
            }
            return serialized;
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to encrypt conversation reference");
        }
    }

    @Override
    public ConversationReference decode(String serialized) {
        ConversationReference reference;
        try {
            if (serialized == null || serialized.isBlank() || serialized.length() > maxTokenLength) {
                throw new IllegalArgumentException();
            }
            String[] parts = serialized.split("\\.", -1);
            if (parts.length != 5 || !parts[1].isEmpty()) throw new IllegalArgumentException();
            for (String part : parts) {
                if (!Base64.getUrlEncoder().withoutPadding().encodeToString(
                        Base64.getUrlDecoder().decode(part)).equals(part)) throw new IllegalArgumentException();
            }
            var token = JWEObject.parse(serialized);
            var header = token.getHeader();
            if (!JWEAlgorithm.DIR.equals(header.getAlgorithm())
                    || !EncryptionMethod.A256GCM.equals(header.getEncryptionMethod())
                    || !TYPE.equals(header.getType())
                    || !header.toJSONObject().keySet().equals(Set.of("alg", "enc", "typ"))) {
                throw new IllegalArgumentException();
            }
            token.decrypt(new DirectDecrypter(key));
            var payload = token.getPayload().toJSONObject();
            if (payload == null || !payload.keySet().equals(
                    Set.of("sessionId", "category", "agentRevision", "expiresAt"))) {
                throw new IllegalArgumentException();
            }
            reference = new ConversationReference((String) payload.get("sessionId"),
                    AnalysisCategory.valueOf((String) payload.get("category")),
                    (String) payload.get("agentRevision"), Instant.parse((String) payload.get("expiresAt")));
        } catch (Exception exception) {
            throw new InvalidConversationReferenceException();
        }
        if (!reference.expiresAt().isAfter(clock.instant())) {
            throw new ExpiredConversationReferenceException();
        }
        return reference;
    }
}
