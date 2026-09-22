package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.contracts.*;
import com.crosscheck.application.error.*;
import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URI;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StartAnalysisHandlerTest {
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private final FakeCodec codec = new FakeCodec();
    private final FakeProvider provider = new FakeProvider();
    private final AnalysisPolicy policy = new AnalysisPolicy(100, 200, "political-v1", Duration.ofHours(2));
    private final StartAnalysisHandler handler = handler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void newQueryCallsProviderOnceAndIssuesOpaqueToken() {
        var result = handler.handle(new StartAnalysisCommand("Consulta", null));
        assertEquals(1, provider.calls);
        assertEquals(0, codec.decodeCalls);
        assertEquals("Consulta", provider.input.text());
        assertNull(provider.input.sessionId());
        assertEquals(AnalysisCategory.POLITICAL_ANALYSIS, provider.input.category());
        assertEquals("political-v1", provider.input.agentRevision());
        assertEquals("opaque-output", result.conversationToken());
        assertEquals("session-1", codec.encoded.sessionId());
        assertEquals(NOW.plus(Duration.ofHours(2)), codec.encoded.expiresAt());
        assertEquals(NOW, result.analysis().analyzedAt());
        assertEquals("INSUFFICIENT_EVIDENCE", result.analysis().verdict().status());
        assertEquals("UNAVAILABLE", result.analysis().publicationPositions().availability());
        assertNull(result.analysis().asOf());
    }

    @Test
    void followUpReusesDecodedSessionAndRefreshesExpiration() {
        codec.decoded = reference(NOW.plusSeconds(60), "political-v1");
        handler.handle(new StartAnalysisCommand("Seguimiento", "opaque-input"));
        assertEquals("opaque-input", codec.receivedToken);
        assertEquals("session-1", provider.input.sessionId());
        assertEquals("session-1", codec.encoded.sessionId());
        assertEquals(NOW.plus(Duration.ofHours(2)), codec.encoded.expiresAt());
    }

    @Test
    void serverDateAndTtlAreAssignedAtCompletion() {
        Clock steppingClock = new Clock() {
            private int calls;
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return calls++ == 0 ? NOW : NOW.plusSeconds(30); }
        };
        codec.decoded = reference(NOW.plusSeconds(10), "political-v1");
        var result = handler(steppingClock).handle(new StartAnalysisCommand("Seguimiento", "token"));
        assertEquals(NOW.plusSeconds(30), result.analysis().analyzedAt());
        assertEquals(NOW.plusSeconds(30).plus(policy.conversationTtl()), codec.encoded.expiresAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsEmptyInputBeforeCallingDependencies(String text) {
        assertThrows(InvalidAnalysisInputException.class,
                () -> handler.handle(new StartAnalysisCommand(text, "token")));
        assertEquals(0, provider.calls);
        assertEquals(0, codec.decodeCalls);
    }

    @Test
    void rejectsMissingCommandAndOversizedInput() {
        assertThrows(InvalidAnalysisInputException.class, () -> handler.handle(null));
        assertThrows(InvalidAnalysisInputException.class,
                () -> handler.handle(new StartAnalysisCommand("x".repeat(101), null)));
        assertEquals(0, provider.calls);
    }

    @Test
    void inputLimitCountsUnicodeCodePointsAndPreservesText() {
        String text = "😀".repeat(100);
        handler.handle(new StartAnalysisCommand(text, null));
        assertEquals(text, provider.input.text());
        assertThrows(InvalidAnalysisInputException.class,
                () -> handler.handle(new StartAnalysisCommand(text + "x", null)));
        assertEquals(1, provider.calls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankTokenInsteadOfStartingNewConversation(String token) {
        assertThrows(InvalidConversationReferenceException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", token)));
        assertEquals(0, codec.decodeCalls);
        assertEquals(0, provider.calls);
    }

    @Test
    void rejectsOversizedTokenBeforeDecoding() {
        assertThrows(InvalidConversationReferenceException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", "x".repeat(201))));
        assertEquals(0, codec.decodeCalls);
        assertEquals(0, provider.calls);
    }

    @Test
    void rejectsManipulatedTokenWithoutCallingProvider() {
        codec.failure = new InvalidConversationReferenceException();
        assertThrows(InvalidConversationReferenceException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", "tampered")));
        assertEquals(0, provider.calls);
        assertEquals(0, codec.encodeCalls);
    }

    @Test
    void rejectsExpiredReferenceIncludingExactBoundary() {
        for (var expiry : List.of(NOW.minusSeconds(1), NOW)) {
            codec.decoded = reference(expiry, "political-v1");
            assertThrows(ExpiredConversationReferenceException.class,
                    () -> handler.handle(new StartAnalysisCommand("Consulta", "expired")));
        }
        assertEquals(0, provider.calls);
    }

    @Test
    void rejectsWrongAgentRevisionOrMissingDecodedReference() {
        codec.decoded = reference(NOW.plusSeconds(60), "old-revision");
        assertThrows(InvalidConversationReferenceException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", "old")));
        codec.decoded = null;
        assertThrows(InvalidConversationReferenceException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", "missing")));
        assertEquals(0, provider.calls);
    }

    @Test
    void providerFailuresPropagateWithoutRetriesOrTokenIssuance() {
        for (var reason : AnalysisProviderException.Reason.values()) {
            provider.calls = 0;
            var failure = new AnalysisProviderException(reason, AnalysisProviderException.ExecutionState.UNKNOWN);
            provider.failure = failure;
            var thrown = assertThrows(AnalysisProviderException.class,
                    () -> handler.handle(new StartAnalysisCommand("Consulta", null)));
            assertSame(failure, thrown);
            assertEquals(AnalysisProviderException.ExecutionState.UNKNOWN, thrown.executionState());
            assertEquals(1, provider.calls);
            assertEquals(0, codec.encodeCalls);
        }
    }

    @Test
    void invalidReportIsTechnicalErrorAndDoesNotIssueToken() {
        provider.failure = new InvalidAnalysisReportException("Detalle interno del proveedor");
        var thrown = assertThrows(InvalidAnalysisOutputException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", null)));
        assertFalse(thrown.getMessage().contains("Detalle interno"));
        assertEquals(0, codec.encodeCalls);
    }

    @Test
    void rejectsMissingProviderResultReportAndSession() {
        for (var turn : new AiAnalysisTurn[]{null, new AiAnalysisTurn("session-1", null),
                new AiAnalysisTurn(null, report()), new AiAnalysisTurn(" ", report())}) {
            provider.turn = turn;
            assertThrows(InvalidAnalysisOutputException.class,
                    () -> handler.handle(new StartAnalysisCommand("Consulta", null)));
        }
        assertEquals(0, codec.encodeCalls);
    }

    @Test
    void rejectsProviderSwitchingSessionDuringFollowUp() {
        codec.decoded = reference(NOW.plusSeconds(60), "political-v1");
        provider.turn = new AiAnalysisTurn("different-session", report());
        assertThrows(InvalidAnalysisOutputException.class,
                () -> handler.handle(new StartAnalysisCommand("Seguimiento", "token")));
        assertEquals(0, codec.encodeCalls);
    }

    @Test
    void codecFailureDoesNotTriggerAnotherProviderExecution() {
        codec.encodeFailure = new IllegalStateException("No se puede emitir el token");
        assertThrows(IllegalStateException.class,
                () -> handler.handle(new StartAnalysisCommand("Consulta", null)));
        assertEquals(1, provider.calls);
    }

    @Test
    void rejectsUnusableEncodedToken() {
        for (var token : new String[]{null, "", " ", "x".repeat(201)}) {
            codec.output = token;
            assertThrows(IllegalStateException.class,
                    () -> handler.handle(new StartAnalysisCommand("Consulta", null)));
        }
    }

    @Test
    void mapsSourcesClassificationReferencesAndOptionalDates() {
        var source = new Evidence("s1", "Fuente", URI.create("https://example.org/s1"), "Publicación",
                LocalDate.of(2026, 9, 15), NOW.minusSeconds(30), "Contexto", "NEWS");
        var positions = new PublicationPositions(ClassificationAvailability.AVAILABLE, null, "Tesis",
                new PublicationPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17)),
                NOW.minusSeconds(10), "Criterio",
                List.of(new PublicationAssessment("u1", List.of("s1"),
                        PublicationPosition.NO_EXPLICIT_POSITION, "Solo informa")), List.of());
        provider.turn = new AiAnalysisTurn("session-1", new AnalysisReport("Título", "Contexto", "Síntesis",
                List.of("s1"), new Verdict(VerdictStatus.NO_SINGLE_VERDICT, "Matices", "Limitado", List.of("s1")),
                List.of(source), positions, List.of("Muestra limitada"), LocalDate.of(2026, 9, 16)));
        var result = handler.handle(new StartAnalysisCommand("Consulta", null)).analysis();
        assertEquals("https://example.org/s1", result.sources().getFirst().url());
        assertEquals(source.publishedAt(), result.sources().getFirst().publishedAt());
        assertEquals(source.consultedAt(), result.sources().getFirst().consultedAt());
        assertEquals(List.of("s1"), result.summarySourceIds());
        assertEquals(List.of("s1"), result.verdict().sourceIds());
        assertEquals("NO_EXPLICIT_POSITION", result.publicationPositions().units().getFirst().position());
        assertEquals(positions.period().from(), result.publicationPositions().period().from());
        assertEquals(positions.consultedAt(), result.publicationPositions().consultedAt());
        assertEquals(List.of("Muestra limitada"), result.limitations());
        assertEquals(LocalDate.of(2026, 9, 16), result.asOf());
        assertThrows(UnsupportedOperationException.class, () -> result.sources().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.publicationPositions().units().getFirst().sourceIds().clear());
    }

    @Test
    void sensitiveWrappersDoNotExposeTokensOrQueriesViaToString() {
        assertFalse(new StartAnalysisCommand("secret-query", "secret-token").toString().contains("secret"));
        assertFalse(reference(NOW, "political-v1").toString().contains("session-1"));
        assertFalse(handler.handle(new StartAnalysisCommand("Consulta", null)).toString().contains("opaque-output"));
    }

    @Test
    void policyRejectsInvalidLimitsRevisionOrTtl() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class,
                () -> new AnalysisPolicy(0, 100, "v1", Duration.ofHours(1))),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new AnalysisPolicy(100, -1, "v1", Duration.ofHours(1))),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new AnalysisPolicy(100, 100, "", Duration.ofHours(1))),
            () -> assertThrows(IllegalArgumentException.class,
                () -> new AnalysisPolicy(100, 100, "v1", Duration.ZERO))
        );
    }

    private StartAnalysisHandler handler(Clock clock) {
        return new StartAnalysisHandler(provider, codec, policy, clock);
    }

    private static ConversationReference reference(Instant expiry, String revision) {
        return new ConversationReference("session-1", AnalysisCategory.POLITICAL_ANALYSIS, revision, expiry);
    }

    private static AnalysisReport report() {
        return new AnalysisReport("Título", "Contexto", "Síntesis", List.of(),
                new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE, "Sin pruebas", "Insuficiente", List.of()),
                List.of(), new PublicationPositions(ClassificationAvailability.UNAVAILABLE, "No realizada",
                    null, null, null, null, List.of(), List.of()), List.of("Sin fuentes accesibles"), null);
    }

    private static final class FakeProvider implements AiPoliticalAnalysisService {
        int calls;
        AiAnalysisInput input;
        AiAnalysisTurn turn = new AiAnalysisTurn("session-1", report());
        RuntimeException failure;
        public AiAnalysisTurn analyze(AiAnalysisInput input) {
            calls++;
            this.input = input;
            if (failure != null) throw failure;
            return turn;
        }
    }

    private static final class FakeCodec implements ConversationReferenceCodec {
        int decodeCalls;
        int encodeCalls;
        String receivedToken;
        String output = "opaque-output";
        ConversationReference decoded;
        ConversationReference encoded;
        RuntimeException failure;
        RuntimeException encodeFailure;
        public ConversationReference decode(String token) {
            decodeCalls++;
            receivedToken = token;
            if (failure != null) throw failure;
            return decoded;
        }
        public String encode(ConversationReference reference) {
            encodeCalls++;
            encoded = reference;
            if (encodeFailure != null) throw encodeFailure;
            return output;
        }
    }
}
