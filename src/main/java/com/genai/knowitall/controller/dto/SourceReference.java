package com.genai.knowitall.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a source (document chunk) used to generate the answer.
 * Includes the chunk's similarity score and excerpt for transparency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceReference {

    /**
     * ID of the document this chunk came from
     */
    private String documentId;

    /**
     * Title of the source document
     */
    private String documentTitle;

    /**
     * Sequential index of this chunk within the document
     */
    private Integer chunkIndex;

    /**
     * Similarity score (0-1) showing how relevant this chunk is to the query
     * 1.0 = perfect match, 0.0 = no relation
     */
    private Double similarity;

    /**
     * The actual text excerpt from the chunk
     * Allows users to verify the source and read full context
     */
    private String excerpt;

    /**
     * Optional: token count of the original chunk
     */
    private Integer tokenCount;
}
