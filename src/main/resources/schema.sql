-- =====================================================
-- KnowItAll Database Schema
-- Initialized automatically on application startup
-- =====================================================

-- Documents Table
-- Stores metadata about uploaded documents
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID of the document',
    title VARCHAR(255) NOT NULL COMMENT 'User-friendly title',
    filename VARCHAR(255) NOT NULL COMMENT 'Original uploaded filename',
    description LONGTEXT COMMENT 'Brief description of document',
    uploaded_at TIMESTAMP NOT NULL COMMENT 'When document was uploaded',
    owner VARCHAR(255) NOT NULL COMMENT 'Email/UserID of uploader',
    status VARCHAR(20) NOT NULL COMMENT 'UPLOADING, PROCESSING, READY, FAILED',
    total_chunks INT NOT NULL DEFAULT 0 COMMENT 'Number of chunks document was split into',
    file_size_bytes BIGINT COMMENT 'File size in bytes',
    error_message LONGTEXT COMMENT 'Error message if processing failed',
    processed_at TIMESTAMP COMMENT 'When processing completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_owner (owner) COMMENT 'For filtering documents by owner',
    KEY idx_status (status) COMMENT 'For filtering by processing status',
    KEY idx_uploaded_at (uploaded_at) COMMENT 'For sorting by upload date'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Stores metadata for uploaded documents';

-- Document Chunks Table
-- Stores individual chunks of documents with their embeddings
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID of chunk',
    document_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to documents table',
    content LONGTEXT NOT NULL COMMENT 'Text content of chunk',
    chunk_index INT NOT NULL COMMENT 'Sequential index of chunk (0-based)',
    vector_id VARCHAR(255) COMMENT 'ID of vector in Qdrant',
    metadata JSON COMMENT 'JSON metadata: {page, section, offset, etc}',
    token_count INT COMMENT 'Number of tokens in chunk',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,

    KEY idx_document_id (document_id) COMMENT 'For fetching chunks by document',
    KEY idx_vector_id (vector_id) COMMENT 'For tracking vectors in store',
    KEY idx_chunk_index (document_id, chunk_index) COMMENT 'For ordering chunks'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Stores document chunks with vector references';

-- =====================================================
-- Performance Indexes Summary
-- =====================================================
-- documents:
--   - idx_owner: Fast lookup of user's documents
--   - idx_status: Fast filtering by processing status
--   - idx_uploaded_at: Fast sorting/filtering by date
--
-- document_chunks:
--   - idx_document_id: Fast retrieval of all chunks for a document
--   - idx_vector_id: Fast tracking of vector embeddings
--   - idx_chunk_index: Fast retrieval in document order
--
-- Foreign Keys:
--   - ON DELETE CASCADE: Automatically deletes chunks when document is deleted
-- =====================================================
