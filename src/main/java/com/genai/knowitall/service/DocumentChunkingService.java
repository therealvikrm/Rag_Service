package com.genai.knowitall.service;

import com.genai.knowitall.model.DocumentChunk;
import com.genai.knowitall.service.exception.ChunkingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service for splitting documents into overlapping chunks.
 *
 * Chunks are created with configurable size and overlap to maintain
 * semantic context across boundaries. This is critical for RAG systems
 * to avoid losing information at chunk boundaries.
 *
 * Strategy: Token-based chunking with overlap
 * - Default: 512 tokens per chunk with 50-token overlap
 * - Tokens are estimated at 1.3 tokens per word (English)
 */
@Service
public class DocumentChunkingService {

    private static final Logger logger = Logger.getLogger(DocumentChunkingService.class.getName());

    @Value("${doc.chunking.size.tokens:512}")
    private int chunkSizeTokens;

    @Value("${doc.chunking.overlap.tokens:50}")
    private int overlapTokens;

    // Approximate: 1 token ≈ 0.75 words for English (conservative estimate)
    private static final double CHARS_PER_TOKEN = 4.0;

    /**
     * Chunk a document's text into overlapping segments.
     * @param documentId ID of the document
     * @param text Full text content of the document
     * @return List of DocumentChunk objects
     * @throws ChunkingException if chunking fails
     */
    public List<DocumentChunk> chunkDocument(String documentId, String text) {
        logger.info("Chunking document " + documentId + " - text length: " + text.length() + " chars");

        if (text.trim().isEmpty()) {
            throw new ChunkingException("Cannot chunk empty text", documentId);
        }

        // Safety check: prevent chunking extremely large documents
        if (text.length() > 10_000_000) { // 10MB limit
            throw new ChunkingException("Document too large for chunking: " + text.length() + " chars (max 10MB)", documentId);
        }

        try {
            List<DocumentChunk> chunks = new ArrayList<>();

            // Calculate character-based chunk sizes from token estimates
            int chunkSizeChars = (int) (chunkSizeTokens * CHARS_PER_TOKEN);
            int overlapChars = (int) (overlapTokens * CHARS_PER_TOKEN);

            int startIndex = 0;
            int chunkIndex = 0;
            int safetyCounter = 0;
            int maxChunks = (text.length() / chunkSizeChars) + 10; // Safety limit

            // Loop to create chunks.
            while (startIndex < text.length() && safetyCounter < maxChunks) {
                safetyCounter++;

                // Calculate end index for this chunk
                int endIndex = Math.min(startIndex + chunkSizeChars, text.length());

                // Try to break at sentence boundary (period, question mark, exclamation)
                if (endIndex < text.length()) {
                    int sentenceBreak = findSentenceBreak(text, endIndex, startIndex + chunkSizeChars / 2);
                    if (sentenceBreak > startIndex) {
                        endIndex = sentenceBreak; //1996,
                    }
                }

                // Extract chunk text - avoid trim() to prevent memory issues
                String chunkText = text.substring(startIndex, endIndex);

                // Only trim if chunk is small enough to be safe
                if (chunkText.length() < 10000) {
                    chunkText = chunkText.trim();
                }

                if (!chunkText.isEmpty()) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .id(UUID.randomUUID().toString())
                            .documentId(documentId)
                            .content(chunkText)
                            .chunkIndex(chunkIndex)
                            .tokenCount(estimateTokenCount(chunkText))
                            .build();

                    chunks.add(chunk);
                    chunkIndex++;
                }

                // Move to next chunk position
                // Always move forward by at least chunkSizeChars - overlapChars
                int moveForward = Math.max(chunkSizeChars - overlapChars, 1);
                startIndex = startIndex + moveForward;

                // Safety check: ensure we're making progress
                if (startIndex >= text.length()) {
                    break;
                }
            }

            logger.info("Created " + chunks.size() + " chunks for document " + documentId);
            return chunks;

        } catch (Exception e) {
            logger.severe("Failed to chunk document " + documentId + ": " + e.getMessage());
            throw new ChunkingException("Failed to chunk document: " + e.getMessage(), documentId, e);
        }
    }

    /**
     * Find the nearest sentence boundary (period, question mark, exclamation)
     * within a reasonable range of the target index.
     *
     * @param text Full text
     * @param targetIndex Target end index
     * @param minIndex Minimum index to search
     * @return Index of sentence break, or -1 if not found
     */
    private int findSentenceBreak(String text, int targetIndex, int minIndex) {
        // Search backward from targetIndex to minIndex
        for (int i = targetIndex; i >= minIndex && i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!') {
                // Found sentence break, return position after the punctuation
                return Math.min(i + 1, text.length());
            }
        }
        return -1;
    }

    /**
     * Estimate token count for a text string.
     * Uses approximation: 1 token ≈ 4 characters for English.
     *
     * For production, consider using OpenAI's tiktoken library or
     * LangChain4j's TokenCounter for accurate counts.
     *
     * @param text Text to estimate
     * @return Estimated token count
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Get configured chunk size in tokens.
     */
    public int getChunkSizeTokens() {
        return chunkSizeTokens;
    }

    /**
     * Get configured overlap size in tokens.
     */
    public int getOverlapTokens() {
        return overlapTokens;
    }
}
