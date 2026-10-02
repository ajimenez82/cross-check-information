package com.crosscheck.application.model;

public record Clarification(String question, Reason reason) {
    public enum Reason { MISSING_PERIOD, MISSING_SCOPE, AMBIGUOUS_REFERENCE, OTHER, CONTEXT_UNAVAILABLE }
    public Clarification {
        if (question == null || question.isBlank() || question.length() > 2000 || reason == null)
            throw new IllegalArgumentException("Invalid clarification");
    }
}
