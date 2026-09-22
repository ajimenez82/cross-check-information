package com.crosscheck.application.error;

public final class InvalidAnalysisOutputException extends RuntimeException {
    public InvalidAnalysisOutputException() {
        super("El proveedor devolvió un resultado no utilizable.");
    }
}
