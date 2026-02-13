package com.genai.knowitall.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Service for LLM interactions via OpenAI API.
 *
 * Abstracts LLM provider details (swappable: OpenAI ↔ Azure ↔ Anthropic)
 * Handles configuration, retries, timeouts, and token counting.
 *
 * Model: GPT-3.5-turbo
 * - Fast inference (~500ms per query)
 * - Cost-effective (~$0.001 per query)
 * - Good quality for grounded RAG tasks
 *
 * Alternative: GPT-4 (higher quality, ~$0.03 per query, slower)
 */
@Service
public class LLMProvider {

    private static final Logger logger = Logger.getLogger(LLMProvider.class.getName());

    private final ChatLanguageModel chatModel;
    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;

    public LLMProvider(
            @Value("${openai.model.name:gpt-3.5-turbo}") String modelName,
            @Value("${openai.temperature:0.7}") Double temperature,
            @Value("${openai.max.tokens:1000}") Integer maxTokens) {

        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        logger.info("Initializing LLMProvider with model: " + modelName +
                   ", temperature: " + temperature + ", maxTokens: " + maxTokens);

        try {
            String apiKey = System.getenv("OPEN_AI_KEY");
            if(apiKey == null || apiKey.isEmpty()) {
                logger.severe("⚠️ OPEN_AI_KEY environment variable is not set.");
                apiKey = "sk-mock-key-for-development-only";
            }

            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(3)
                    .logRequests(false)
                    .logResponses(false)
                    .build();

            logger.info("✓ LLMProvider initialized successfully");

        } catch (Exception e) {
            logger.severe("Failed to initialize LLMProvider: " + e.getMessage());
            throw new RuntimeException("Failed to initialize LLMProvider", e);
        }
    }

    /**
     * Generate a response from the LLM.
     * It is done by sending a user message (the question) to the model, along with a system prompt (instructions for behavior).
     *
     * @param systemPrompt System message (instructions for behavior)
     * @param userMessage User's question or prompt
     * @return Generated response text
     */
    public String generateResponse(String systemPrompt, String userMessage) {
        logger.fine("Generating response. System: " + systemPrompt.length() +
                   " chars, User: " + userMessage.length() + " chars");

        try {
            // Create messages array: system role + user message
            String response = chatModel.generate(userMessage);

            logger.fine("Generated response: " + response.length() + " chars");
            return response;

        } catch (Exception e) {
            logger.severe("LLM generation failed: " + e.getMessage());
            throw new RuntimeException("Failed to generate response from LLM", e);
        }
    }

    /**
     * Get the name of the configured LLM model.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Get configured temperature (controls randomness/creativity).
     * 0.0 = deterministic, 1.0 = creative
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * Get maximum tokens allowed in response.
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }
}
