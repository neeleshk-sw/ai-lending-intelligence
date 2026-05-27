package com.ailending.ingestion.service;

import com.ailending.common.enums.DocumentStatus;

/**
 * Output DTO from the document ingestion pipeline.
 */
public class IngestionResult {

    private final String sourceDocumentId;
    private final int chunkCount;
    private final DocumentStatus status;
    private final long durationMs;

    public IngestionResult(String sourceDocumentId, int chunkCount, DocumentStatus status, long durationMs) {
        this.sourceDocumentId = sourceDocumentId;
        this.chunkCount = chunkCount;
        this.status = status;
        this.durationMs = durationMs;
    }

    public String getSourceDocumentId() { return sourceDocumentId; }
    public int getChunkCount() { return chunkCount; }
    public DocumentStatus getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
}
