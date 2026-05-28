# Phase 3 Implementation Plan — ALIM Backend

## Context

Phase 1 (ai-core, ingestion, lending, app) and Phase 2 (audit, rule-engine, policy, workflow) are complete with 195 passing tests. The three remaining scaffold-only modules are Phase 3:

- `document-intelligence/` — structured extraction (tables, form fields) on top of the ingestion pipeline
- `integration/` — port-and-adapter connectors for external systems (CBS, CRM, Credit Bureau, eKYC, Notification)
- `orchestration/` — top-level composition layer that chains Phase 2/3 services into end-to-end pipelines

No Kafka or real external systems are required. All implementations will be in-memory/stub, testable without infrastructure — same pattern as Phases 1 and 2.

---

## Build Order

```
1. document-intelligence/   (depends on: ingestion)
2. integration/             (depends on: common)
3. orchestration/           (depends on: workflow, rule-engine, audit, policy,
                                         document-intelligence, integration)
```

---

## Module 1: `document-intelligence/` — `com.ailending.documentintelligence`

### Why
The base ingestion pipeline (Tika → plain text → chunks) loses structured information from tables and key-value form fields. This module adds a second pass over parsed text to extract typed values that downstream components (rule-engine, audit) can reason over.

### New files

**`model/FormField.java`** — value object: `fieldName`, `rawValue`, `confidence`

**`model/ExtractedTable.java`** — value object: `headers` (List<String>), `rows` (List<List<String>>)

**`model/DocumentLayout.java`** — value object: `formFields`, `tables`; `static DocumentLayout empty()`

**`extraction/FormFieldExtractor.java`** — interface: `List<FormField> extract(String text)`

**`extraction/RegexFormFieldExtractor.java`** — `@Component`
- Matches `"Key: Value"` patterns (confidence 0.9) and `"Key — Value"` (confidence 0.7) line-by-line

**`extraction/TableExtractor.java`** — interface: `List<ExtractedTable> extract(String text)`

**`extraction/PipeTableExtractor.java`** — `@Component`
- Detects pipe-delimited (`|`) table blocks; parses header + separator + data rows
- Skips rows with wrong column count

**`service/DocumentIntelligenceService.java`** — interface: `DocumentLayout analyze(String parsedText, String documentType)`

**`service/DocumentIntelligenceServiceImpl.java`** — `@Service`; merges results from both extractors

**`exception/DocumentIntelligenceException.java`** — extends `BaseException`

### Tests
- `RegexFormFieldExtractorTest`: colon pair (0.9), dash pair (0.7), no separator ignored, multiple pairs
- `PipeTableExtractorTest`: valid pipe table, mismatched columns skipped, no table → empty
- `DocumentIntelligenceServiceImplTest`: both extractors called, blank text → empty layout, exception wrapped

---

## Module 2: `integration/` — `com.ailending.integration`

### Why
Establishes port interfaces (domain contracts) and in-memory stub adapters for 5 external systems. Real HTTP adapters added in the infrastructure testing phase.

### Connectors

| Connector | Port | Adapter | Key behavior |
|---|---|---|---|
| CBS | `CoreBankingPort` | `CoreBankingAdapter` | `accountExists`→true; `disburseLoan` records IDs |
| CRM | `CrmPort` | `CrmAdapter` | returns synthetic `CustomerProfile` |
| Credit Bureau | `CreditBureauPort` | `CibilAdapter` | returns score 720 |
| eKYC | `EkycPort` | `AadhaarEkycAdapter` | blank Aadhaar→exception; valid→verified=true |
| Notification | `NotificationPort` | `InMemoryNotificationAdapter` | stores sent notifications; `getSent()` for test inspection |

Supporting value objects: `CustomerProfile`, `CreditReport`, `EkycResult`, `NotificationRequest`, `NotificationChannel` enum.

**`exception/IntegrationException.java`** — extends `BaseException`

### Tests — one per adapter (5 test classes)

---

## Module 3: `orchestration/` — `com.ailending.orchestration`

### Why
The only module that sees all Phase 2/3 services. Assembles two end-to-end pipelines; placeholder for future Kafka event bus wiring.

### Loan Submission Pipeline
**`pipeline/LoanSubmissionResult.java`** — `Loan loan`, `RuleResult ruleResult`, `String auditId`, `Instant processedAt`

**`pipeline/LoanSubmissionPipeline.java`** — interface

**`pipeline/LoanSubmissionPipelineImpl.java`** — `@Service`
1. `WorkflowService.submitLoan` → Loan
2. `RuleEvaluationService.evaluate` → RuleResult
3. `AuditService.record` → AuditRecord
4. Returns `LoanSubmissionResult`

### Document Processing Pipeline
**`pipeline/DocumentProcessingResult.java`** — `IngestionResult`, `DocumentLayout`, `String auditId`, `Instant processedAt`

**`pipeline/DocumentProcessingPipeline.java`** — interface

**`pipeline/DocumentProcessingPipelineImpl.java`** — `@Service`
1. `DocumentIngestor.ingest` → IngestionResult
2. Tika text extraction from request bytes
3. `DocumentIntelligenceService.analyze` → DocumentLayout
4. `AuditService.record`
5. Returns `DocumentProcessingResult`

**`config/OrchestrationConfig.java`** — `@Configuration` (placeholder for event bus)

**`exception/OrchestrationException.java`** — extends `BaseException`

### Tests
- `LoanSubmissionPipelineImplTest`: all 3 services called; WorkflowException → OrchestrationException; auditId non-null
- `DocumentProcessingPipelineImplTest`: all services called; zero-chunk result still runs document-intelligence

---

## REST API additions (`app/`)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/lending/orchestration/loans` | Full loan submission pipeline |
| `POST` | `/api/lending/orchestration/documents` | Full document processing pipeline (multipart) |

Add `app/pom.xml` dependency: `orchestration` module.

---

## Verification

```bash
mvn clean install -pl document-intelligence,integration,orchestration -am -DskipTests
mvn test -pl document-intelligence
mvn test -pl integration
mvn test -pl orchestration
mvn clean install   # full build, all tests
```

Expected: all tests green with no DB, LLM, or external systems.
