package com.ailending.orchestration.pipeline;

import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.ingestion.service.IngestionResult;

import java.time.Instant;

public final class DocumentProcessingResult {

    private final IngestionResult ingestionResult;
    private final DocumentLayout documentLayout;
    private final String auditId;
    private final Instant processedAt;

    public DocumentProcessingResult(IngestionResult ingestionResult,
                                     DocumentLayout documentLayout,
                                     String auditId,
                                     Instant processedAt) {
        this.ingestionResult = ingestionResult;
        this.documentLayout  = documentLayout;
        this.auditId         = auditId;
        this.processedAt     = processedAt;
    }

    public IngestionResult getIngestionResult() { return ingestionResult; }
    public DocumentLayout getDocumentLayout()   { return documentLayout; }
    public String getAuditId()                  { return auditId; }
    public Instant getProcessedAt()             { return processedAt; }
}
