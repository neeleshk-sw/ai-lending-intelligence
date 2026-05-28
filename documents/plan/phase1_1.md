# Flow 2 — RAG Query (Read / Inference Path)

## Overview

This is the AI inference path. Given a natural-language question and optional metadata filters, the system retrieves relevant document chunks from the vector store, assembles them into a prompt, and sends it to the local LLM (Ollama) to generate an answer. The result always includes source evidence and a confidence score so human underwriters can verify the output.

**Entry point:** `RagEngine.execute(RagRequest)` in `ai-core/src/main/java/com/ailending/aicore/rag/RagEngine.java`

---

## Step-by-Step Flow

### 1. Availability Guard

```
RagEngine.execute(RagRequest)
  └─ llmProvider.isAvailable() → throws IllegalStateException if Ollama is down
```

Fails fast with a human-readable message before doing any work. Callers (underwriters, UI) receive a clear signal to review documents manually rather than getting a silent failure.

---

### 2. Retrieval — `RetrievalEngine.retrieve()`

`RagEngine` builds a `RetrievalRequest` from the incoming `RagRequest`:

| Field | Source |
|---|---|
| `query` | `RagRequest.query` |
| `loanId` | `RagRequest.loanId` (optional filter) |
| `customerId` | `RagRequest.customerId` (optional filter) |
| `documentType` | `RagRequest.documentType` (optional filter) |
| `policyVersion` | `RagRequest.policyVersion` (optional filter) |
| `topK` | Hard-coded to `5` |

`RetrievalEngine` converts this into a `SearchRequest` and delegates to `VectorStore.search()`.

---

### 3. Vector Search — `SpringVectorStoreWrapper.search()`

**File:** `ai-core/src/main/java/com/ailending/aicore/vectorstore/SpringVectorStoreWrapper.java`

1. Builds a Spring AI `SearchRequest` with the query text and `topK`.
2. For each non-null filter field (`loanId`, `customerId`, `documentType`, `policyVersion`), creates a `FilterExpressionBuilder.eq()` predicate.
3. ANDs all predicates together into a single compound filter expression.
4. Calls `delegate.similaritySearch(springRequest)` — this hits pgvector and returns the nearest chunks by embedding distance.
5. Converts pgvector's `distance` metadata into a `similarityScore` via:
   ```
   similarityScore = clamp(1.0 - distance, 0.0, 1.0)
   ```
6. Maps each Spring AI `Document` back to a domain `VectorDocument` (restoring typed metadata fields) and wraps it in a `SearchResult`.

Returns: `List<SearchResult>` — up to 5 hits, each carrying the chunk text, metadata, and similarity score.

---

### 4. Context Assembly

```java
hits.stream()
    .map(h -> "[Doc: " + h.getDocument().getId() + "] " + h.getDocument().getContent())
    .collect(Collectors.joining("\n\n"))
```

The top-K chunks are concatenated into a single `contextText` string that is injected into the prompt.

---

### 5. Prompt Assembly — `PromptBuilder.buildPrompt()`

**File:** `ai-core/src/main/java/com/ailending/aicore/prompt/PromptBuilder.java`

`RagEngine` holds four named `PromptTemplate` instances:

| Template name | Use case | Notes |
|---|---|---|
| `default` | General Q&A about a loan | Falls back to this if the requested template is unknown |
| `loan-summary` | Structured JSON borrower summary | Produces `borrowerProfile`, `monthlyIncome`, `riskIndicators`, `manualReviewRecommended` |
| `underwriting` | Detailed underwriting observations | References specific context pieces for risk/compliance flags |
| `policy-qa` | Lending policy interpretation | Requires citation of policy section or version |

`PromptBuilder.buildPrompt()` does three things:
1. Copies all variables from `PromptContext` into a local map.
2. Injects `{context}` = assembled chunk text.
3. Calls `template.render(variables)` — replaces `{placeholder}` tokens in the template string.

---

### 6. LLM Generation — `OllamaLlmProvider.generate()`

**File:** `ai-core/src/main/java/com/ailending/aicore/llm/OllamaLlmProvider.java`

`LlmRequest` is built with:
- `prompt` = final rendered string from step 5
- `temperature`:
  - `0.1` for `loan-summary` (deterministic JSON output)
  - `0.7` for all other templates (more expressive prose)

Returns `LlmResponse` carrying: generated text, model name, token count, and duration.

---

### 7. Confidence Score Calculation

```
confidenceScore = avg(similarityScore for each hit), clamped to [0, 1]
```

A heuristic: higher average similarity between the query embedding and the retrieved chunks implies the LLM had more relevant context to work from. This score is surfaced in `RagResponse` so underwriters can weigh the answer's reliability.

---

### 8. Response — `RagResponse`

| Field | Content |
|---|---|
| `generatedText` | LLM answer |
| `hits` | `List<SearchResult>` — the source evidence chunks |
| `model` | Ollama model name used |
| `confidenceScore` | Heuristic score in [0, 1] |
| `totalDurationMs` | End-to-end wall-clock time |

Per NFR-5, all four required fields (source references, confidence indicator, retrieved evidence metadata, generation timestamp) are present in `RagResponse`.

---

## Sequence Diagram

```
Caller
  │
  ▼
RagEngine.execute(RagRequest)
  │
  ├─[guard]─► OllamaLlmProvider.isAvailable()
  │
  ├─► RetrievalEngine.retrieve(RetrievalRequest)
  │       └─► SpringVectorStoreWrapper.search(SearchRequest)
  │               └─► pgvector similaritySearch  ──► top-5 VectorDocuments
  │
  ├─► [assemble contextText from hits]
  │
  ├─► PromptBuilder.buildPrompt(template, context, contextText)
  │       └─► PromptTemplate.render(variables)  ──► finalPrompt
  │
  ├─► OllamaLlmProvider.generate(LlmRequest)
  │       └─► Ollama HTTP (local)  ──► LlmResponse
  │
  └─► [calculateConfidence(hits)]  ──► RagResponse
```

---

## Key Constraints

- **AI is advisory only.** `RagResponse` is an input to human underwriter review, never an autonomous decision (ADR-003).
- **No external AI calls.** All inference runs through Ollama on org-controlled infrastructure (NFR-7, ADR-006).
- **SLA targets:** Retrieval < 2 s, AI summarization < 8 s, Policy Q&A < 5 s (NFR-1).
