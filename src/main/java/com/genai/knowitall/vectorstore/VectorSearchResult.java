package com.genai.knowitall.vectorstore;

import lombok.*;

/**
 * Data Transfer Object representing a single search result from the vector store.
 * Contains the matched chunk metadata and similarity score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorSearchResult {

    /**
     * The vector ID (typically documentChunk.id)
     */
    private String vectorId;

    /**
     * Similarity score (0.0 to 1.0, where 1.0 is identical)
     */
    private Double score;

    /**
     * Document ID this chunk belongs to
     */
    private String documentId;

    /**
     * Sequential index of this chunk within the document
     */
    private Integer chunkIndex;

    /**
     * The actual text content of the chunk
     */
    private String content;

    /**
     * Source/filename for reference
     */
    private String source;
}
