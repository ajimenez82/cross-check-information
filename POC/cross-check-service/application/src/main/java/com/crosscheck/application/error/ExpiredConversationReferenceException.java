package com.crosscheck.application.error;

public final class ExpiredConversationReferenceException extends RuntimeException {
    public ExpiredConversationReferenceException() {
        super("La referencia de conversación ha caducado.");
    }
}
