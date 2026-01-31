package com.genai.knowitall.vectorstore;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

/**
 * Configuration class to set up LangChain4j EmbeddingStore backed by Qdrant.
 * This class reads Qdrant connection properties from application configuration
 * and initializes the EmbeddingStore and VectorStoreClient beans.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger logger = Logger.getLogger(VectorStoreConfig.class.getName());

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    @Value("${qdrant.api.key:}")
    private String qdrantApiKey;

    @Value("${qdrant.collection.name}")
    private String collectionName;

    /**
     * Create EmbeddingStore bean using Qdrant implementation from LangChain4j.
     * @return EmbeddingStore implementation backed by Qdrant
     */
    @Bean
    public EmbeddingStore<String> embeddingStore() {
        logger.info("Initializing Qdrant EmbeddingStore: " + qdrantHost + ":" + qdrantPort +
                    ", collection: " + collectionName);

        try {
            // Create QdrantEmbeddingStore with configuration
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
            logger.severe("Make sure Qdrant is running. Start with: docker run -p 6333:6333 qdrant/qdrant");
            throw new RuntimeException(msg, e);
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
