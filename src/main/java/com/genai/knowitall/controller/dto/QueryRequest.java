package com.genai.knowitall.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for querying the RAG system.
 *
 * Example:
 * {
 *   "question": "What are the benefits of AI in healthcare?",
 *   "topK": 5,
 *   "confidenceThreshold": 0.7,
 *   "documentFilter": "optional-document-id"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryRequest {

    /**
     * The question to answer
     */
    private String question;

    /**
     * Number of chunks to retrieve for context (default: 5)
     */
    @Builder.Default
    private Integer topK = 5;

    /**
     * Minimum similarity score (0-1) to consider a chunk as relevant (default: 0.7)
     * If no chunks meet this threshold, return low-confidence answer
     */
    @Builder.Default
    private Double confidenceThreshold = 0.7;

    /**
     * Optional: Filter results to a specific document
     * If null, search across all documents
     */
    private String documentFilter;

    /**
     * Validation: question must not be empty
     */
    public boolean isValid() {
        return question != null && !question.trim().isEmpty();
    }
}
