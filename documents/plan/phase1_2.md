# Flow 3 — Remaining Development Tasks & Gap Analysis

## Overview

This document captures the gap between the `implementation_plan.md` / architecture design and the **current state of the codebase** as of the analysis date. It serves as the authoritative task list for all remaining development work.

Two reference documents were used:
- `/implementation_plan.md` (root) — Architecture design for `ai-core`: strict zero-Spring, zero-lending-domain constraints
- `documents/plan/implementation_plan.md` — Full roadmap: component specs + verification plan

---

## Current Development State

### ✅ Fully Implemented

| Module | What's Done |
|---|---|
| `ai-core` | All 6 packages: `llm`, `embedding`, `vectorstore`, `retrieval`, `prompt`, `rag` — all interfaces, DTOs, and Ollama/pgvector implementations |
| `ingestion` | Full pipeline: `TikaDocumentParser` → `OverlapChunker` → `OllamaEmbeddingProvider` → `SpringVectorStoreWrapper` |
| `common` | `DocumentStatus` enum, `BaseException` |
| `app` | `AiLendingApplication`, `HealthController`, `LendingController` (hello-world stubs only) |

### ⚠️ Partially Done

| Item | State |
|---|---|
| Unit tests | `PromptBuilderTest`, `OverlapChunkerTest`, `DocumentIngestorImplTest` exist — integration tests and several unit tests are missing |
| `lending` module | `Loan`, `LoanService`, `RiskAnalysisService` are empty stubs |
| REST API | No functional endpoints beyond `/health` and `/api/lending/helloWorld` |

---

## Remaining Tasks

### Task 1 — Fix Architecture Violations in `ai-core`

**Priority: HIGH — Blocks clean testing and violates ADR**

The architecture document mandates that `ai-core` has **zero Spring Framework dependencies**. Provider implementations must live in `app` (or a dedicated `ai-providers` sub-package within `app`). Three classes currently violate this:

| Class | Current Location | Required Location |
|---|---|---|
| `OllamaLlmProvider` | `ai-core/.../llm/` | `app/.../providers/llm/` |
| `OllamaEmbeddingProvider` | `ai-core/.../embedding/` | `app/.../providers/embedding/` |
| `SpringVectorStoreWrapper` | `ai-core/.../vectorstore/` | `app/.../providers/vectorstore/` |

**Steps:**
1. Create package `com.ailending.app.providers` in the `app` module.
2. Move all three classes into this package.
3. Remove `spring-ai-ollama-spring-boot-starter` and `spring-ai-pgvector-store-spring-boot-starter` from `ai-core/pom.xml` — these starters must only be in `app/pom.xml`.
4. Update `@ComponentScan` in `AiLendingApplication` to pick up the new provider package.
5. Verify: `mvn dependency:tree -pl ai-core` must show no Spring AI or Spring Framework imports.

**Files to change:**
- `ai-core/pom.xml` — remove Spring AI starters
- `app/pom.xml` — add Spring AI starters here if not already present
- Move: `OllamaLlmProvider.java`, `OllamaEmbeddingProvider.java`, `SpringVectorStoreWrapper.java`
- `app/src/main/java/com/ailending/app/AiLendingApplication.java`

---

### Task 2 — Fix `VectorStore` Interface: Remove Domain Leak

**Priority: HIGH — Architecture boundary violation**

`VectorStore.deleteByLoanId(String loanId)` introduces a lending-domain concept (`loanId`) into `ai-core`, which must be domain-agnostic.

**Replace with:**
```java
// VectorStore.java — in ai-core
void deleteByMetadata(String key, String value);
```

Callers in `lending` or `app` pass `("loanId", loanId)` — the generic key stays clean.

**Files to change:**
- `ai-core/src/main/java/com/ailending/aicore/vectorstore/VectorStore.java`
- `ai-core/src/main/java/com/ailending/aicore/vectorstore/SpringVectorStoreWrapper.java` (update impl, after Task 1 moves it to `app`)
- Any callers of `deleteByLoanId` in `ingestion` or `lending`

---

### Task 3 — Complete `EmbeddingProvider` Interface

**Priority: MEDIUM**

