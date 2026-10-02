package com.crosscheck.infrastructure.conversation;

import com.crosscheck.application.error.*;
import com.crosscheck.application.model.ConversationReference;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectEncrypter;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EncryptedConversationReferenceCodecTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private final String secret = newSecret();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EncryptedConversationReferenceCodec codec = codec(secret, clock);

    @Test
    void roundTripSurvivesNewInstanceWithSameKey() throws Exception {
        var reference = reference(NOW.plusSeconds(3600));
        String token = codec.encode(reference);
        assertEquals(reference, codec(secret, clock).decode(token));
        // Random ciphertext may coincidentally contain short strings such as "v1".
        // Check the public header instead; payload confidentiality is covered by wrong-key rejection.
        assertEquals(Set.of("alg", "enc", "typ"), JWEObject.parse(token).getHeader().toJSONObject().keySet());
        assertNotEquals(token, codec.encode(reference));
        assertEquals(5, token.split("\\.", -1).length);
    }

    @Test
    void wrongKeyInvalidatesReference() {
        String token = codec.encode(reference(NOW.plusSeconds(3600)));
        assertThrows(InvalidConversationReferenceException.class, () -> codec(newSecret(), clock).decode(token));
    }

    @Test void contextIdentifierIsAuthenticatedWithoutEmbeddingConversationText() {
        var reference = new ConversationReference("session-context", AnalysisCategory.POLITICAL_ANALYSIS,
                "political-v3-test", NOW.plusSeconds(60), UUID.randomUUID().toString());
        var token = codec.encode(reference);
        assertEquals(reference, codec.decode(token));
        assertFalse(token.contains(reference.contextId()));
        assertThrows(InvalidConversationReferenceException.class, () -> codec(newSecret(), clock).decode(token));
    }

    @Test
    void changingProtectedHeaderIvCiphertextOrTagIsRejected() {
        String token = codec.encode(reference(NOW.plusSeconds(3600)));
        for (int index : new int[]{0, 2, 3, 4}) {
            String[] parts = token.split("\\.", -1);
            byte[] bytes = Base64.getUrlDecoder().decode(parts[index]);
            bytes[0] ^= 1;
            parts[index] = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String altered = String.join(".", parts);
            assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(altered));
        }
    }

    @Test
    void expirationBoundaryAndFutureExpiryAreCheckedAfterAuthentication() {
        String token = codec.encode(reference(NOW.plusSeconds(1)));
        assertNotNull(codec.decode(token));
        assertThrows(ExpiredConversationReferenceException.class,
                () -> codec(secret, Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC)).decode(token));
        assertThrows(ExpiredConversationReferenceException.class,
                () -> codec(secret, Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC)).decode(token));
        assertThrows(InvalidConversationReferenceException.class,
                () -> codec(newSecret(), Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC)).decode(token));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "dev_old", "a.b.c", "a..b.c.d", "a..b.c.d.extra"})
    void malformedInputIsRejected(String token) {
        assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(token));
    }

    @Test
    void nullOversizedAndNonCanonicalInputAreRejected() {
        assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(null));
        assertThrows(InvalidConversationReferenceException.class, () -> codec.decode("x".repeat(4097)));
        String token = codec.encode(reference(NOW.plusSeconds(1)));
        assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(token + "="));
    }

    @Test
    void unsupportedHeaderPolicyIsRejectedEvenWithValidAuthentication() throws Exception {
        for (var header : List.of(
                new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256)
                        .type(new JOSEObjectType("crosscheck-conversation-v1")).build(),
                new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                        .type(new JOSEObjectType("other-purpose")).build(),
                new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                        .type(new JOSEObjectType("crosscheck-conversation-v1")).compressionAlgorithm(CompressionAlgorithm.DEF).build())) {
            var token = new JWEObject(header, new Payload(payload()));
            token.encrypt(new DirectEncrypter(Base64.getDecoder().decode(secret)));
            assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(token.serialize()));
        }
    }

    @Test
    void malformedAuthenticatedPayloadIsRejected() throws Exception {
        for (var field : payload().keySet()) {
            var values = new HashMap<>(payload());
            values.remove(field);
            assertInvalidPayload(values);
        }
        var unknown = new HashMap<>(payload());
        unknown.put("category", "GENERAL");
        assertInvalidPayload(unknown);
        unknown = new HashMap<>(payload());
        unknown.put("sessionId", 123);
        assertInvalidPayload(unknown);
        unknown = new HashMap<>(payload());
        unknown.put("expiresAt", "invalid-date");
        assertInvalidPayload(unknown);
        unknown = new HashMap<>(payload());
        unknown.put("extra", "unexpected");
        assertInvalidPayload(unknown);
    }

    @Test
    void missingInvalidOrWrongLengthSecretsFailWithoutLeakingValue() {
        for (String value : new String[]{null, "", "not-base64-secret!", Base64.getEncoder().encodeToString(new byte[16])}) {
            var error = assertThrows(IllegalArgumentException.class, () -> codec(value, clock));
            if (value != null && !value.isEmpty()) assertFalse(error.getMessage().contains(value));
            assertNull(error.getCause());
        }
    }

    @Test
    void encodedTokenMustFitConfiguredLimit() {
        var limited = new EncryptedConversationReferenceCodec(secret, clock, 512);
        var reference = new ConversationReference("x".repeat(1000), AnalysisCategory.POLITICAL_ANALYSIS,
                "v1", NOW.plusSeconds(60));
        assertThrows(IllegalStateException.class, () -> limited.encode(reference));
    }

    private void assertInvalidPayload(Map<String, Object> values) throws Exception {
        var token = new JWEObject(new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                .type(new JOSEObjectType("crosscheck-conversation-v1")).build(), new Payload(values));
        token.encrypt(new DirectEncrypter(Base64.getDecoder().decode(secret)));
        assertThrows(InvalidConversationReferenceException.class, () -> codec.decode(token.serialize()));
    }

    private Map<String, Object> payload() {
        return Map.of("sessionId", "session-private", "category", "POLITICAL_ANALYSIS",
                "agentRevision", "v1", "expiresAt", NOW.plusSeconds(60).toString());
    }

    private ConversationReference reference(Instant expiry) {
        return new ConversationReference("session-private", AnalysisCategory.POLITICAL_ANALYSIS, "v1", expiry);
    }

    private static String newSecret() {
        return Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
    }

    private EncryptedConversationReferenceCodec codec(String key, Clock time) {
        return new EncryptedConversationReferenceCodec(key, time, 4096);
    }
}
