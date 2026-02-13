package com.genai.knowitall.service;

import com.genai.knowitall.service.exception.EmbeddingException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for generating text embeddings using OpenAI models via LangChain4j.
 */
@Service
public class EmbeddingService {

    private static final Logger logger = Logger.getLogger(EmbeddingService.class.getName());

    private final EmbeddingModel embeddingModel;
    /**
     * -- GETTER --
     *  Get the name of the embedding model being used.
     */
    @Getter
    private final String embeddingModelName;
    private final int maxRetries;
    private final long retryBackoffMs;

    public EmbeddingService(
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.embedding.model:text-embedding-3-small}") String modelName,
            @Value("${doc.embedding.retry.max-attempts:3}") int maxRetries,
            @Value("${doc.embedding.retry.backoff-ms:1000}") long retryBackoffMs) {

        this.embeddingModelName = modelName;
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;

        logger.info("Initializing EmbeddingService with model: " + modelName);

        try {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException(
                    "OpenAI API key is not set. Set OPEN_AI_KEY or OPENAI_API_KEY environment variable, or openai.api.key in configuration.");
            }

            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(3)
                    .logRequests(false)
                    .logResponses(false)
                    .build();

            logger.info("✓ EmbeddingService initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize EmbeddingService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize EmbeddingService", e);
        }
    }

    /**
     * Generate embedding for the given text.
     * Retry on transient failures with exponential backoff.
     * @param text Input text to embed
     * @return List of Float representing the embedding vector
     * @throws EmbeddingException if embedding generation fails
     */
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }

        logger.fine("Generating embedding for text (length: " + text.length() + " chars)");

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                // Generate embedding using LangChain4j
                Embedding embedding = embeddingModel.embed(text).content();

                // Convert to List<Float>
                float[] vector = embedding.vector();
                List<Float> embeddingList = new ArrayList<>(vector.length);
                for (float value : vector) {
                    embeddingList.add(value);
                }

                logger.fine("Successfully generated embedding (dimensions: " + embeddingList.size() + ")");
                return embeddingList;

            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < maxRetries) {
                    long backoff = retryBackoffMs * (long) Math.pow(2, attempt - 1);
                    logger.warning("Embedding generation failed (attempt " + attempt + "/" + maxRetries +
                                 "), retrying in " + backoff + "ms: " + e.getMessage());

                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmbeddingException("Embedding generation interrupted", ie);
                    }
                } else {
                    logger.severe("Embedding generation failed after " + maxRetries + " attempts: " + e.getMessage());
                }
            }
        }

        throw new EmbeddingException("Failed to generate embedding after " + maxRetries + " attempts", lastException);
    }

}
