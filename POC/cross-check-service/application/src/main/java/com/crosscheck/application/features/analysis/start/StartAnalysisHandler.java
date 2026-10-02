package com.crosscheck.application.features.analysis.start;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.contracts.ConversationReferenceCodec;
import com.crosscheck.application.error.*;
import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.AnalysisCategory;
import com.crosscheck.domain.analysis.InvalidAnalysisReportException;
import java.time.Clock;
import java.util.Objects;

/** Coordinates a single attempt and maps the validated report using the final verdict policy. */
public final class StartAnalysisHandler {
    private final AiPoliticalAnalysisService provider;
    private final ConversationReferenceCodec codec;
    private final AnalysisPolicy policy;
    private final Clock clock;
    private final com.crosscheck.application.contracts.ConversationContexts contexts;

    public StartAnalysisHandler(AiPoliticalAnalysisService provider, ConversationReferenceCodec codec,
                                AnalysisPolicy policy, Clock clock) {
        this(provider, codec, policy, clock, new ConversationContextStore(clock, 1000));
    }

    public StartAnalysisHandler(AiPoliticalAnalysisService provider, ConversationReferenceCodec codec,
            AnalysisPolicy policy, Clock clock, com.crosscheck.application.contracts.ConversationContexts contexts) {
        this.provider = Objects.requireNonNull(provider);
        this.codec = Objects.requireNonNull(codec);
        this.policy = Objects.requireNonNull(policy);
        this.clock = Objects.requireNonNull(clock);
        this.contexts = Objects.requireNonNull(contexts);
    }

    public record Prepared(AiAnalysisInput input, StartAnalysisResult localResult) {}

    public Prepared prepare(StartAnalysisCommand command) {
        validateInput(command);
        var reference = resolve(command.conversationToken());
        var context = reference == null || reference.contextId() == null ? null
                : contexts.get(reference.contextId(), reference.sessionId());
        if (reference != null && reference.contextId() != null && context == null) {
            return new Prepared(null, new StartAnalysisResult(null, null, new Clarification(
                    "El contexto anterior ya no está disponible. Escribe la consulta completa para iniciar un nuevo análisis.",
                    Clarification.Reason.CONTEXT_UNAVAILABLE)));
        }
        var input = new AiAnalysisInput(command.text(), AnalysisCategory.POLITICAL_ANALYSIS,
                policy.agentRevision(), reference == null ? null : reference.sessionId(), context);

        return new Prepared(input, null);
    }

    public StartAnalysisResult handle(StartAnalysisCommand command) {
        var prepared = prepare(command);
        if (prepared.localResult() != null) return prepared.localResult();
        AiAnalysisTurn turn;
        try { turn = provider.analyze(prepared.input()); }
        catch (InvalidAnalysisReportException invalid) { throw new InvalidAnalysisOutputException(); }
        return complete(prepared.input(), turn);
    }

    /** Called in the same transaction as the durable job completion. */
    public StartAnalysisResult complete(AiAnalysisInput input, AiAnalysisTurn turn) {
        var context = input.context();
        if (turn == null || (turn.report() == null) == (turn.clarification() == null)
                || turn.sessionId() == null || turn.sessionId().isBlank()
                || (input.sessionId() != null && !input.sessionId().equals(turn.sessionId()))) {
            throw new InvalidAnalysisOutputException();
        }

        var completedAt = clock.instant();
        var mappedReport = turn.report() == null ? null : AnalysisResultMapper.map(turn.report(), completedAt);
        String contextId = null;
        try {
            if (turn.clarification() != null) {
                if (turn.clarification().reason() == Clarification.Reason.CONTEXT_UNAVAILABLE)
                    return new StartAnalysisResult(null, null, turn.clarification());
                var nextContext = new ConversationContext(context == null ? input.text()
                        : context.previousInput() + "\n" + input.text(),
                        context == null ? null : context.analysisTarget(),
                        context == null ? java.util.List.of() : context.propositions(), turn.clarification().question());
                contextId = contexts.put(turn.sessionId(), nextContext, completedAt.plus(policy.conversationTtl()));
            } else if (turn.report().claimAnalysis() != null) {
                var claims = turn.report().claimAnalysis();
                contextId = contexts.put(turn.sessionId(), new ConversationContext(input.text(), claims.analysisTarget(),
                        claims.claims().stream().map(claim -> claim.proposition()).toList(), null),
                        completedAt.plus(policy.conversationTtl()));
            }
        } catch (IllegalArgumentException invalid) { throw new InvalidAnalysisOutputException(); }
        var nextReference = new ConversationReference(turn.sessionId(), AnalysisCategory.POLITICAL_ANALYSIS,
                input.agentRevision(), completedAt.plus(policy.conversationTtl()), contextId);
        var token = codec.encode(nextReference);
        if (token == null || token.isBlank() || token.length() > policy.maxTokenLength()) {
            throw new IllegalStateException("El codificador no produjo una referencia válida");
        }
        return new StartAnalysisResult(token, mappedReport,
                turn.clarification());
    }

    private void validateInput(StartAnalysisCommand command) {
        if (command == null || command.text() == null || command.text().isBlank()
                || command.text().codePointCount(0, command.text().length()) > policy.maxInputLength()) {
            throw new InvalidAnalysisInputException();
        }
    }

    private ConversationReference resolve(String token) {
        if (token == null) {
            return null;
        }
        if (token.isBlank() || token.length() > policy.maxTokenLength()) {
            throw new InvalidConversationReferenceException();
        }
        var reference = codec.decode(token);
        if (reference == null || reference.category() != AnalysisCategory.POLITICAL_ANALYSIS
                || !reference.agentRevision().equals(policy.agentRevision())) {
            throw new InvalidConversationReferenceException();
        }
        if (!reference.expiresAt().isAfter(clock.instant())) {
            throw new ExpiredConversationReferenceException();
        }
        return reference;
    }
}
