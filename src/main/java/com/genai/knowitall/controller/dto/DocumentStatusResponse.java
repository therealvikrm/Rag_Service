package com.genai.knowitall.controller.dto;

import com.genai.knowitall.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for document status endpoint.
 * Contains detailed processing status and progress information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatusResponse {

    /**
     * Document identifier
     */
    private String documentId;

    /**
     * Document title
     */
    private String title;

    /**
     * Original filename
     */
    private String filename;

    /**
     * Current processing status
     */
    private DocumentStatus status;

    /**
     * Total number of chunks (0 if not yet chunked)
     */
    private Integer totalChunks;

    /**
     * Number of chunks successfully processed with embeddings
     */
    private Integer processedChunks;

    /**
     * Processing progress percentage (0-100)
     */
    private Double progressPercentage;

    /**
     * Error message if status is FAILED
     */
    private String errorMessage;

    /**
     * Upload timestamp
     */
    private LocalDateTime uploadedAt;

    /**
     * Processing completion timestamp (if READY or FAILED)
     */
    private LocalDateTime processedAt;

    /**
     * User-friendly status message
     */
    private String message;
}