The architecture doc specifies three methods. Current implementation only has `embed(EmbeddingRequest)`. Two are missing:

```java
// EmbeddingProvider.java — add these:
List<EmbeddingResult> embed(List<EmbeddingRequest> requests);  // batch for ingestion efficiency
int getDimensions();                                            // vector size compatibility check
```

**Files to change:**
- `ai-core/src/main/java/com/ailending/aicore/embedding/EmbeddingProvider.java`
- `OllamaEmbeddingProvider.java` (after Task 1 moves it to `app`) — add implementations

---

### Task 4 — Missing Tests (Complete the Verification Plan)

**Priority: MEDIUM**

The verification plan in `implementation_plan.md` lists tests that have not been written yet.

#### 4a. `LlmRequestTest`
**File:** `ai-core/src/test/java/com/ailending/aicore/llm/LlmRequestTest.java`

- Test fluent builder sets all fields correctly
- Test that missing required fields (`prompt`) throw `IllegalArgumentException`
- Test default values for `temperature` and `maxTokens`

#### 4b. `SpringVectorStoreWrapperTest`
**File:** `app/src/test/java/com/ailending/app/providers/vectorstore/SpringVectorStoreWrapperTest.java` (after Task 1)

- Test that a single metadata filter builds the correct Spring AI `Filter.Expression`
- Test that multiple filters are ANDed together correctly
- Test that null filter fields are omitted (no null-key predicates)
- Mock the Spring AI `VectorStore` delegate

#### 4c. In-Memory `VectorStore` for Unit Testing
**File:** `ai-core/src/test/java/com/ailending/aicore/vectorstore/InMemoryVectorStore.java`

An in-memory `VectorStore` implementation (test-scope only) that stores `VectorDocument` objects in a `List` and performs brute-force cosine similarity. Used by:
- `RetrievalEngineTest` — test retrieval with mock embedder + in-memory store
- `RagEngineTest` — test full RAG orchestration with no external services

#### 4d. `RagEngineTest`
**File:** `ai-core/src/test/java/com/ailending/aicore/rag/RagEngineTest.java`

- Mock `RetrievalEngine` to return fixed `SearchResult` hits
- Mock `LlmProvider` to return a canned `LlmResponse`
- Verify `RagResponse` fields: `generatedText`, `hits`, `confidenceScore`, `totalDurationMs`
- Test graceful degradation: when `llmProvider.isAvailable()` returns `false`, expect `IllegalStateException`

#### 4e. Testcontainers pgvector Integration Test
**File:** `app/src/test/java/com/ailending/app/integration/PgVectorIntegrationTest.java`

- Spin up a Postgres + pgvector container using Testcontainers
- Store 5 `VectorDocument` objects with varied metadata
- Run a `SearchRequest` with a metadata filter and verify only matching documents are returned
- Run a similarity search and verify ranking by score

**Dependency to add to `app/pom.xml`:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

### Task 5 — Implement `lending` Module Business Logic

**Priority: MEDIUM**

All three classes in `lending` are empty stubs. These must be implemented to support the loan workflow (FR-8) and risk analysis (FR-3, FR-4).

#### 5a. `Loan` Domain Entity
**File:** `lending/src/main/java/com/ailending/lending/domain/Loan.java`

Fields required:
```
String loanId          — UUID, primary identifier
String customerId      — links to borrower
BigDecimal amount      — requested loan amount
String currency        — ISO 4217
LoanStatus status      — workflow state (FR-8): AI_REVIEW_PENDING → AI_REVIEW_COMPLETED
                                                → UNDERWRITER_REVIEW_PENDING
                                                → MANUAL_OVERRIDE / FINAL_DECISION_COMPLETED
Instant submittedAt
Instant updatedAt
String assignedUnderwriter  — nullable; set when status = UNDERWRITER_REVIEW_PENDING
```

#### 5b. `LoanService` Implementation
**File:** `lending/src/main/java/com/ailending/lending/service/LoanServiceImpl.java`

