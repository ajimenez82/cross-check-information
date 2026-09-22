package com.crosscheck.domain.analysis;

/** Signals a structural inconsistency, never a verdict about the content. */
public final class InvalidAnalysisReportException extends IllegalArgumentException {
    public InvalidAnalysisReportException(String message) {
        super(message);
    }
}
