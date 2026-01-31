package com.genai.knowitall.service.exception;

/**
 * Base exception for document processing errors.
 * All specific processing exceptions extend this for unified error handling.
 */
public class DocumentProcessingException extends RuntimeException {

    private final String documentId;

    public DocumentProcessingException(String message) {
        super(message);
        this.documentId = null;
    }

    public DocumentProcessingException(String message, String documentId) {
        super(message);
        this.documentId = documentId;
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.documentId = null;
    }

    public DocumentProcessingException(String message, String documentId, Throwable cause) {
        super(message, cause);
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }
}
