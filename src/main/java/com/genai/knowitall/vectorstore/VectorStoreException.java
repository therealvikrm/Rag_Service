package com.genai.knowitall.vectorstore;

/**
 * Custom exception for vector store operations.
 * Wraps underlying Qdrant client exceptions for cleaner error handling.
 */
public class VectorStoreException extends RuntimeException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public VectorStoreException(Throwable cause) {
        super(cause);
    }
}
