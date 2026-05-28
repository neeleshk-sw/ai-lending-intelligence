package com.ailending.app.controller.dto.orchestration;

import com.ailending.orchestration.pipeline.DocumentProcessingResult;

import java.time.Instant;

public class DocumentProcessingResponse {
    private final int chunkCount;
    private final String ingestionStatus;
    private final int formFieldCount;
    private final int tableCount;
    private final String auditId;
    private final Instant processedAt;

    private DocumentProcessingResponse(DocumentProcessingResult r) {
        this.chunkCount      = r.getIngestionResult().getChunkCount();
        this.ingestionStatus = r.getIngestionResult().getStatus().name();
        this.formFieldCount  = r.getDocumentLayout().getFormFields().size();
        this.tableCount      = r.getDocumentLayout().getTables().size();
        this.auditId         = r.getAuditId();
        this.processedAt     = r.getProcessedAt();
    }

    public static DocumentProcessingResponse from(DocumentProcessingResult r) {
        return new DocumentProcessingResponse(r);
    }

    public int getChunkCount()        { return chunkCount; }
    public String getIngestionStatus(){ return ingestionStatus; }
    public int getFormFieldCount()    { return formFieldCount; }
    public int getTableCount()        { return tableCount; }
    public String getAuditId()        { return auditId; }
    public Instant getProcessedAt()   { return processedAt; }
}
