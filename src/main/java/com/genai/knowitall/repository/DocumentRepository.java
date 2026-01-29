package com.genai.knowitall.repository;

import com.genai.knowitall.model.Document;
import com.genai.knowitall.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Document entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    /**
     * Find all documents owned by a specific user
     */
    List<Document> findByOwner(String owner);

    /**
     * Find documents by processing status
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * Find all documents owned by a user with a specific status
     */
    List<Document> findByOwnerAndStatus(String owner, DocumentStatus status);

    /**
     * Find a document by exact filename
     */
    Optional<Document> findByFilename(String filename);

    /**
     * Find all READY documents for a specific owner (queryable documents)
     */
    List<Document> findByOwnerAndStatusOrderByUploadedAtDesc(String owner, DocumentStatus status);

    /**
     * Count documents by status (useful for monitoring)
     */
    Long countByStatus(DocumentStatus status);
}
