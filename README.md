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
