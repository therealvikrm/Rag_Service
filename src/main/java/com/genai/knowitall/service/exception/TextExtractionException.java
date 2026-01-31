package com.genai.knowitall.service.exception;

/**
 * Exception thrown when text extraction from a document fails.
 * This can occur with corrupted PDFs, password-protected files, or unsupported formats.
 */
public class TextExtractionException extends DocumentProcessingException {

    public TextExtractionException(String message) {
        super(message);
    }

    public TextExtractionException(String message, String documentId) {
        super(message, documentId);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TextExtractionException(String message, String documentId, Throwable cause) {
        super(message, documentId, cause);
    }
}
