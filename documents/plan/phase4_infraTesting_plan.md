# Phase 4 — Infrastructure Testing Plan

## Goal

Validate the full ALIM stack against real infrastructure: PostgreSQL with pgvector (vector store), and Ollama (LLM + embeddings). All previous phases use in-memory stubs and mocks. This phase ensures the wired-together system works end-to-end under real I/O conditions and meets NFR-1 performance SLAs.

---

## Setup Requirements

### 1. Docker & Docker Compose
**[MANUAL SETUP — must be installed on the developer machine]**

- Docker Desktop (Mac/Windows) or Docker Engine (Linux) — v24+
- Docker Compose v2 (`docker compose` command, not `docker-compose`)

Verify:
```bash
docker --version
docker compose version
```

### 2. PostgreSQL with pgvector
**[SETUP via Docker Compose — see `docker-compose.yml` below]**

- PostgreSQL 15+
- pgvector extension v0.5+ must be installed in the database
- Default config expected by Spring AI pgvector starter:
  - Host: `localhost`
  - Port: `5432`
  - Database: `ailending`
  - Username: `ailending`
  - Password: `ailending`

The `vector` extension must be enabled after container start:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
The Spring AI pgvector starter creates the `vector_store` table automatically on first run (via `initialize-schema: true`).

### 3. Ollama
**[MANUAL SETUP — must be installed and running locally]**

Ollama runs as a native process outside Docker (GPU passthrough in Docker is complex). It must be started separately before running integration tests.

- Download and install: https://ollama.com/download
- Default base URL expected by Spring AI: `http://localhost:11434`

Pull the required models:
```bash
# Chat model (used by RagEngine / LlmProvider)
ollama pull llama3.2

# Embedding model (used by EmbeddingProvider)
ollama pull nomic-embed-text
```

**[MANUALLY UPDATE `application-integration-test.properties`]** with the actual model names you pulled, if different from the defaults above.

Verify Ollama is running:
```bash
curl http://localhost:11434/api/tags
```

### 4. Java & Maven
**[MANUAL SETUP — already required for Phase 1–3]**

- Java 17+
- Maven 3.9+

---

## Docker Compose File

Create `docker/docker-compose.yml` in the project root:

```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg15
    container_name: ailending-postgres
    environment:
      POSTGRES_DB: ailending
      POSTGRES_USER: ailending
      POSTGRES_PASSWORD: ailending
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Start:
```bash
docker compose -f docker/docker-compose.yml up -d
```

Stop and remove data:
```bash
docker compose -f docker/docker-compose.yml down -v
```

---

## Spring Configuration

Create `app/src/test/resources/application-integration-test.properties`:

```properties
# PostgreSQL / pgvector
spring.datasource.url=jdbc:postgresql://localhost:5432/ailending
spring.datasource.username=ailending
spring.datasource.password=ailending
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.dimensions=768

