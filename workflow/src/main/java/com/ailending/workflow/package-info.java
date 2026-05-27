/**
 * Loan lifecycle orchestration module (Phase 2).
 *
 * <p>Coordinates the end-to-end loan workflow using a durable process engine
 * (Camunda BPM or Temporal). Each step in the FR-8 state machine
 * ({@code AI_REVIEW_PENDING → AI_REVIEW_COMPLETED → UNDERWRITER_REVIEW_PENDING
 * → MANUAL_OVERRIDE / FINAL_DECISION_COMPLETED}) is modelled as a process activity,
 * enabling retries, timeouts, and human-task assignment.
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.workflow
 *     ├── process/     — BPMN process definitions and task delegates
 *     ├── service/     — WorkflowService, TaskAssignmentService
 *     └── event/       — Domain event listeners (LoanSubmitted, AiReviewCompleted, …)
 * </pre>
 *
 * <h2>Key dependency</h2>
 * <ul>
 *   <li>{@code lending} — {@code Loan}, {@code LoanService}, {@code LoanStatus}</li>
 * </ul>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.workflow;
