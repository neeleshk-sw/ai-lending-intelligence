# Phase 2 Implementation Plan — ALIM Backend

## Context

Phase 1 delivered the core RAG infrastructure (`ai-core`), document ingestion pipeline (`ingestion`), in-memory loan domain (`lending`), and REST API (`app`). All Phase 2 modules (`audit/`, `rule-engine/`, `policy/`, `workflow/`) exist as Maven-registered stubs containing only `package-info.java`.

Phase 2 fills those stubs with real implementations following Phase 1 conventions: Builder-pattern DTOs, interface/impl separation, constructor injection, module-scoped exceptions extending `BaseException`, and unit tests using Mockito + `standaloneSetup` (no real DB or LLM required).

---

## Modules and Build Order

Implement in this order to respect the dependency graph:

```
1. audit/          (depends on: ai-core, common)
2. rule-engine/    (depends on: lending)
3. policy/         (depends on: ai-core)
4. workflow/       (depends on: lending)
```

---

## Module 1: `audit/`  — `com.ailending.audit`

### Why
Regulatory and explainability requirement (NFR-5): every AI recommendation and every human decision must be captured in an immutable, queryable audit trail.

### New files

**`model/AuditEventType.java`** — enum
```
AI_REVIEW_COMPLETED, STATUS_TRANSITION, UNDERWRITER_DECISION, MANUAL_OVERRIDE
```

**`model/AuditRecord.java`** — immutable value object, Builder pattern
| Field | Type | Notes |
|---|---|---|
| `auditId` | `String` | UUID generated in builder |
| `loanId` | `String` | required |
| `customerId` | `String` | required |
| `eventType` | `AuditEventType` | required |
| `timestamp` | `Instant` | defaults to `Instant.now()` |
| `performedBy` | `String` | "system" or underwriter ID |
| `aiGeneratedText` | `String` | nullable; from `RagResponse.generatedText` |
| `confidenceScore` | `Double` | nullable |
| `modelUsed` | `String` | nullable |
| `aiDurationMs` | `Long` | nullable |
| `sourceReferenceCount` | `Integer` | nullable |
| `decision` | `String` | nullable; underwriter's final decision text |
| `notes` | `String` | nullable |

**`service/AuditService.java`** — interface
```java
void record(AuditRecord record);
List<AuditRecord> findByLoanId(String loanId);
List<AuditRecord> findByCustomerId(String customerId);
```

**`service/AuditServiceImpl.java`** — `@Service`
- `CopyOnWriteArrayList<AuditRecord>` (append-only, thread-safe)
- `findByLoanId` / `findByCustomerId` filter via stream

**`exception/AuditException.java`** — extends `BaseException`

### Tests — `AuditServiceImplTest`
- `record()` stores and `findByLoanId()` retrieves correctly
- Multiple events for same loanId returned in insertion order
- `findByCustomerId` returns correct subset; empty list for unknown ID
- `record()` with null loanId throws `AuditException`
- Records are immutable (verify via Builder isolation)

---

## Module 2: `rule-engine/`  — `com.ailending.ruleengine`

### Why
Deterministic, auditable rules complement AI analysis. They catch hard compliance failures (low credit score, failed KYC) that don't require language model reasoning.

### New files

**`model/RuleInput.java`** — immutable, Builder
| Field | Type |
|---|---|
| `loanId` | `String` |
| `customerId` | `String` |
| `monthlyIncome` | `BigDecimal` |
| `requestedAmount` | `BigDecimal` |
| `existingMonthlyDebt` | `BigDecimal` |
| `creditScore` | `int` |
| `kycVerified` | `boolean` |
| `amlClear` | `boolean` |

**`model/RuleResult.java`** — value object
```java
String loanId, boolean passed, List<String> violations, Instant evaluatedAt
```

**`rule/Rule.java`** — interface
```java
Optional<String> evaluate(RuleInput input);   // empty = pass, message = violation
String ruleName();
```

**`rule/DebtToIncomeRule.java`**
- Estimated EMI = `requestedAmount × 0.02` (2 % monthly approximation)
- Violation if `(existingMonthlyDebt + estimatedEMI) / monthlyIncome > 0.45`