Methods:
```
Loan createLoan(String customerId, BigDecimal amount, String currency)
    → Creates loan with status AI_REVIEW_PENDING, persists (in-memory map for now)

Loan getLoan(String loanId)
    → Retrieves by ID, throws LoanNotFoundException if missing

Loan updateStatus(String loanId, LoanStatus newStatus)
    → Validates state transition is legal per FR-8 workflow, throws if invalid
```

Use an in-memory `Map<String, Loan>` for storage initially (database persistence is a Phase 2 concern).

#### 5c. `RiskAnalysisService` Implementation
**File:** `lending/src/main/java/com/ailending/lending/service/RiskAnalysisServiceImpl.java`

Depends on `RagEngine` (injected). Method:
```
RagResponse analyzeRisk(String loanId, String customerId)
    → Builds RagRequest with:
         query            = "Provide an underwriting risk assessment for this loan application."
         loanId           = loanId
         customerId       = customerId
         templateName     = "underwriting"
    → Calls RagEngine.execute(request)
    → Returns RagResponse (advisory only — does NOT update loan status)
```

---

### Task 6 — REST API Endpoints

**Priority: MEDIUM — Required for manual verification from the plan**

Expose the following endpoints in `LendingController` (or split into focused controllers):

#### 6a. Document Ingestion Endpoint
```
POST /api/lending/ingest
Content-Type: multipart/form-data

Parameters:
  file          — document bytes (PDF, DOCX, etc.)
  loanId        — String
  customerId    — String
  documentType  — String (one of: LOAN_APPLICATION, SALARY_SLIP, BANK_STATEMENT, ...)
  sourceDocumentId — String (caller-assigned ID)
  policyVersion — String (optional)

Response 200:
{
  "sourceDocumentId": "...",
  "chunkCount": 12,
  "status": "COMPLETED",
  "durationMs": 1240
}
```

Delegates to `DocumentIngestor.ingest(IngestionRequest)`.

#### 6b. RAG Query Endpoint
```
POST /api/lending/query
Content-Type: application/json

Body:
{
  "query": "What is the monthly income of the borrower?",
  "loanId": "loan-001",
  "customerId": "cust-001",
  "documentType": "SALARY_SLIP",   // optional filter
  "templateName": "default"        // optional; defaults to "default"
}

Response 200:
{
  "generatedText": "...",
  "confidenceScore": 0.82,
  "model": "llama3",
  "totalDurationMs": 3450,
  "hits": [
    { "content": "...", "similarityScore": 0.91, "documentType": "SALARY_SLIP" }
  ]
}
```

Delegates to `RagEngine.execute(RagRequest)`.

#### 6c. Loan CRUD Endpoints
```
POST /api/lending/loans
Body: { "customerId": "...", "amount": 50000, "currency": "INR" }
Response 201: Loan object with generated loanId

GET /api/lending/loans/{loanId}
Response 200: Loan object
Response 404: if not found

PATCH /api/lending/loans/{loanId}/status
Body: { "status": "AI_REVIEW_COMPLETED" }
Response 200: Updated Loan object
Response 400: If state transition is invalid
```

Delegates to `LoanService`.

---

### Task 7 — Move Business Prompt Templates to `lending`

**Priority: LOW — Clean architecture, not blocking**

`RagEngine` (in `ai-core`) currently hard-codes 4 prompt templates: `default`, `loan-summary`, `underwriting`, `policy-qa`. The architecture doc states:

> Business prompt templates belong in `lending`, not in `ai-core`.

**Steps:**
1. Remove the template initialization block from `RagEngine`'s `@PostConstruct`.
2. Add a `registerTemplate(String name, PromptTemplate template)` method to `RagEngine`.
3. Create a `PromptTemplateConfig` class in `lending` (or `app`) that registers all 4 templates on startup by calling `ragEngine.registerTemplate(...)`.
4. This keeps `ai-core` template-agnostic and lets `lending` own its prompts.

**Files to change:**
- `ai-core/src/main/java/com/ailending/aicore/rag/RagEngine.java`
- New: `lending/src/main/java/com/ailending/lending/config/PromptTemplateConfig.java`

---

### Task 8 — Testcontainers pgvector Integration Test

*(See Task 4e above — listed separately for tracking as it is a larger standalone effort)*

**Priority: LOW — Full end-to-end verification**

