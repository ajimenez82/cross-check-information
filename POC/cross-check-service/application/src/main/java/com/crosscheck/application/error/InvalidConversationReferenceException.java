package com.crosscheck.application.error;

public final class InvalidConversationReferenceException extends RuntimeException {
    public InvalidConversationReferenceException() {
        super("La referencia de conversación no es válida.");
    }
}
