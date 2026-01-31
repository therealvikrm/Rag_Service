package com.genai.knowitall.service;

import com.genai.knowitall.model.Document;
import com.genai.knowitall.model.DocumentChunk;
import com.genai.knowitall.model.DocumentStatus;
import com.genai.knowitall.repository.DocumentChunkRepository;
import com.genai.knowitall.repository.DocumentRepository;
import com.genai.knowitall.service.exception.DocumentProcessingException;
import com.genai.knowitall.vectorstore.VectorStoreClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Async service for orchestrating document ingestion pipeline.
 *
 * Pipeline stages:
 * 1. Update status → PROCESSING
 * 2. Extract text from document
 * 3. Chunk document into overlapping segments
 * 4. Generate embeddings for each chunk
 * 5. Store embeddings in Qdrant vector store
 * 6. Save chunks with vectorId to database
 * 7. Update status → READY or FAILED
 *
 * Uses @Async for non-blocking processing.
 * Error handling: "best effort" - skip failed chunks, continue processing.
 */
@Service
public class DocumentIngestionService {

    private static final Logger logger = Logger.getLogger(DocumentIngestionService.class.getName());

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreClient vectorStoreClient;

    public DocumentIngestionService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            TextExtractionService textExtractionService,
            DocumentChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStoreClient vectorStoreClient) {

        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreClient = vectorStoreClient;
    }

    /**
     * Asynchronously process a document through the ingestion pipeline.
     * @param documentId ID of the document to process
     * @param textContent Extracted text content of the document
     */
    @Async
    public void processDocumentAsync(String documentId, String textContent) {
        logger.info("Starting async processing for document: " + documentId);

        try {
            // Stage 1: Update status to PROCESSING
            updateDocumentStatus(documentId, DocumentStatus.PROCESSING, null);

            // Stage 2: Chunk document
            logger.info("Chunking document: " + documentId);
            List<DocumentChunk> chunks = chunkingService.chunkDocument(documentId, textContent);

            if (chunks.isEmpty()) {
                throw new DocumentProcessingException("No chunks generated for document", documentId);
            }

            // Update total chunks count
            updateDocumentTotalChunks(documentId, chunks.size());
            logger.info("Created " + chunks.size() + " chunks for document: " + documentId);

            // Stage 3 & 4 & 5: Generate embeddings and store (best effort)
            int successCount = 0;
            int failedCount = 0;
            String embeddingModelName = embeddingService.getEmbeddingModelName();

            for (DocumentChunk chunk : chunks) {
                try {
                    // Generate embedding for this chunk
                    List<Float> embedding = embeddingService.generateEmbedding(chunk.getContent());

                    // Prepare metadata for vector store
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("document_id", documentId);
                    metadata.put("chunk_index", chunk.getChunkIndex());
                    metadata.put("content", chunk.getContent());
                    metadata.put("token_count", chunk.getTokenCount());

                    // Store in Qdrant
                    vectorStoreClient.storeEmbedding(chunk.getId(), embedding, metadata);

                    // Update chunk with vector ID and embedding model
                    chunk.setVectorId(chunk.getId());
                    chunk.setEmbeddingModel(embeddingModelName);

                    // Save chunk to database
                    chunkRepository.save(chunk);

                    successCount++;
                    logger.fine("Processed chunk " + (chunk.getChunkIndex() + 1) + "/" + chunks.size() +
                               " for document: " + documentId);

                } catch (Exception e) {
                    failedCount++;
                    logger.warning("Failed to process chunk " + chunk.getChunkIndex() +
                                 " for document " + documentId + ": " + e.getMessage() +
                                 " (continuing with best effort)");
                    // Continue processing other chunks (best effort strategy)
                }
            }

            // Stage 6: Update final status
            if (successCount == 0) {
                // All chunks failed - mark document as FAILED
                String errorMsg = "Failed to process all " + chunks.size() + " chunks";
                updateDocumentStatus(documentId, DocumentStatus.FAILED, errorMsg);
                logger.severe("Document processing failed completely: " + documentId);
            } else {
                // At least some chunks succeeded - mark as READY
                updateDocumentStatus(documentId, DocumentStatus.READY, null);
                logger.info("Document processing completed: " + documentId +
                          " (success: " + successCount + "/" + chunks.size() +
                          ", failed: " + failedCount + ")");
            }

        } catch (Exception e) {
            // Fatal error during processing
            logger.severe("Document processing failed for " + documentId + ": " + e.getMessage());
            String errorMsg = "Processing failed: " + e.getMessage();
            updateDocumentStatus(documentId, DocumentStatus.FAILED, errorMsg);
        }
    }

    /**
     * Update document status and error message.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateDocumentStatus(String documentId, DocumentStatus status, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(status);
            doc.setErrorMessage(errorMessage);

            if (status == DocumentStatus.READY || status == DocumentStatus.FAILED) {
                doc.setProcessedAt(LocalDateTime.now());
            }

            documentRepository.save(doc);
            logger.info("Updated document " + documentId + " status to: " + status);
        });
    }

    /**
     * Update document total chunks count.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateDocumentTotalChunks(String documentId, int totalChunks) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setTotalChunks(totalChunks);
            documentRepository.save(doc);
        });
    }

    /**
     * Get processing progress for a document.
     *
     * @param documentId Document ID
     * @return Map with keys: totalChunks, processedChunks, progressPercentage
     */
    public Map<String, Object> getProcessingProgress(String documentId) {
        Map<String, Object> progress = new HashMap<>();

        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            progress.put("error", "Document not found");
            return progress;
        }

        int totalChunks = doc.getTotalChunks() != null ? doc.getTotalChunks() : 0;
        long processedChunks = chunkRepository.countByDocumentIdAndVectorIdIsNotNull(documentId);

        progress.put("totalChunks", totalChunks);
        progress.put("processedChunks", processedChunks);
        progress.put("progressPercentage", totalChunks > 0 ? (processedChunks * 100.0 / totalChunks) : 0.0);

        return progress;
    }
}
