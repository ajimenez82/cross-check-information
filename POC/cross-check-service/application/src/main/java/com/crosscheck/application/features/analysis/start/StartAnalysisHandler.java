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

    public StartAnalysisHandler(AiPoliticalAnalysisService provider, ConversationReferenceCodec codec,
                                AnalysisPolicy policy, Clock clock) {
        this.provider = Objects.requireNonNull(provider);
        this.codec = Objects.requireNonNull(codec);
        this.policy = Objects.requireNonNull(policy);
        this.clock = Objects.requireNonNull(clock);
    }

    public StartAnalysisResult handle(StartAnalysisCommand command) {
        validateInput(command);
        var reference = resolve(command.conversationToken());
        var input = new AiAnalysisInput(command.text(), AnalysisCategory.POLITICAL_ANALYSIS,
                policy.agentRevision(), reference == null ? null : reference.sessionId());

        AiAnalysisTurn turn;
        try {
            turn = provider.analyze(input);
        } catch (InvalidAnalysisReportException invalidReport) {
            throw new InvalidAnalysisOutputException();
        }
        if (turn == null || turn.report() == null || turn.sessionId() == null || turn.sessionId().isBlank()
                || (reference != null && !reference.sessionId().equals(turn.sessionId()))) {
            throw new InvalidAnalysisOutputException();
        }

        var completedAt = clock.instant();
        var nextReference = new ConversationReference(turn.sessionId(), AnalysisCategory.POLITICAL_ANALYSIS,
                policy.agentRevision(), completedAt.plus(policy.conversationTtl()));
        var token = codec.encode(nextReference);
        if (token == null || token.isBlank() || token.length() > policy.maxTokenLength()) {
            throw new IllegalStateException("El codificador no produjo una referencia válida");
        }
        return new StartAnalysisResult(token, AnalysisResultMapper.map(turn.report(), completedAt));
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
