/**
 * Cross-module process coordination module (Phase 3).
 *
 * <p>The top-level coordinator that composes the Phase 2/3 modules into cohesive
 * end-to-end lending processes. This module owns no business logic — it is purely
 * a composition and routing layer, wiring together:
 *
 * <ul>
 *   <li>{@code workflow}              — process engine steps</li>
 *   <li>{@code rule-engine}           — deterministic rule evaluation</li>
 *   <li>{@code audit}                 — decision trail recording</li>
 *   <li>{@code policy}                — policy-aware retrieval</li>
 *   <li>{@code document-intelligence} — structured document extraction</li>
 *   <li>{@code integration}           — external system calls</li>
 * </ul>
 *
 * <p>Entry points into this module are driven by domain events (e.g.,
 * {@code LoanSubmitted}, {@code DocumentUploaded}) published over the event bus
 * (Kafka — planned, not yet implemented).
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.orchestration
 *     ├── handler/     — Domain event handlers
 *     ├── pipeline/    — Assembled processing pipelines
 *     └── config/      — Spring wiring for cross-module beans
 * </pre>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.orchestration;
