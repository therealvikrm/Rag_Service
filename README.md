# KnowItAll

_AI-powered document search (RAG) backend built with Java/Spring Boot._

## Overview
KnowItAll lets users ask natural-language questions over ingested documents and receive grounded, explainable answers. It retrieves relevant context from a vector database, constrains the LLM to that context, and returns responses with sources.

## Problem Statement
- LLMs lack private/domain-specific knowledge.
- They can hallucinate or be outdated.
- Pure-generation is costly and inefficient without retrieval.

KnowItAll addresses these by retrieving semantic context first, restricting answers to retrieved content, and exposing source references.

## What It Does
1) Ingests documents into a semantic knowledge base.  
2) Converts content to embeddings and stores them in a vector DB.  
3) Retrieves the most relevant chunks for a query.  
4) Builds context and invokes an LLM constrained to that context.  
5) Returns answers with the source citations used.

## Core Concepts
- Retrieval-Augmented Generation (RAG)
- Vector similarity search
- Text embeddings, chunking, and context construction
- LLM orchestration with guardrails
- Explainability and confidence-based responses

## High-Level Architecture
```
Documents --> Chunking --> Embeddings --> Vector DB
      ^                                 |
      |                                 v
User Query -> Embedding -> Retrieval -> Context -> LLM -> Answer + Sources
```

## Detailed Flow Diagrams

### Flow 1: Document Ingestion
```
┌─────────────────────────────────────────────────────────────────────────┐
│                    DOCUMENT INGESTION FLOW                              │
└─────────────────────────────────────────────────────────────────────────┘

1. CLIENT REQUEST
   ├─ Endpoint: POST /api/documents/upload
   ├─ Controller: DocumentController.uploadDocument()
   └─ Payload:
      {
        "file": <multipart/form-data PDF file>,
        "title": "Document",
        "description": "Documentation for review",
        "owner": "vikramaditya"
      }

2. DOCUMENT CONTROLLER
   ├─ Validates file (PDF, size check)
   ├─ Creates Document entity with metadata
   ├─ Saves to database (status: PROCESSING)
   └─ Calls: DocumentIngestionService.ingestDocumentAsync()
      Returns: 201 Created with DocumentId

3. DOCUMENT INGESTION SERVICE (Async/Background)
   ├─ Extracts text from PDF
   │  └─ Calls: TextExtractionService.extractText()
   │
   ├─ Chunks document into smaller pieces
   │  └─ Calls: DocumentChunkingService.chunkDocument()
   │     └─ Splits by token count (200 tokens per chunk)
   │     └─ Creates DocumentChunk objects with metadata
   │
   ├─ For each chunk:
   │  ├─ Generate embedding vector
   │  │  └─ Calls: EmbeddingService.generateEmbedding(chunkText)
   │  │     └─ Calls OpenAI API (text-embedding-3-small)
   │  │     └─ Returns: 1536-dimensional vector
   │  │
   │  ├─ Store embedding in Qdrant
   │  │  └─ Calls: QdrantVectorStore.storeEmbedding()
   │  │     └─ Payload: {id, embedding[], metadata}
   │  │     └─ Stores in Qdrant collection: "knowitall_docs"
   │  │
   │  └─ Store chunk metadata in PostgreSQL
   │     └─ Calls: DocumentChunkRepository.save()
   │        └─ Saves: chunk_id, document_id, content, tokens, vector_id
   │
   └─ Update document status to READY

4. DATA PERSISTENCE
   ├─ PostgreSQL Database
   │  └─ Tables: documents, document_chunks
   │     └─ Stores: document metadata, chunk text, references to vectors
   │
   └─ Qdrant Vector Database
      └─ Collection: "knowitall_docs"
         └─ Stores: vector embeddings, chunk references, metadata
```

