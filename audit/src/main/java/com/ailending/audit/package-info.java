/**
 * Immutable audit trail module (Phase 2).
 *
 * <p>Records every AI recommendation together with the human decision that followed,
 * producing an append-only, tamper-evident log that satisfies regulatory explainability
 * and traceability requirements.
 *
 * <p>Each audit record captures:
 * <ul>
 *   <li>The {@code loanId} and {@code customerId}</li>
 *   <li>The full {@code RagResponse} (generated text, confidence score, source references,
 *       model used, duration) — satisfying NFR-5</li>
 *   <li>The underwriter's final decision and timestamp</li>
 *   <li>An immutable event sequence number and wall-clock timestamp</li>
 * </ul>
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.audit
 *     ├── model/       — AuditRecord, AuditEvent
 *     ├── service/     — AuditService, AuditEventPublisher
 *     └── repository/  — AuditRepository (append-only store)
 * </pre>
 *
 * <h2>Key dependencies</h2>
 * <ul>
 *   <li>{@code ai-core} — {@code RagResponse}, {@code SearchResult}</li>
 *   <li>{@code common}  — {@code BaseException}, shared enums</li>
 * </ul>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.audit;
