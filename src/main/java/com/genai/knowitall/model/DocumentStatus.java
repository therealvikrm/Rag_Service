package com.genai.knowitall.model;

/**
 * Enum representing the status of document processing
 */
public enum DocumentStatus {
    /**
     * Document is being uploaded
     */
    UPLOADING,

    /**
     * Document is being processed (text extraction, chunking, embedding)
     */
    PROCESSING,

    /**
     * Document has been successfully processed and is ready for queries
     */
    READY,

    /**
     * Document processing failed
     */
    FAILED
}
