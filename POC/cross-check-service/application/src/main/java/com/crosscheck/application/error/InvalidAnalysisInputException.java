package com.crosscheck.application.error;

public final class InvalidAnalysisInputException extends RuntimeException {
    public InvalidAnalysisInputException() {
        super("La consulta no es válida.");
    }
}
