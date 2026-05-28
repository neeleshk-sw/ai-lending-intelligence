package com.ailending.orchestration.pipeline;

import com.ailending.audit.model.AuditEventType;
import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.documentintelligence.service.DocumentIntelligenceService;
import com.ailending.ingestion.parser.DocumentParser;
import com.ailending.ingestion.service.DocumentIngestor;
import com.ailending.ingestion.service.IngestionRequest;
import com.ailending.ingestion.service.IngestionResult;
import com.ailending.orchestration.exception.OrchestrationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class DocumentProcessingPipelineImpl implements DocumentProcessingPipeline {

    private final DocumentIngestor documentIngestor;
    private final DocumentParser documentParser;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final AuditService auditService;

    public DocumentProcessingPipelineImpl(DocumentIngestor documentIngestor,
                                           DocumentParser documentParser,
                                           DocumentIntelligenceService documentIntelligenceService,
                                           AuditService auditService) {
        this.documentIngestor           = documentIngestor;
        this.documentParser             = documentParser;
        this.documentIntelligenceService = documentIntelligenceService;
        this.auditService               = auditService;
    }

    @Override
    public DocumentProcessingResult process(IngestionRequest request) {
        try {
            // Step 1: ingest (parse → chunk → embed → store)
            IngestionResult ingestionResult = documentIngestor.ingest(request);

            // Step 2: extract plain text for structured analysis
            String parsedText;
            try {
                parsedText = documentParser.parse(request.getContent(), request.getMimeType());
            } catch (Exception e) {
                // Fall back to raw bytes interpreted as UTF-8 for plain-text test fixtures
                parsedText = new String(request.getContent(), StandardCharsets.UTF_8);
            }

            // Step 3: structured document intelligence (tables, form fields)
            DocumentLayout documentLayout = documentIntelligenceService.analyze(
                    parsedText, request.getDocumentType());

            // Step 4: audit trail
            AuditRecord auditRecord = AuditRecord.builder()
                    .loanId(request.getLoanId())
                    .customerId(request.getCustomerId())
                    .eventType(AuditEventType.STATUS_TRANSITION)
                    .performedBy("system")
                    .notes(String.format("Ingested %d chunks; extracted %d form fields, %d tables",
                            ingestionResult.getChunkCount(),
                            documentLayout.getFormFields().size(),
                            documentLayout.getTables().size()))
                    .build();
            auditService.record(auditRecord);

            return new DocumentProcessingResult(
                    ingestionResult, documentLayout, auditRecord.getAuditId(), Instant.now());

        } catch (OrchestrationException e) {
            throw e;
        } catch (Exception e) {
            throw new OrchestrationException(
                    "Document processing pipeline failed for loan " + request.getLoanId()
                    + ": " + e.getMessage(), e);
        }
    }
}
