package com.genai.knowitall.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * JPA Entity representing a chunk of a document.
 * When a document is split into chunks for embedding and vector storage,
 * each chunk is represented by this entity.
 */
@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_document_id", columnList = "document_id"),
        @Index(name = "idx_vector_id", columnList = "vector_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    /**
     * Unique identifier for this chunk (UUID)
     */
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /**
     * Foreign key to parent document
     */
    @Column(name = "document_id", nullable = false)
    private String documentId;

    /**
     * The actual text content of this chunk
     */
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    /**
     * Sequential index of this chunk within the document (0-based)
     * Helps reconstruct document order
     */
    @Column(nullable = false)
    private Integer chunkIndex;

    /**
     * ID of the embedding vector stored in Qdrant
     * Used to track and delete embeddings when chunks are deleted
     */
    @Column(name = "vector_id")
    private String vectorId;

    /**
     * Name of the embedding model used to generate the vector
     * Example: "text-embedding-3-small", "text-embedding-3-large"
     * Useful for tracking and migration when switching models
     */
    @Column(name = "embedding_model")
    private String embeddingModel;

    /**
     * JSON metadata for this chunk
     * Can contain: page_number, section_title, original_position, etc.
     * Example: {"page": 1, "section": "Introduction", "char_offset": 1024}
     */
    @Column(columnDefinition = "JSON")
    private String metadata;

    /**
     * Number of tokens in this chunk (useful for cost/limit tracking)
     */
    private Integer tokenCount;

    /**
     * Many-to-One relationship with Document
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Document document;

    /**
     * Generate a new UUID for chunk if not set
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