---

### Task 9 — Phase 2/3 Module Scaffolding (Future)

**Priority: LOW — Per TDD roadmap, not blocking Phase 1**

The following Maven modules are defined in the TDD but not yet created. New feature work should go here rather than extending `lending/` or `app/`:

| Module | Purpose | Key Dependency |
|---|---|---|
| `workflow/` | Loan lifecycle orchestration (Camunda or Temporal) | `lending` |
| `rule-engine/` | Business rule evaluation (Drools) | `lending` |
| `audit/` | Immutable audit trail of all AI decisions | `ai-core`, `common` |
| `policy/` | Policy document versioning + retrieval | `ai-core` |
| `orchestration/` | Cross-module process coordination | All |
| `document-intelligence/` | Advanced document understanding (tables, forms) | `ingestion` |
| `integration/` | External system connectors (CBS, CRM) | `common` |

Scaffold each as an empty Maven module with the correct parent POM reference and package structure before implementation begins.

---

## Execution Order Summary

| # | Task | Effort | Priority | Unblocks |
|---|---|---|---|---|
| 1 | Move Ollama providers + wrapper to `app` | M | HIGH | Tasks 2, 4b, 4e |
| 2 | Fix `VectorStore.deleteByLoanId` → `deleteByMetadata` | S | HIGH | Task 4b |
| 3 | Add batch embed + `getDimensions()` to `EmbeddingProvider` | S | MEDIUM | Task 4c |
| 4a | `LlmRequestTest` | S | MEDIUM | — |
| 4b | `SpringVectorStoreWrapperTest` | M | MEDIUM | — |
| 4c | In-memory `VectorStore` + `RagEngineTest` | M | MEDIUM | Task 4d |
| 4d | `RagEngineTest` | S | MEDIUM | — |
| 5a | `Loan` entity | S | MEDIUM | Tasks 5b, 5c, 6c |
| 5b | `LoanService` implementation | M | MEDIUM | Task 6c |
| 5c | `RiskAnalysisService` implementation | S | MEDIUM | Task 6b |
| 6a | Ingest endpoint | S | MEDIUM | Manual verification |
| 6b | RAG query endpoint | S | MEDIUM | Manual verification |
| 6c | Loan CRUD endpoints | M | MEDIUM | Manual verification |
| 7 | Move prompt templates to `lending` | S | LOW | — |
| 4e | Testcontainers pgvector integration test | L | LOW | — |
| 9 | Phase 2/3 module scaffolding | L | LOW | Future phases |

> **Effort key:** S = Small (< 2h), M = Medium (2–4h), L = Large (4h+)

---

## Verification Checklist (End-to-End)

Once all tasks are complete, verify in this order:

1. `mvn dependency:tree -pl ai-core` — confirm **no Spring imports** in `ai-core`
2. `mvn test` — all unit tests pass (including new ones from Task 4)
3. `mvn spring-boot:run -pl app` — application starts without errors
4. `POST /api/lending/ingest` with a real PDF → response contains `chunkCount > 0` and `status: COMPLETED`
5. `POST /api/lending/query` with a question matching ingested content → response contains `generatedText` and `confidenceScore > 0`
6. `POST /api/lending/loans` → returns loan with `status: AI_REVIEW_PENDING`
7. `PATCH /api/lending/loans/{id}/status` with `AI_REVIEW_COMPLETED` → successful transition
8. `PATCH /api/lending/loans/{id}/status` with an illegal transition → returns `400 Bad Request`
9. Run Testcontainers integration test → metadata-filtered vector search returns only matching documents

---

## Key Constraints (carry forward to all tasks)

- **AI is advisory only.** No code path may autonomously approve or reject loans (ADR-003).
- **No external AI calls.** All LLM and embedding inference must use Ollama on org-controlled infrastructure (NFR-7, ADR-006).
- **Every `RagResponse` must include** source references, confidence indicator, retrieved evidence metadata, and generation timestamp (NFR-5). Do not strip these fields.
- **SLA targets (NFR-1):** Document ingestion < 5s, semantic retrieval < 2s, AI summarization < 8s, policy Q&A < 5s.
