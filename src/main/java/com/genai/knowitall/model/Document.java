package com.genai.knowitall.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity representing a document that has been uploaded and processed.
 * Stores metadata about the document and relationships to its chunks.
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    /**
     * Unique identifier for the document (UUID)
     */
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /**
     * User-friendly title of the document
     */
    @Column(nullable = false)
    private String title;

    /**
     * Original filename as uploaded
     */
    @Column(nullable = false)
    private String filename;

    /**
     * Brief description of the document content
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Timestamp when document was uploaded
     */
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    /**
     * Owner/uploader of the document (email or user ID)
     */
    @Column(nullable = false)
    private String owner;

    /**
     * Current status of document processing
     * Values: UPLOADING, PROCESSING, READY, FAILED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    /**
     * Total number of chunks this document was split into
     */
    @Column(nullable = false)
    private Integer totalChunks;

    /**
     * File size in bytes
     */
    private Long fileSizeBytes;

    /**
     * Error message if processing failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when document processing completed
     */
    private LocalDateTime processedAt;

    /**
     * One-to-Many relationship with DocumentChunks
     * When a document is deleted, all its chunks are automatically deleted (cascade)
     */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DocumentChunk> chunks;

    /**
     * Generate a new UUID for document if not set
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = DocumentStatus.UPLOADING;
        }
        if (this.totalChunks == null) {
            this.totalChunks = 0;
        }
    }
}