**`rule/MinimumCreditScoreRule.java`** — violation if `creditScore < 650`

**`rule/KycVerificationRule.java`** — violation if `!kycVerified`

**`rule/AmlComplianceRule.java`** — violation if `!amlClear`

**`service/RuleEvaluationService.java`** — interface
```java
RuleResult evaluate(RuleInput input);
```

**`service/RuleEvaluationServiceImpl.java`** — `@Service`
- Constructor receives `List<Rule>` (Spring collects all `Rule` beans automatically)
- Runs all rules, collects violations; `passed = violations.isEmpty()`

**`exception/RuleEvaluationException.java`** — extends `BaseException`

### Tests
**`rule/DebtToIncomeRuleTest`** — passes at 0.45, fails at 0.46; boundary cases

**`rule/MinimumCreditScoreRuleTest`** — passes at 650, fails at 649

**`rule/KycVerificationRuleTest`** / **`AmlComplianceRuleTest`** — boolean flip

**`service/RuleEvaluationServiceImplTest`**
- All rules pass → `passed=true`, `violations` empty
- One rule fails → `passed=false`, one violation message
- Multiple failures → all violation messages collected
- Null `RuleInput` → `RuleEvaluationException`

---

## Module 3: `policy/`  — `com.ailending.policy`

### Why
Policy documents must be versioned and retrievable independently from loan documents. The existing RAG pipeline already supports `policyVersion` as a metadata filter; this module manages that lifecycle.

### Design note
`policy/pom.xml` depends on `ai-core` only (not `ingestion`). Therefore `PolicyIngestorService` accepts pre-extracted text (no Tika parsing) and does its own simple paragraph-based chunking. Full document parsing will be added when `document-intelligence/` is implemented in Phase 3.

### New files

**`model/PolicyVersion.java`** — value object
```java
String version, String policyName, Instant ingestedAt, int chunkCount
```

**`service/PolicyIngestorService.java`** — interface
```java
// textContent is already-extracted plain text; no file parsing in Phase 2
PolicyVersion ingest(String policyName, String version, String textContent);
```

**`service/PolicyIngestorServiceImpl.java`** — `@Service`
- Splits `textContent` on blank lines; merges short segments so each chunk is ≤ 500 words
- Embeds each chunk via `EmbeddingProvider`; builds `VectorDocument` with:
  - `documentType = "POLICY_DOCUMENT"`
  - `policyVersion = version`
  - `sourceDocumentId = policyName`
  - `chunkSequence` = sequential index
- Stores via `VectorStore.store()`
- Returns `PolicyVersion` with `chunkCount`

**`service/PolicyQueryService.java`** — interface
```java
RagResponse query(String question, String policyVersion);
```

**`service/PolicyQueryServiceImpl.java`** — `@Service`
- Builds `RagRequest` with `documentType = "POLICY_DOCUMENT"`, `policyVersion` filter, `promptTemplateName = "policy-qa"`
- Delegates to `RagEngine.execute()`

**`exception/PolicyException.java`** — extends `BaseException`

### Tests
**`PolicyIngestorServiceImplTest`**
- Multi-paragraph text produces correct chunk count
- `EmbeddingProvider.embed()` called once per chunk
- `VectorStore.store()` called with `documentType=POLICY_DOCUMENT` and correct `policyVersion`
- Returns `PolicyVersion` with matching `chunkCount`
- Blank/null `textContent` throws `PolicyException`

**`PolicyQueryServiceImplTest`**
- `RagEngine.execute()` called with `documentType=POLICY_DOCUMENT`, correct `policyVersion`, template `policy-qa`
- Returns `RagResponse` from engine unchanged

---

## Module 4: `workflow/`  — `com.ailending.workflow`

### Why
`LoanServiceImpl` currently holds loans in a `ConcurrentHashMap` with no event history. The workflow module adds an event log per loan (foundation for future Kafka/Camunda integration) and a richer orchestration API that wraps `LoanService`.

### New files

**`event/WorkflowEvent.java`** — abstract base
```java
String loanId, String customerId, Instant occurredAt
```

