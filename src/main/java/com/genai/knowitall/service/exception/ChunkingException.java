package com.genai.knowitall.service.exception;

/**
 * Exception thrown when document chunking fails.
 * This can occur with invalid text encoding or chunking logic errors.
 */
public class ChunkingException extends DocumentProcessingException {

    public ChunkingException(String message) {
        super(message);
    }

    public ChunkingException(String message, String documentId) {
        super(message, documentId);
    }

    public ChunkingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChunkingException(String message, String documentId, Throwable cause) {
        super(message, documentId, cause);
    }
}
