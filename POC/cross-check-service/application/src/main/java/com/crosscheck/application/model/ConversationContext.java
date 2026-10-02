package com.crosscheck.application.model;

import java.util.List;

/** Server-held context is integrity protected, not independently verified evidence. */
public record ConversationContext(String previousInput, String analysisTarget, List<String> propositions, String pendingQuestion) {
    public ConversationContext {
        propositions = List.copyOf(propositions);
        if (previousInput == null || previousInput.isBlank() || propositions.size() > 8)
            throw new IllegalArgumentException("Invalid conversation context");
        int size = previousInput.length() + (analysisTarget == null ? 0 : analysisTarget.length())
                + (pendingQuestion == null ? 0 : pendingQuestion.length())
                + propositions.stream().mapToInt(String::length).sum();
        if (size > 32000) throw new IllegalArgumentException("Conversation context exceeds limit");
    }
    @Override public String toString() { return "ConversationContext[redacted]"; }
}
