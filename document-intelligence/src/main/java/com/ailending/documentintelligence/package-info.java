/**
 * Advanced document understanding module (Phase 3).
 *
 * <p>Extends the basic ingestion pipeline in the {@code ingestion} module with structured
 * extraction capabilities for complex document layouts:
 * <ul>
 *   <li>Table extraction from bank statements and balance sheets</li>
 *   <li>Form field extraction from loan applications and Form 16</li>
 *   <li>Signature and stamp detection for authenticity checks</li>
 *   <li>Multi-column and multi-page layout analysis</li>
 * </ul>
 *
 * <p>Outputs structured metadata alongside raw text so downstream components
 * (rule-engine, audit) can reason over typed values rather than free text.
 *
 * <h2>Planned package structure</h2>
 * <pre>
 *   com.ailending.documentintelligence
 *     ├── extraction/  — TableExtractor, FormFieldExtractor
 *     ├── model/       — ExtractedTable, FormField, DocumentLayout
 *     └── service/     — DocumentIntelligenceService
 * </pre>
 *
 * <h2>Key dependency</h2>
 * <ul>
 *   <li>{@code ingestion} — base document parsing and chunking pipeline</li>
 * </ul>
 *
 * <p><b>Not yet implemented.</b> Scaffold only.
 */
package com.ailending.documentintelligence;
