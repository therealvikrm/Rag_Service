package com.genai.knowitall.repository;

import com.genai.knowitall.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DocumentChunk entity
 * Provides CRUD operations and custom queries for document chunks
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    /**
     * Find all chunks belonging to a specific document
     * Ordered by chunk index to maintain document order
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(String documentId);

    /**
     * Find a chunk by its vector ID in Qdrant
     * Useful for mapping back from vector store to database
     */
    Optional<DocumentChunk> findByVectorId(String vectorId);

    /**
     * Count chunks that have been successfully embedded (have vectorId set)
     * Used to track processing progress
     */
    long countByDocumentIdAndVectorIdIsNotNull(String documentId);

    /**
     * Find all chunks for a document with a specific vector ID status
     * (useful for tracking which chunks have been embedded)
     */
    List<DocumentChunk> findByDocumentIdAndVectorIdNotNull(String documentId);

    /**
     * Find chunks for a document that haven't been embedded yet
     */
    List<DocumentChunk> findByDocumentIdAndVectorIdIsNull(String documentId);

    /**
     * Delete all chunks for a document
     * (though CASCADE should handle this, this is explicit)
     */
    Long deleteByDocumentId(String documentId);

    /**
     * Count total chunks for a document
     */
    Long countByDocumentId(String documentId);

    /**
     * Custom query: Find chunks that have been successfully embedded
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.documentId = :documentId AND dc.vectorId IS NOT NULL ORDER BY dc.chunkIndex ASC")
    List<DocumentChunk> findEmbeddedChunksForDocument(@Param("documentId") String documentId);

    /**
     * Find a specific chunk by document and chunk index
     */
    Optional<DocumentChunk> findByDocumentIdAndChunkIndex(String documentId, Integer chunkIndex);
}