**Concrete events** (each extends `WorkflowEvent`):
| Class | Extra fields |
|---|---|
| `LoanSubmittedEvent` | `BigDecimal amount`, `String currency` |
| `AiReviewCompletedEvent` | `double confidenceScore`, `String modelUsed` |
| `UnderwriterAssignedEvent` | `String underwriterId` |
| `DecisionRecordedEvent` | `LoanStatus finalStatus`, `String reason` |

**`service/WorkflowService.java`** — interface
```java
Loan submitLoan(String customerId, BigDecimal amount, String currency);
Loan completeAiReview(String loanId, double confidenceScore, String modelUsed);
Loan assignUnderwriter(String loanId, String underwriterId);
Loan recordDecision(String loanId, LoanStatus finalStatus, String reason);
List<WorkflowEvent> getHistory(String loanId);
```

**`service/WorkflowServiceImpl.java`** — `@Service`
- Constructor receives `LoanService`
- `ConcurrentHashMap<String, List<WorkflowEvent>>` for in-memory event history
- Each method: delegates state transition to `LoanService`, appends event to history
- Wraps `LoanNotFoundException` / `InvalidStatusTransitionException` in `WorkflowException`

**`exception/WorkflowException.java`** — extends `BaseException`

### Tests — `WorkflowServiceImplTest`
- `submitLoan` → LoanService.createLoan called; `LoanSubmittedEvent` recorded; returned Loan has `AI_REVIEW_PENDING`
- `completeAiReview` → LoanService.updateStatus called with `AI_REVIEW_COMPLETED`; `AiReviewCompletedEvent` recorded
- `assignUnderwriter` → transitions to `UNDERWRITER_REVIEW_PENDING`; event recorded
- `recordDecision(FINAL_DECISION_COMPLETED)` → terminal state; event recorded
- `recordDecision(MANUAL_OVERRIDE)` → terminal state; event recorded
- `getHistory` returns events in insertion order
- Unknown loanId → `WorkflowException` (wraps `LoanNotFoundException`)
- Illegal transition → `WorkflowException` (wraps `InvalidStatusTransitionException`)

---

## Cross-cutting conventions (same as Phase 1)

- All DTOs: immutable, Builder with `build()`-time validation, no Lombok
- `@Service` on impls; interfaces plain Java
- Constructor injection with `final` fields
- Module-scoped exceptions: `extends BaseException`, message includes context (`loanId`, etc.)
- Tests: no `@SpringBootTest`; mocks via Mockito for interfaces; direct `new` for value objects; `ArgumentCaptor` to verify forwarding

---

## REST API additions (in `app/`)

Wire the four new services into existing or new controllers:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/lending/audit` | Record an audit event |
| `GET` | `/api/lending/audit/{loanId}` | Fetch audit trail for a loan |
| `POST` | `/api/lending/rules/evaluate` | Run rule evaluation for a loan |
| `POST` | `/api/lending/policies/ingest` | Ingest a policy document (text body) |
| `POST` | `/api/lending/policies/query` | Query a versioned policy via RAG |
| `POST` | `/api/lending/workflow/loans` | Submit a loan via workflow |
| `PATCH` | `/api/lending/workflow/loans/{loanId}/ai-review` | Mark AI review complete |
| `PATCH` | `/api/lending/workflow/loans/{loanId}/underwriter` | Assign underwriter |
| `PATCH` | `/api/lending/workflow/loans/{loanId}/decision` | Record final decision |
| `GET` | `/api/lending/workflow/loans/{loanId}/history` | Get event history |

Add `app/pom.xml` dependencies: `audit`, `rule-engine`, `policy`, `workflow` modules.

---

## Verification

```bash
# Build Phase 2 modules only (fast)
mvn clean install -pl audit,rule-engine,policy,workflow -am -DskipTests

# Run all Phase 2 tests
mvn test -pl audit
mvn test -pl rule-engine
mvn test -pl policy
mvn test -pl workflow

# Full build with all tests
mvn clean install

# Run a single test class
mvn test -pl rule-engine -Dtest=DebtToIncomeRuleTest
```

Expected: all tests green with no real DB or LLM running.
