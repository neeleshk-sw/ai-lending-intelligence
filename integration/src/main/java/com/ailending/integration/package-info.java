/**
 * External system connectors module (Phase 3).
 *
 * <p>Provides adapter interfaces and implementations for integrating the ALIM platform
 * with external systems. All connectors follow the hexagonal-architecture "port-and-adapter"
 * pattern: domain code depends on interfaces (ports), concrete HTTP/gRPC/JDBC clients
 * live here (adapters).
 *
 * <p>Planned connectors:
 * <ul>
 *   <li><b>CBS</b> — Core Banking System (loan disbursement, account lookup)</li>
 *   <li><b>CRM</b> — Customer Relationship Management (borrower profile enrichment)</li>
 *   <li><b>Credit Bureau</b> — CIBIL / Experian score retrieval</li>
 *   <li><b>eKYC</b> — Aadhaar-based identity verification</li>
 *   <li><b>Notification</b> — Email / SMS / push alerts for workflow events</li>
 * </ul>
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.integration
 *     ├── cbs/         — CoreBankingPort, CoreBankingAdapter
 *     ├── crm/         — CrmPort, CrmAdapter
 *     ├── creditbureau/ — CreditBureauPort, CibilAdapter
 *     └── notification/ — NotificationPort, EmailAdapter, SmsAdapter
 * </pre>
 *
 * <h2>Key dependency</h2>
 * <ul>
 *   <li>{@code common} — shared DTOs and {@code BaseException}</li>
 * </ul>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.integration;
