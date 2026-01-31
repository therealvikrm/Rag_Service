package com.genai.knowitall.vectorstore;

import java.util.List;
import java.util.Map;

/**
 * Abstraction layer for vector database operations.
 * Defines the contract for storing, retrieving, and managing document embeddings.
 *
 * Implementation: Qdrant (via LangChain4j)
 * Purpose: Language-agnostic interface to allow swapping implementations
 */
public interface VectorStoreClient {

    /**
     * Store a document chunk with its embedding vector and metadata.
     *
     * @param vectorId      Unique identifier for the vector (typically documentChunk.id)
     * @param embedding     The embedding vector (e.g., 1536 dimensions for OpenAI)
     * @param metadata      Key-value metadata (document_id, chunk_index, content, etc.)
     */
    void storeEmbedding(String vectorId, List<Float> embedding, Map<String, Object> metadata);

    /**
     * Semantic search: find top-K most similar chunks for a query embedding.
     *
     * @param queryEmbedding    The query vector (same dimensions as stored embeddings)
     * @param topK              Number of results to return (default: 5)
     * @param documentFilter    Optional document ID to filter search results (null = search all)
     * @return List of SearchResult objects (sorted by similarity, highest first)
     */
    List<VectorSearchResult> search(List<Float> queryEmbedding, int topK, String documentFilter);

    /**
     * Delete all embeddings associated with a document.
     *
     * @param documentId    The ID of the document to delete
     */
    void deleteDocument(String documentId);

    /**
     * Update an existing chunk's embedding (for reprocessing).
     *
     * @param vectorId      The vector ID to update
     * @param embedding     The new embedding vector
     * @param metadata      Updated metadata
     */
    void updateEmbedding(String vectorId, List<Float> embedding, Map<String, Object> metadata);

    /**
     * Check if a vector ID exists in the store.
     *
     * @param vectorId  The vector ID to check
     * @return true if exists, false otherwise
     */
    boolean exists(String vectorId);

    /**
     * Clear all embeddings from the collection (use with caution - typically for testing).
     */
    void deleteAll();

    /**
     * Get health status of the vector store.
     *
     * @return true if connected and operational, false otherwise
     */
    boolean isHealthy();
}
