# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all modules
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Build a specific module
mvn clean install -pl ai-core -am

# Run the application (from the app module)
mvn spring-boot:run -pl app

# Run all tests
mvn test

# Run tests in a specific module
mvn test -pl ai-core

# Run a single test class
mvn test -pl ai-core -Dtest=PromptBuilderTest

# Run a single test method
mvn test -pl ai-core -Dtest=PromptBuilderTest#testBuildPrompt
```

## Project Context

This is **ALIM** — the AI-Assisted Lending Intelligence Module. It is an AI augmentation layer for an enterprise lending platform. Critical constraints from the FRD/TDD:

- **AI is advisory only.** The system must never autonomously approve or reject loans. All final decisions are made by human underwriters. Any code path that bypasses human review violates the core governance model (ADR-003).
- **No external AI calls.** All LLM inference and embedding generation must run within organization-controlled infrastructure (Ollama/vLLM/TGI). Sending customer or loan data to a public AI provider (OpenAI, Anthropic, etc.) violates regulatory and confidentiality requirements (NFR-7, ADR-006).
- **Every AI response must include** source references, a confidence indicator, retrieved evidence metadata, and a generation timestamp (NFR-5). Do not strip these from `RagResponse`.

## Architecture

This is a Java 17 / Spring Boot 3.3 multi-module Maven project. The stack is **Spring AI 1.0.0-M1** (milestone) + **Ollama** (local LLM and embeddings) + **pgvector** (PostgreSQL vector store).

### Module dependency graph

```
common  ←─────────────────────────── (no Spring, no business logic)
   ↑
ai-core  ←──── ingestion
   ↑               ↑
   └───────────────┤
                lending
                   ↑
                  app  (Spring Boot entry point + REST controllers)
```

### Module responsibilities

| Module | Package root | Role |
|---|---|---|
| `common` | `com.ailending.common` | Shared DTOs, enums (`DocumentStatus`), and exceptions. No Spring or business logic. |
| `ai-core` | `com.ailending.aicore` | RAG engine, LLM integration, embeddings, vector store, retrieval, and prompt building. Core infrastructure for AI features. |
| `ingestion` | `com.ailending.ingestion` | Document parsing, chunking, and embedding preparation. Depends on `ai-core` to store vectors. |
| `lending` | `com.ailending.lending` | Business use cases: `LoanService`, `RiskAnalysisService`, domain entity `Loan`. Depends on `ai-core` for RAG queries. |
| `app` | `com.ailending.app` | Spring Boot application entry point (`AiLendingApplication`), REST controllers (`LendingController` at `/api/lending`). |

### RAG pipeline (`ai-core`)

The end-to-end Retrieval-Augmented Generation flow is orchestrated by `RagEngine`:

1. **Retrieval** — `RetrievalEngine` builds a `SearchRequest` with metadata filters (`loanId`, `customerId`, `documentType`, `policyVersion`) and fetches top-K chunks from `VectorStore`.
2. **Vector Store** — `SpringVectorStoreWrapper` wraps Spring AI's `VectorStore` (backed by pgvector). Metadata filters are built with Spring AI's `FilterExpressionBuilder` and ANDed together. Distance is converted to similarity score via `1 - distance`.
3. **Prompt Assembly** — `PromptBuilder` injects the retrieved context and query variables into a `PromptTemplate`. Templates use `{variable}` placeholders and are rendered via `PromptTemplate.render(Map)`. `RagEngine` holds four named templates: `default`, `loan-summary`, `underwriting`, `policy-qa`.
4. **LLM** — `OllamaLlmProvider` calls Spring AI's `ChatModel` with `OllamaOptions`. Temperature is set to `0.1` for `loan-summary` (JSON output) and `0.7` for all other templates. Returns `LlmResponse` with generated text, model name, token count, and duration.
5. **Embeddings** — `OllamaEmbeddingProvider` wraps Spring AI's `EmbeddingModel` for generating vector embeddings during ingestion.

### Key design conventions

- `LlmProvider`, `EmbeddingProvider`, and `VectorStore` are interfaces — swap implementations without touching callers.
- `RagEngine.execute()` performs a graceful-degradation check (`llmProvider.isAvailable()`) before doing any work.
- `RagRequest` carries optional metadata filters that flow through to `RetrievalRequest` and then to `SearchRequest` for pgvector filtering.
- Confidence score in `RagResponse` is a heuristic: average similarity score across the top-K hits, clamped to [0, 1].

### Infrastructure dependencies (required at runtime)

- **Ollama** — local LLM server for both chat and embeddings (default Spring AI auto-configuration applies).
- **PostgreSQL with pgvector extension** — vector store backend via `spring-ai-pgvector-store-spring-boot-starter`.

### Domain model

**Supported document types** (use these exact string values for `documentType` metadata):
`LOAN_APPLICATION`, `SALARY_SLIP`, `BANK_STATEMENT`, `FORM_16`, `ID_PROOF`, `ADDRESS_PROOF`, `POLICY_DOCUMENT`, `UNDERWRITER_NOTE`

**Loan workflow states** (FR-8):
`AI_REVIEW_PENDING` → `AI_REVIEW_COMPLETED` → `UNDERWRITER_REVIEW_PENDING` → `MANUAL_OVERRIDE` / `FINAL_DECISION_COMPLETED`

**Domain events** (Kafka, event-driven — not yet implemented):
`LoanSubmitted`, `DocumentUploaded`, `DocumentIngested`, `EmbeddingGenerated`, `AiReviewRequested`, `AiReviewCompleted`, `ManualReviewRequired`, `UnderwriterApproved`, `LoanRejected`

**Vector document metadata** — every stored chunk must carry: `loanId`, `customerId`, `documentType`, `sourceDocumentId`, `ingestionTimestamp`, `chunkSequence`, `policyVersion`.

### Performance SLAs (NFR-1)

| Operation | Target |
|---|---|
| Document ingestion | < 5 seconds |
| Semantic retrieval | < 2 seconds |
| AI summarization | < 8 seconds |
| Policy Q&A | < 5 seconds |

### Planned expanded modules (not yet created)

The TDD defines these additional modules for Phase 2/3: `workflow/` (Camunda/Temporal), `rule-engine/` (Drools), `audit/`, `policy/`, `orchestration/`, `document-intelligence/`, `integration/`. New feature work should go into one of these future modules rather than bloating `lending/` or `app/`.

### Reference documents

`documents/files/` contains `FRD.pdf` (Functional Requirements) and `TDD.pdf` (Technical Design Document) for this project.
