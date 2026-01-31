package com.genai.knowitall.controller;

import com.genai.knowitall.controller.dto.DocumentStatusResponse;
import com.genai.knowitall.controller.dto.DocumentUploadResponse;
import com.genai.knowitall.model.Document;
import com.genai.knowitall.model.DocumentStatus;
import com.genai.knowitall.repository.DocumentRepository;
import com.genai.knowitall.service.DocumentIngestionService;
import com.genai.knowitall.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * REST controller for document upload and status tracking.
 * Handles endpoints for uploading documents, checking processing status,
 * listing all documents, and deleting documents.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = Logger.getLogger(DocumentController.class.getName());

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final DocumentIngestionService ingestionService;

    @Value("${doc.max.file.size.bytes:10485760}")
    private long maxFileSizeBytes;

    @Value("${doc.allowed.file.types}")
    private String allowedFileTypes;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public DocumentController(
            DocumentRepository documentRepository,
            TextExtractionService textExtractionService,
            DocumentIngestionService ingestionService) {

        this.documentRepository = documentRepository;
        this.textExtractionService = textExtractionService;
        this.ingestionService = ingestionService;
    }

    /**
     * Upload a document for processing.
     * @param file Multipart file to upload
     * @param title file name
     * @param description description of the document
     * @param owner owner of the document
     * @return ResponseEntity with upload result
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "owner", defaultValue = "anonymous") String owner) {

        logger.info("Received upload request: " + file.getOriginalFilename() +
                   " (size: " + file.getSize() + " bytes)");

        try {
            // Validate file
            validateFile(file);

            // Generate document ID
            String documentId = UUID.randomUUID().toString();
            String filename = file.getOriginalFilename();
            String documentTitle = (title != null && !title.isEmpty()) ? title : filename;

            // Create document entity
            Document document = Document.builder()
                    .id(documentId)
                    .title(documentTitle)
                    .filename(filename)
                    .description(description)
                    .owner(owner)
                    .status(DocumentStatus.UPLOADING)
                    .uploadedAt(LocalDateTime.now())
                    .fileSizeBytes(file.getSize())
                    .totalChunks(0)
                    .build();

            // Save document with UPLOADING status
            documentRepository.save(document);
            logger.info("Created document record: " + documentId);

            // Extract text synchronously (fast operation)
            String textContent;
            try {
                textContent = textExtractionService.extractText(
                        file.getInputStream(),
                        file.getContentType(),
                        filename
                );
                logger.info("Extracted " + textContent.length() + " characters from " + filename);
            } catch (Exception e) {
                // Update document status to FAILED
                document.setStatus(DocumentStatus.FAILED);
                document.setErrorMessage("Text extraction failed: " + e.getMessage());
                documentRepository.save(document);

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("error", "Failed to extract text from document: " + e.getMessage()));
            }

            // Start async processing (chunking + embedding + vector storage)
            ingestionService.processDocumentAsync(documentId, textContent);

            // Return response
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                    .documentId(documentId)
                    .filename(filename)
                    .title(documentTitle)
                    .status(DocumentStatus.PROCESSING)
                    .uploadedAt(document.getUploadedAt())
                    .fileSizeBytes(file.getSize())
                    .message("Document uploaded successfully. Processing in background.")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warning("File validation failed: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.severe("Document upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload document: " + e.getMessage()));
        }
    }

    /**
     * Get document processing status.
     *
     * @param id Document ID
     * @return DocumentStatusResponse with processing progress
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable String id) {
        logger.info("Status check for document: " + id);

        return documentRepository.findById(id)
                .map(doc -> {
                    Map<String, Object> progress = ingestionService.getProcessingProgress(id);

                    DocumentStatusResponse response = DocumentStatusResponse.builder()
                            .documentId(doc.getId())
                            .title(doc.getTitle())
                            .filename(doc.getFilename())
                            .status(doc.getStatus())
                            .totalChunks((Integer) progress.getOrDefault("totalChunks", 0))
                            .processedChunks(((Long) progress.getOrDefault("processedChunks", 0L)).intValue())
                            .progressPercentage((Double) progress.getOrDefault("progressPercentage", 0.0))
                            .errorMessage(doc.getErrorMessage())
                            .uploadedAt(doc.getUploadedAt())
                            .processedAt(doc.getProcessedAt())
                            .message(getStatusMessage(doc.getStatus()))
                            .build();

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all documents.
     */
    @GetMapping
    public ResponseEntity<?> listDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }

    /**
     * Delete a document and its chunks.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        logger.info("Delete request for document: " + id);

        return documentRepository.findById(id)
                .map(doc -> {
                    documentRepository.delete(doc);
                    logger.info("Deleted document: " + id);
                    return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Helper Methods ====================

    /**
     * Validate uploaded file (type and size).
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed: " + (maxFileSizeBytes / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: PDF, DOCX. Received: " + contentType);
        }
    }

    /**
     * Get user-friendly status message.
     */
    private String getStatusMessage(DocumentStatus status) {
        return switch (status) {
            case UPLOADING -> "Document is being uploaded...";
            case PROCESSING -> "Processing document (extracting, chunking, embedding)...";
            case READY -> "Document is ready for queries";
            case FAILED -> "Document processing failed";
        };
    }
}