# Ollama — update model names if you pulled different versions
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
```

**[MANUALLY UPDATE]** model names to match what you have pulled via `ollama list`.

Integration tests activate this profile with `@ActiveProfiles("integration-test")`.

---

## Test Strategy

Integration tests live in `app/src/test/java/.../integration/` and are tagged `@Tag("integration")` so they can be excluded from the default `mvn test` run (which must remain infrastructure-free).

Run integration tests explicitly:
```bash
mvn test -pl app -Dgroups=integration
```

Exclude integration tests from default build (add to root `pom.xml` surefire config):
```xml
<excludedGroups>integration</excludedGroups>
```

### Test class structure

All integration test classes:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — full application context
- `@ActiveProfiles("integration-test")`
- `@Tag("integration")`

---

## Test Coverage

### 1. Vector Store (`VectorStoreIT`)
Validates `SpringVectorStoreWrapper` against real pgvector.

- Store a `Document` with metadata and retrieve it by similarity search
- Metadata filter: retrieve by `loanId` — only matching documents returned
- Metadata filter: retrieve by `documentType=SALARY_SLIP` — type filtering works
- Store 50 documents; top-K retrieval returns K results with correct similarity ordering
- Distance-to-similarity conversion: returned scores are in [0, 1]

### 2. Embedding Generation (`EmbeddingProviderIT`)
Validates `OllamaEmbeddingProvider` against real Ollama.

- `embed(text)` returns a non-empty float array
- Vector dimension matches configured value (768 for `nomic-embed-text`)
- Two semantically similar texts produce cosine similarity > 0.8
- Two semantically unrelated texts produce cosine similarity < 0.5

### 3. LLM Response (`LlmProviderIT`)
Validates `OllamaLlmProvider` against real Ollama.

- `generate(prompt, "default")` returns a non-blank `LlmResponse`
- `LlmResponse` carries model name, token count > 0, duration > 0
- `isAvailable()` returns `true` when Ollama is running
- Temperature override: `loan-summary` template uses 0.1 (JSON-oriented output)

### 4. Full Ingestion Pipeline (`IngestionPipelineIT`)
Validates end-to-end: upload → parse → chunk → embed → store in pgvector.

- Upload a PDF salary slip; `IngestionResult` reports chunk count > 0
- Immediately query vector store for `loanId`; chunks are retrievable
- `ingestionTimestamp` metadata is set on stored chunks
- Ingestion of a 10-page PDF completes within **5 seconds** (NFR-1)

### 5. Full RAG Query (`RagQueryIT`)
Validates end-to-end: query → retrieve from pgvector → LLM answer.

- Ingest a policy document, then RAG query against it; answer is non-blank
- `RagResponse` includes: `answer`, `sources` (non-empty), `confidenceScore` ∈ [0,1], `generatedAt` (NFR-5)
- `promptTemplateName=policy-qa` is used for policy queries
- Query with no matching documents returns graceful response (no exception)
- Full RAG query (retrieval + LLM) completes within **8 seconds** (NFR-1)

### 6. Orchestration Pipeline (`LoanSubmissionPipelineIT`)
Validates the Phase 3 orchestration layer against real infrastructure.

- `POST /api/lending/orchestration/loans` with valid payload returns 201 + `loanId`
- Loan is persisted and retrievable via `GET /api/lending/loans/{loanId}`
- Rule violations are reflected in response (`rulesPassed=false` + violations list)
- Audit record is created for the submission event

### 7. Performance SLA Assertions (NFR-1)
Each test above asserts timing via `StopWatch` or `assertTimeout`:

| Operation | SLA |
|---|---|
| Document ingestion | < 5 000 ms |
| Semantic retrieval (top-K) | < 2 000 ms |
| AI summarization | < 8 000 ms |
| Policy Q&A | < 5 000 ms |

---

## Pre-Run Checklist

Before running integration tests, verify:

- [ ] Docker Desktop is running
- [ ] PostgreSQL container is up: `docker compose -f docker/docker-compose.yml up -d`
- [ ] pgvector extension enabled: `psql -h localhost -U ailending -d ailending -c "CREATE EXTENSION IF NOT EXISTS vector;"`
- [ ] Ollama is running: `curl http://localhost:11434/api/tags`
- [ ] Required models are pulled: `ollama list` — confirm `llama3.2` and `nomic-embed-text` present
- [ ] `application-integration-test.properties` model names match `ollama list` output

---

## Running Tests

```bash
# Start infrastructure
docker compose -f docker/docker-compose.yml up -d

# Run integration tests only
mvn test -pl app -Dgroups=integration

# Run a specific integration test
mvn test -pl app -Dgroups=integration -Dtest=RagQueryIT

# Run all tests (unit only — no infrastructure needed)
mvn test

# Full build including integration tests
mvn test -Dgroups=integration
```

---

## Future Phases (Noted)

The following items are identified but not yet planned. They will be scoped into separate phases:

| Item | Notes |
|---|---|
| **Domain Events / Kafka** | Producers/consumers for `LoanSubmitted`, `DocumentIngested`, `AiReviewCompleted`, etc. Scaffold exists in `workflow/` and `orchestration/`. |
| **Real Workflow Engine** | Replace in-memory `WorkflowServiceImpl` with Camunda or Temporal. `workflow/` module is the stub target. |
| **Real Rule Engine** | Replace hand-coded rules with Drools. `rule-engine/` module is the stub target. |
| **Real Integration Adapters** | HTTP/gRPC adapters for CBS, CRM (CIBIL), Aadhaar eKYC, SMS/email notification. Replace in-memory stubs in `integration/`. |
| **Persistent Audit Storage** | Persist `AuditRecord` to PostgreSQL instead of `CopyOnWriteArrayList`. |
| **Security** | Spring Security with JWT/OAuth2, role-based access (underwriter vs. admin). |
| **Observability** | Micrometer metrics, distributed tracing (Zipkin/Jaeger), structured logging. |
| **OpenAPI / Developer Experience** | Springdoc OpenAPI 3 spec, Bean Validation on request DTOs, standardized error response envelope. |
| **Multi-tenant isolation** | Vector store partitioning per customer/loan enforced at API layer. |
| **Model versioning** | Track which LLM model version produced each `RagResponse` for audit trail. |
| **Performance SLA enforcement** | Circuit breakers / timeouts wired to NFR-1 targets via Resilience4j. |
