package com.genai.knowitall.controller;

import com.genai.knowitall.controller.dto.QueryRequest;
import com.genai.knowitall.controller.dto.QueryResponse;
import com.genai.knowitall.service.RAGService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * REST Controller for RAG query/search operations.
 *
 * Endpoint:
 * - POST /api/query - Submit a question and get RAG-grounded answer
 *
 * Features:
 * - Semantic search across all documents
 * - Document filtering (optional)
 * - Confidence scoring and grounding detection
 * - Source attribution with excerpts
 * - Performance metrics (retrieval time, generation time)
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {

    private static final Logger logger = Logger.getLogger(QueryController.class.getName());

    private final RAGService ragService;

    public QueryController(RAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * Execute a RAG query.
     *
     * Request body example:
     * {
     *   "question": "What are the benefits of AI in healthcare?",
     *   "topK": 5,
     *   "confidenceThreshold": 0.7,
     *   "documentFilter": null
     * }
     *
     * Response example:
     * {
     *   "answer": "AI in healthcare provides multiple benefits including...",
     *   "confidence": 0.92,
     *   "isGrounded": true,
     *   "sources": [
     *     {
     *       "documentId": "doc-123",
     *       "documentTitle": "AI in Healthcare",
     *       "chunkIndex": 5,
     *       "similarity": 0.94,
     *       "excerpt": "AI models can detect diseases early..."
     *     }
     *   ],
     *   "chunksRetrieved": 3,
     *   "retrievalTimeMs": 45,
     *   "generationTimeMs": 1200,
     *   "totalTimeMs": 1245
     * }
     *
     * @param request QueryRequest with question and optional parameters
     * @return QueryResponse with answer, confidence, and sources
     */
    @PostMapping
    public ResponseEntity<?> query(@RequestBody QueryRequest request) {
        logger.info("Received query request: " + request.getQuestion());

        // Validate request
        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
        }

        try {
            // Call RAG service
            QueryResponse response = ragService.query(
                    request.getQuestion(),
                    request.getTopK(),
                    request.getConfidenceThreshold(),
                    request.getDocumentFilter()
            );

            // Log metrics
            logger.info("Query processed: confidence=" + String.format("%.2f", response.getConfidence()) +
                       ", grounded=" + response.getIsGrounded() +
                       ", sources=" + response.getSources().size() +
                       ", totalTime=" + response.getTotalTimeMs() + "ms");

            // Return response
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.severe("Query processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process query: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for query service.
     *
     * Returns: { "status": "healthy", "message": "Query service is operational" }
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "message", "Query service is operational"
        ));
    }
}
