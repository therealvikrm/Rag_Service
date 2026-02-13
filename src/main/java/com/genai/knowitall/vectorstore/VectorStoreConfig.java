package com.genai.knowitall.vectorstore;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration class to set up LangChain4j EmbeddingStore backed by Qdrant.
 * Creates the Qdrant collection if it does not exist, then initializes the store.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger logger = Logger.getLogger(VectorStoreConfig.class.getName());

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    @Value("${qdrant.rest.port:6333}")
    private int qdrantRestPort;

    @Value("${qdrant.api.key:}")
    private String qdrantApiKey;

    @Value("${qdrant.collection.name}")
    private String collectionName;

    @Value("${qdrant.vector.size:1536}")
    private int vectorSize;

    /**
     * Create EmbeddingStore bean using Qdrant implementation from LangChain4j.
     * Ensures the collection exists (creates it via REST if missing).
     */
    @Bean
    public EmbeddingStore<String> embeddingStore() {
        logger.info("Initializing Qdrant EmbeddingStore: " + qdrantHost + ":" + qdrantPort +
                    ", collection: " + collectionName);

        try {
            ensureCollectionExists();
            QdrantEmbeddingStore store = QdrantEmbeddingStore.builder()
                    .host(qdrantHost)
                    .port(qdrantPort)
                    .collectionName(collectionName)
                    .build();
            logger.info("✓ Qdrant EmbeddingStore initialized successfully");
            return (EmbeddingStore<String>) (EmbeddingStore<?>) store;
        } catch (Exception e) {
            String msg = "Failed to initialize Qdrant EmbeddingStore at " + qdrantHost + ":" + qdrantPort;
            logger.severe(msg);
            logger.severe("Make sure Qdrant is running. Use gRPC port 6334. Example: docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant");
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Create the Qdrant collection via REST API if it does not exist.
     * Uses REST port (6333); vector size must match embedding model (e.g. 1536 for text-embedding-3-small).
     */
    private void ensureCollectionExists() {
        String url = "http://" + qdrantHost + ":" + qdrantRestPort + "/collections/" + collectionName;
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Request body: {"vectors": {"size": 1536, "distance": "Cosine"}}
        Map<String, Object> vectorsConfig = Map.of(
                "size", vectorSize,
                "distance", "Cosine"
        );
        Map<String, Object> body = Map.of("vectors", vectorsConfig);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.PUT, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Created Qdrant collection: " + collectionName);
            }
        } catch (Exception e) {
            // 409 or 400 may mean collection already exists
            String msg = e.getMessage();
            if (msg != null && (msg.contains("409") || msg.contains("already exists") || msg.contains("400"))) {
                logger.fine("Collection " + collectionName + " already exists or creation returned: " + msg);
                return;
            }
            logger.warning("Could not ensure Qdrant collection exists (will fail on first upsert if missing): " + e.getMessage());
        }
    }

    /**
     * Create VectorStoreClient bean using the EmbeddingStore.
     * @param embeddingStore LangChain4j EmbeddingStore bean
     * @return VectorStoreClient implementation
     */
    @Bean
    public VectorStoreClient vectorStoreClient(EmbeddingStore<String> embeddingStore) {
        logger.info("Creating QdrantVectorStore service bean");
        return new QdrantVectorStore(embeddingStore);
    }
}
