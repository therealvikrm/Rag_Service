package com.genai.knowitall.controller.dto;

import com.genai.knowitall.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for document upload endpoint.
 * Contains the created document ID and initial status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {

    /**
     * Unique identifier of the uploaded document
     */
    private String documentId;

    /**
     * Original filename
     */
    private String filename;

    /**
     * Document title
     */
    private String title;

    /**
     * Current processing status (typically UPLOADING or PROCESSING)
     */
    private DocumentStatus status;

    /**
     * Upload timestamp
     */
    private LocalDateTime uploadedAt;

    /**
     * File size in bytes
     */
    private Long fileSizeBytes;

    /**
     * Message to display to user
     */
    private String message;
}
