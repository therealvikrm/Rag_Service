package com.genai.knowitall.service.exception;

/**
 * Exception thrown when embedding generation fails.
 * This can occur with OpenAI API errors, rate limits, or network issues.
 */
public class EmbeddingException extends DocumentProcessingException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, String documentId) {
        super(message, documentId);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(String message, String documentId, Throwable cause) {
        super(message, documentId, cause);
    }
}
