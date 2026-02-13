package com.genai.knowitall.service;

import com.genai.knowitall.controller.dto.QueryResponse;
import com.genai.knowitall.controller.dto.SourceReference;
import com.genai.knowitall.model.DocumentChunk;
import com.genai.knowitall.repository.DocumentChunkRepository;
import com.genai.knowitall.vectorstore.VectorSearchResult;
import com.genai.knowitall.vectorstore.VectorStoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Core RAG (Retrieval-Augmented Generation) Service.
 *
 * Orchestrates the complete RAG pipeline:
 * 1. Embed user question
 * 2. Retrieve top-K similar document chunks from Qdrant
 * 3. Score confidence based on similarity
 * 4. Generate LLM response grounded in retrieved context
 * 5. Attach source references
 *
 * Error Handling Strategy: "Best Effort"
 * - If no high-confidence context found: still generate answer (low confidence flag)
 * - If LLM fails: return error response
 * - If retrieval fails: return error response
 */
@Service
public class RAGService {

    private static final Logger logger = Logger.getLogger(RAGService.class.getName());

    private final VectorStoreClient vectorStoreClient;
    private final EmbeddingService embeddingService;
    private final LLMProvider llmProvider;
    private final DocumentChunkRepository chunkRepository;

    @Value("${rag.retrieval.top.k:5}")
    private Integer defaultTopK;

    @Value("${rag.confidence.threshold:0.7}")
    private Double defaultConfidenceThreshold;

    @Value("${rag.max.context.tokens:2000}")
    private Integer maxContextTokens;

    public RAGService(
            VectorStoreClient vectorStoreClient,
            EmbeddingService embeddingService,
            LLMProvider llmProvider,
            DocumentChunkRepository chunkRepository) {

        this.vectorStoreClient = vectorStoreClient;
        this.embeddingService = embeddingService;
        this.llmProvider = llmProvider;
        this.chunkRepository = chunkRepository;

        logger.info("RAGService initialized");
    }

