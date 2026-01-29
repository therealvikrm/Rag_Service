-- =====================================================
-- KnowItAll Database Schema (H2 Compatible)
-- Initialized automatically on application startup
-- =====================================================

-- Documents Table
-- Stores metadata about uploaded documents
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    description CLOB,
    uploaded_at TIMESTAMP NOT NULL,
    owner VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_chunks INT NOT NULL DEFAULT 0,
    file_size_bytes BIGINT,
    error_message CLOB,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for documents table
CREATE INDEX IF NOT EXISTS idx_owner ON documents(owner);
CREATE INDEX IF NOT EXISTS idx_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_uploaded_at ON documents(uploaded_at);

-- Document Chunks Table
-- Stores individual chunks of documents with their embeddings
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    content CLOB NOT NULL,
    chunk_index INT NOT NULL,
    vector_id VARCHAR(255),
    metadata JSON,
    token_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create indexes for document_chunks table
CREATE INDEX IF NOT EXISTS idx_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_vector_id ON document_chunks(vector_id);
CREATE INDEX IF NOT EXISTS idx_chunk_index ON document_chunks(document_id, chunk_index);

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
