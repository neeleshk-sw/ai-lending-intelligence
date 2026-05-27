/**
 * Policy document versioning and retrieval module (Phase 2).
 *
 * <p>Manages the lifecycle of lending policy documents: ingestion with version tagging,
 * version-aware retrieval via the {@code policyVersion} metadata filter, and a query
 * interface that maps to the {@code policy-qa} RAG prompt template.
 *
 * <p>Policy documents are stored as vector chunks in pgvector with the
 * {@code policyVersion} metadata field (e.g., {@code "v3.0"}) so queries can be
 * scoped to the applicable policy version in force at a given time.
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.policy
 *     ├── model/       — PolicyDocument, PolicyVersion
 *     ├── service/     — PolicyIngestorService, PolicyQueryService
 *     └── repository/  — PolicyVersionRepository
 * </pre>
 *
 * <h2>Key dependency</h2>
 * <ul>
 *   <li>{@code ai-core} — {@code RagEngine}, {@code VectorStore}, {@code EmbeddingProvider}</li>
 * </ul>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.policy;