    /**
     * Process a query end-to-end using RAG pipeline.
     *
     * @param question User's question
     * @param topK Number of chunks to retrieve
     * @param confidenceThreshold Minimum similarity to consider context valid
     * @param documentFilter Optional: filter to specific document ID
     * @return QueryResponse with answer, confidence, and sources
     */
    public QueryResponse query(String question, Integer topK, Double confidenceThreshold, String documentFilter) {
        long startTime = System.currentTimeMillis();
        long retrievalStartTime = startTime;

        logger.info("Processing query: " + question + " (topK: " + topK + ", threshold: " + confidenceThreshold + ")");

        try {
            // Resolve defaults
            if (topK == null) topK = defaultTopK;
            if (confidenceThreshold == null) confidenceThreshold = defaultConfidenceThreshold;

            // Stage 1: Embed question
            logger.fine("Embedding question...");
            List<Float> questionEmbedding = embeddingService.generateEmbedding(question);

            // Stage 2: Retrieve similar chunks from Qdrant
            logger.fine("Retrieving top-" + topK + " chunks from vector store...");
            List<VectorSearchResult> searchResults = vectorStoreClient.search(
                    questionEmbedding,
                    topK,
                    documentFilter
            );

            long retrievalEndTime = System.currentTimeMillis();
            long retrievalTimeMs = retrievalEndTime - retrievalStartTime;

            logger.fine("Retrieved " + searchResults.size() + " chunks in " + retrievalTimeMs + "ms");

            // Stage 3: Score confidence
            Double confidence = 0.0;
            Boolean isGrounded = false;

            if (!searchResults.isEmpty()) {
                // Get max similarity as confidence score
                confidence = searchResults.stream()
                        .mapToDouble(VectorSearchResult::getScore)
                        .max()
                        .orElse(0.0);

                // Check if confidence meets threshold
                isGrounded = confidence >= confidenceThreshold;
            }

            logger.fine("Confidence score: " + confidence + ", Grounded: " + isGrounded);

            // Stage 4: Build context from retrieved chunks
            String context = buildContext(searchResults);
            List<SourceReference> sources = buildSourceReferences(searchResults);

            // Stage 5: Generate answer via LLM
            long generationStartTime = System.currentTimeMillis();

            String answer = generateAnswer(question, context, isGrounded);

            long generationEndTime = System.currentTimeMillis();
            long generationTimeMs = generationEndTime - generationStartTime;

            long totalTimeMs = generationEndTime - startTime;

            logger.info("Query processed successfully in " + totalTimeMs + "ms " +
                       "(retrieval: " + retrievalTimeMs + "ms, generation: " + generationTimeMs + "ms)");

            // Stage 6: Build response
            return QueryResponse.builder()
                    .answer(answer)
                    .confidence(confidence)
                    .isGrounded(isGrounded)
                    .sources(sources)
                    .chunksRetrieved(searchResults.size())
                    .retrievalTimeMs(retrievalTimeMs)
                    .generationTimeMs(generationTimeMs)
                    .totalTimeMs(totalTimeMs)
                    .build();

        } catch (Exception e) {
            logger.severe("Query processing failed: " + e.getMessage());
            long totalTimeMs = System.currentTimeMillis() - startTime;

            return QueryResponse.builder()
                    .answer(null)
                    .confidence(0.0)
                    .isGrounded(false)
                    .sources(List.of())
                    .chunksRetrieved(0)
                    .totalTimeMs(totalTimeMs)
                    .error("Failed to process query: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Build context string from retrieved chunks.
     * Concatenates chunk content with metadata markers.
     *
     * @param searchResults Retrieved chunks sorted by relevance
     * @return Formatted context for LLM
     */
    private String buildContext(List<VectorSearchResult> searchResults) {
        if (searchResults.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder();
        int totalTokens = 0;

        for (VectorSearchResult result : searchResults) {
            if (totalTokens >= maxContextTokens) {
                logger.fine("Context token limit reached");
                break;
            }

            String chunkText = result.getContent();
            int chunkTokens = estimateTokens(chunkText);

            if (totalTokens + chunkTokens > maxContextTokens) {
                // Truncate this chunk to fit
                int remainingTokens = maxContextTokens - totalTokens;
                chunkText = truncateByTokens(chunkText, remainingTokens);
            }

            contextBuilder.append("[Document: ").append(result.getDocumentId())
                    .append(" | Chunk ").append(result.getChunkIndex())
                    .append(" | Similarity: ").append(String.format("%.2f", result.getScore()))
                    .append("]\n")
                    .append(chunkText)
                    .append("\n\n");

            totalTokens += chunkTokens;
        }

        return contextBuilder.toString();
    }

    /**
     * Build source references from search results.
     * Includes document metadata and excerpts.
     */
    private List<SourceReference> buildSourceReferences(List<VectorSearchResult> searchResults) {
        return searchResults.stream()
                .map(result -> {
                    // Fetch full chunk for metadata
                    Optional<DocumentChunk> chunk = chunkRepository.findById(result.getVectorId());

                    SourceReference.SourceReferenceBuilder builder = SourceReference.builder()
                            .documentId(result.getDocumentId())
                            .chunkIndex(result.getChunkIndex())
                            .similarity(result.getScore())
                            .excerpt(truncateExcerpt(result.getContent(), 200));

                    // Add additional metadata if chunk found in DB
                    if (chunk.isPresent()) {
                        DocumentChunk dc = chunk.get();
                        builder.tokenCount(dc.getTokenCount());
                        // Optionally fetch document title from relationship
                        if (dc.getDocument() != null) {
                            builder.documentTitle(dc.getDocument().getTitle());
                        }
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate answer using LLM, constrained to retrieved context.
     *
     * @param question User's question
     * @param context Retrieved context from documents
     * @param isGrounded Whether context meets confidence threshold
     * @return LLM-generated answer
     */
    private String generateAnswer(String question, String context, Boolean isGrounded) {
        // Build system prompt
        String systemPrompt = isGrounded
                ? buildSystemPromptGrounded()
                : buildSystemPromptUngrounded();

        // Build user message with context
        String userMessage = buildUserMessage(question, context);

        logger.fine("Sending to LLM. System prompt length: " + systemPrompt.length() +
                   ", User message length: " + userMessage.length());

        return llmProvider.generateResponse(systemPrompt, userMessage);
    }

    /**
     * System prompt for grounded answers (high-confidence context available).
     */
    private String buildSystemPromptGrounded() {
        return """
                You are a helpful AI assistant that answers questions based strictly on provided context.
                
                IMPORTANT RULES:
                1. Answer ONLY based on the provided context below
                2. If the context doesn't contain information to answer the question, say "I don't have enough information"
                3. Be concise and factual
                4. Do NOT make up or infer information not in the context
                5. If the answer spans multiple chunks, synthesize them naturally
                
                Provided Context:
                """;
    }

    /**
     * System prompt for best-effort answers (low-confidence context).
     */
    private String buildSystemPromptUngrounded() {
        return """
                You are a helpful AI assistant. The following context is available but may be limited.
                
                IMPORTANT RULES:
                1. If the context below helps answer the question, use it as primary source
                2. If context is insufficient, provide the best answer you can based on your knowledge
                3. Be honest about context limitations
                4. Clearly indicate if you're going beyond the provided context
                5. Be concise and factual
                
                Available Context (limited):
                """;
    }

    /**
     * Build the user message combining question and context.
     */
    private String buildUserMessage(String question, String context) {
        if (context.isEmpty()) {
            return "Question: " + question + "\n\n(No relevant context found in documents)";
        }

        return context + "\n\nQuestion: " + question + "\n\nAnswer: ";
    }

    /**
     * Estimate token count for text (1 token ≈ 4 characters).
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Truncate text to approximately N tokens.
     */
    private String truncateByTokens(String text, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }

        String truncated = text.substring(0, maxChars);
        // Try to break at sentence boundary
        int lastPeriod = truncated.lastIndexOf('.');
        if (lastPeriod > 0) {
            return truncated.substring(0, lastPeriod + 1);
        }

        return truncated + "...";
    }

    /**
     * Truncate excerpt to specified length for display.
     */
    private String truncateExcerpt(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0) {
            return truncated.substring(0, lastSpace) + "...";
        }

        return truncated + "...";
    }
}
