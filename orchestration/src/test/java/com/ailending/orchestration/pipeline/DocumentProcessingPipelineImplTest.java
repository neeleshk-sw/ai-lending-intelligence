package com.ailending.orchestration.pipeline;

import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.ailending.common.enums.DocumentStatus;
import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.documentintelligence.service.DocumentIntelligenceService;
import com.ailending.ingestion.parser.DocumentParser;
import com.ailending.ingestion.service.DocumentIngestor;
import com.ailending.ingestion.service.IngestionRequest;
import com.ailending.ingestion.service.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DocumentProcessingPipelineImplTest {

    private DocumentIngestor documentIngestor;
    private DocumentParser documentParser;
    private DocumentIntelligenceService documentIntelligenceService;
    private AuditService auditService;
    private DocumentProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        documentIngestor           = mock(DocumentIngestor.class);
        documentParser             = mock(DocumentParser.class);
        documentIntelligenceService = mock(DocumentIntelligenceService.class);
        auditService               = mock(AuditService.class);
        pipeline = new DocumentProcessingPipelineImpl(
                documentIngestor, documentParser, documentIntelligenceService, auditService);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void process_allServicesCalledInOrder_returnsResult() {
        IngestionRequest request = sampleRequest("Income: 50000\nCity: Mumbai");
        IngestionResult ingestionResult = new IngestionResult("src-1", 3, DocumentStatus.COMPLETED, 100L);
        DocumentLayout layout = new DocumentLayout(Collections.emptyList(), Collections.emptyList());

        when(documentIngestor.ingest(request)).thenReturn(ingestionResult);
        when(documentParser.parse(any(), anyString())).thenReturn("Income: 50000\nCity: Mumbai");
        when(documentIntelligenceService.analyze(anyString(), anyString())).thenReturn(layout);

        DocumentProcessingResult result = pipeline.process(request);

        assertNotNull(result);
        assertSame(ingestionResult, result.getIngestionResult());
        assertSame(layout, result.getDocumentLayout());
        assertNotNull(result.getAuditId(), "auditId must be populated");
        assertNotNull(result.getProcessedAt());

        verify(documentIngestor).ingest(request);
        verify(documentParser).parse(any(), anyString());
        verify(documentIntelligenceService).analyze(anyString(), eq("SALARY_SLIP"));
        verify(auditService).record(any(AuditRecord.class));
    }

    @Test
    void process_zeroChunkResult_stillRunsDocumentIntelligence() {
        IngestionRequest request = sampleRequest("some text");
        IngestionResult zeroChunks = new IngestionResult("src-1", 0, DocumentStatus.COMPLETED, 50L);
        DocumentLayout layout = DocumentLayout.empty();

        when(documentIngestor.ingest(request)).thenReturn(zeroChunks);
        when(documentParser.parse(any(), anyString())).thenReturn("some text");
        when(documentIntelligenceService.analyze(anyString(), anyString())).thenReturn(layout);

        DocumentProcessingResult result = pipeline.process(request);

        assertEquals(0, result.getIngestionResult().getChunkCount());
        verify(documentIntelligenceService).analyze(anyString(), anyString());
    }

    @Test
    void process_auditRecordContainsLoanIdAndCustomerId() {
        when(documentIngestor.ingest(any())).thenReturn(
                new IngestionResult("src-1", 2, DocumentStatus.COMPLETED, 80L));
        when(documentParser.parse(any(), anyString())).thenReturn("text");
        when(documentIntelligenceService.analyze(anyString(), anyString())).thenReturn(DocumentLayout.empty());

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        pipeline.process(sampleRequest("text"));
        verify(auditService).record(captor.capture());

        assertEquals("loan-1",  captor.getValue().getLoanId());
        assertEquals("cust-1",  captor.getValue().getCustomerId());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private IngestionRequest sampleRequest(String textContent) {
        return IngestionRequest.builder()
                .content(textContent.getBytes(StandardCharsets.UTF_8))
                .mimeType("text/plain")
                .loanId("loan-1")
                .customerId("cust-1")
                .documentType("SALARY_SLIP")
                .sourceDocumentId("src-1")
                .build();
    }
}
