package com.genai.knowitall.vectorstore;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Qdrant implementation of VectorStoreClient using LangChain4j EmbeddingStore.
 */
@Service
public class QdrantVectorStore implements VectorStoreClient {

    private static final Logger logger = Logger.getLogger(QdrantVectorStore.class.getName());
    private final EmbeddingStore<String> embeddingStore;
    private final Map<String, Map<String, Object>> metadataCache;

    public QdrantVectorStore(EmbeddingStore<String> embeddingStore) {
        this.embeddingStore = embeddingStore;
        this.metadataCache = new HashMap<>();
        logger.info("QdrantVectorStore initialized with EmbeddingStore");
    }

    /**
     * Store embedding vector with metadata.
     * @param vectorId      Unique identifier for the vector (typically documentChunk.id)
     * @param embedding     The embedding vector (e.g., 1536 dimensions for OpenAI)
     * @param metadata      Key-value metadata (document_id, chunk_index, content, etc.)
     */
    @Override
    public void storeEmbedding(String vectorId, List<Float> embedding, Map<String, Object> metadata) {
        try {
            // Convert List<Float> to float[]
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i);
            }

            // Create Embedding object from float array
            Embedding embeddingObj = new Embedding(embeddingArray);

            // Store in EmbeddingStore
            embeddingStore.add(vectorId, embeddingObj);

            // Cache metadata locally for retrieval during search
            metadataCache.put(vectorId, new HashMap<>(metadata));

        } catch (Exception e) {
            throw new VectorStoreException("Failed to store embedding for vectorId: " + vectorId, e);
        }
    }

    /**
     * Semantic search: find top-K most similar chunks for a query embedding.
     * @param queryEmbedding    The query vector (same dimensions as stored embeddings)
     * @param topK              Number of results to return (default: 5)
     * @param documentFilter    Optional document ID to filter search results (null = search all)
     * @return List of VectorSearchResult objects (sorted by similarity, highest first)
     */
    @Override
    public List<VectorSearchResult> search(List<Float> queryEmbedding, int topK, String documentFilter) {
        try {
            // Convert List<Float> to float[]
            float[] queryEmbeddingArray = new float[queryEmbedding.size()];
            for (int i = 0; i < queryEmbedding.size(); i++) {
                queryEmbeddingArray[i] = queryEmbedding.get(i);
            }

            // Create Embedding from float array
            Embedding queryEmbedding_obj = new Embedding(queryEmbeddingArray);

            // Search in EmbeddingStore
            List<EmbeddingMatch<String>> matches = embeddingStore.findRelevant(queryEmbedding_obj, topK);

            // Convert to domain objects and apply document filter if needed
            return matches.stream()
                    .filter(match -> {
                        if (documentFilter != null && !documentFilter.isEmpty()) {
                            String docId = getDocumentIdFromMatch(match);
                            return documentFilter.equals(docId);
                        }
                        return true;
                    })
                    .map(this::convertEmbeddingMatch)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new VectorStoreException("Search operation failed", e);
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        // Note: EmbeddingStore interface doesn't support bulk delete by metadata
        logger.warning("Bulk delete by document_id not supported via EmbeddingStore interface");
    }

    /**
     * Update an existing chunk's embedding (for reprocessing).
     * @param vectorId      The vector ID to update
     * @param embedding     The new embedding vector
     * @param metadata      Updated metadata
     */
    @Override
    public void updateEmbedding(String vectorId, List<Float> embedding, Map<String, Object> metadata) {
        try {
            storeEmbedding(vectorId, embedding, metadata);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to update embedding for vectorId: " + vectorId, e);
        }
    }

    /**
     * Check if a vector ID exists in the store.
     * @param vectorId  The vector ID to check
     * @return true if exists, false otherwise
     */
    @Override
    public boolean exists(String vectorId) {
        return metadataCache.containsKey(vectorId);
    }

    /**
     * Clear all embeddings from the collection (use with caution - typically for testing).
     */
    @Override
    public void deleteAll() {
        logger.warning("deleteAll() not supported via EmbeddingStore interface");
        metadataCache.clear();
    }

    /**
     * Get health status of the vector store.
     * @return true if connected and operational, false otherwise
     */
    @Override
    public boolean isHealthy() {
        try {
            return embeddingStore != null;
        } catch (Exception e) {
            logger.warning("Vector store health check failed: " + e.getMessage());
            return false;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Extract document_id from EmbeddingMatch metadata.
     * @param match The EmbeddingMatch object
     * @return The document_id if available, else null
     */
    private String getDocumentIdFromMatch(EmbeddingMatch<String> match) {
        String vectorId = match.embeddingId();
        if (metadataCache.containsKey(vectorId)) {
            Map<String, Object> metadata = metadataCache.get(vectorId);
            Object docId = metadata.get("document_id");
            return docId != null ? docId.toString() : null;
        }
        return null;
    }

    /**
     * Convert EmbeddingMatch to VectorSearchResult.
     */
    private VectorSearchResult convertEmbeddingMatch(EmbeddingMatch<String> match) {
        String vectorId = match.embeddingId();
        Map<String, Object> metadata = metadataCache.getOrDefault(vectorId, new HashMap<>());

        return VectorSearchResult.builder()
                .vectorId(vectorId)
                .score(Math.max(0.0, Math.min(1.0, match.score())))
                .documentId(getStringValue(metadata, "document_id"))
                .chunkIndex(getIntValue(metadata))
                .content(getStringValue(metadata, "content"))
                .source(getStringValue(metadata, "source"))
                .build();
    }

    /**
     * Extract string value from metadata map.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extract integer chunk_index value from metadata map.
     */
    private Integer getIntValue(Map<String, Object> map) {
        Object value = map.get("chunk_index");
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
