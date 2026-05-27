package com.ailending.app.controller.dto.ingestion;

import com.ailending.ingestion.service.IngestionResult;

/**
 * HTTP response body for {@code POST /api/lending/ingest}.
 */
public class IngestionResponse {

    private final String sourceDocumentId;
    private final int chunkCount;
    private final String status;
    private final long durationMs;

    private IngestionResponse(String sourceDocumentId, int chunkCount, String status, long durationMs) {
        this.sourceDocumentId = sourceDocumentId;
        this.chunkCount       = chunkCount;
        this.status           = status;
        this.durationMs       = durationMs;
    }

    public static IngestionResponse from(IngestionResult result) {
        return new IngestionResponse(
                result.getSourceDocumentId(),
                result.getChunkCount(),
                result.getStatus().name(),
                result.getDurationMs()
        );
    }

    public String getSourceDocumentId() { return sourceDocumentId; }
    public int    getChunkCount()       { return chunkCount; }
    public String getStatus()           { return status; }
    public long   getDurationMs()       { return durationMs; }
}