### Flow 2: Query (RAG) Flow
```
┌─────────────────────────────────────────────────────────────────────────┐
│                    QUERY / RAG FLOW (End-to-End)                        │
└─────────────────────────────────────────────────────────────────────────┘

1. CLIENT REQUEST
   ├─ Endpoint: POST /api/query
   ├─ Controller: QueryController.query()
   └─ Payload:
      {
         "question": "What are the benefits of AI in healthcare?",
         "topK": 5,
         "confidenceThreshold": 0.7,
         "documentFilter": null
     }

2. QUERY CONTROLLER
   ├─ Validates query input
   └─ Calls: RAGService.queryDocuments()

3. RAG SERVICE - RETRIEVAL PHASE
   ├─ Convert question to embedding
   │  └─ Calls: EmbeddingService.generateEmbedding(question)
   │     └─ Calls OpenAI API (text-embedding-3-small)
   │     └─ Returns: 1536-dimensional vector
   │
   ├─ Search Qdrant for similar vectors
   │  └─ Calls: QdrantVectorStore.search(questionEmbedding, topK=5)
   │     └─ Performs vector similarity search (cosine distance)
   │     └─ Returns top 5 most similar chunks with scores
   │
   ├─ Retrieve full chunk content from PostgreSQL
   │  └─ Calls: DocumentChunkRepository.findByVectorIds()
   │     └─ Returns chunk text + metadata (document_id, title, etc)
   │
   └─ Filter by confidence threshold
      └─ Removes chunks with similarity score < 0.7

4. RAG SERVICE - GENERATION PHASE
   ├─ Build context from retrieved chunks
   │  └─ Concatenate top relevant chunks with sources
   │
   ├─ Invoke LLM with context
   │  └─ Calls: LLMProvider.generateAnswer()
   │     └─ Model: gpt-3.5-turbo
   │     └─ System prompt: "Answer based on provided context only"
   │     └─ Payload:
   │        {
   │          "messages": [
   │            {"role": "system", "content": "Context: <chunks>"},
   │            {"role": "user", "content": "What is the technical experience?"}
   │          ],
   │          "temperature": 0.7,
   │          "max_tokens": 512
   │        }
   │     └─ Returns: Generated answer text
   │
   └─ Compile response with sources

5. RESPONSE RETURNED TO CLIENT
   ├─ Status: 200 OK
   └─ Payload:
      {
         "answer": "AI in healthcare provides multiple benefits including...",
         "confidence": 0.92,
         "isGrounded": true,
         "sources": [
         {
         "documentId": "doc-123",
         "documentTitle": "AI in Healthcare",
         "chunkIndex": 5,
         "similarity": 0.94,
         "excerpt": "AI models can detect diseases early..."
         }
         ],
         "chunksRetrieved": 3,
         "retrievalTimeMs": 45,
         "generationTimeMs": 1200,
         "totalTimeMs": 1245
     }
```

### Data Flow Summary
```
INGESTION:
  User PDF → DocumentController → DocumentIngestionService → 
  TextExtractionService → DocumentChunkingService → 
  EmbeddingService (OpenAI) → QdrantVectorStore + DocumentRepository

QUERY:
  User Question → QueryController → RAGService → 
  EmbeddingService (OpenAI) → QdrantVectorStore (vector search) → 
  DocumentRepository (chunk retrieval) → LLMProvider (OpenAI) → 
  Answer with Sources
```

## Key Features
- Document ingestion with chunking + metadata
- Semantic search via vector similarity
- Confidence gating (no answer if context is weak)
- Explainable responses with document references
- Cost-aware design (retrieve before generate)
- Backend-first architecture (UI optional)

## User Flow (Example)
1) User ingests documents.  
2) User asks: "What are the limitations of vector databases?"  
3) KnowItAll finds relevant sections, builds curated context, invokes LLM, returns answer with sources.

## Non-Goals
- Training or fine-tuning models
- Acting as a general-purpose chatbot
- Browser automation
- Relying on LLM "general knowledge" without retrieved context

## Why This Matters
- Demonstrates production-minded GenAI design
- Controls LLM behavior via retrieval and guardrails
- Emphasizes reliability, transparency, and trust

## Tech Stack
- Java 17
- Spring Boot
- LangChain4j
- Vector DB (Milvus or Qdrant)
- LLM provider (OpenAI / Azure OpenAI)
- Object storage for document persistence

## Learning Outcomes
Hands-on practice with RAG system design, vector DB integration, context management, guardrails to reduce hallucinations, and production-focused GenAI thinking.

## Future Enhancements
- Chrome extension client for guided navigation
- RAG evaluation and quality metrics
- Caching for frequent queries
- Role-based document access
- Streaming responses

## Project Philosophy
> The LLM is not the system. The system surrounds the LLM.
