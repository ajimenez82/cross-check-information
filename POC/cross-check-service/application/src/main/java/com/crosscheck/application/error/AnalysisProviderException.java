package com.crosscheck.application.error;

import java.util.Objects;

/** The adapter maps failures without exposing provider messages, responses, or secrets. */
public final class AnalysisProviderException extends RuntimeException {
    public enum Reason { UNAVAILABLE, TIMEOUT, SESSION_UNAVAILABLE, CONFLICT }
    public enum ExecutionState { NOT_STARTED, UNKNOWN }

    private final Reason reason;
    private final ExecutionState executionState;

    public AnalysisProviderException(Reason reason, ExecutionState executionState) {
        super("No se ha podido completar el análisis.");
        this.reason = Objects.requireNonNull(reason);
        this.executionState = Objects.requireNonNull(executionState);
    }

    public Reason reason() { return reason; }
    public ExecutionState executionState() { return executionState; }
}
