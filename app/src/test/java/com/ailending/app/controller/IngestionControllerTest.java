package com.ailending.app.controller;

import com.ailending.common.enums.DocumentStatus;
import com.ailending.ingestion.service.DocumentIngestor;
import com.ailending.ingestion.service.IngestionRequest;
import com.ailending.ingestion.service.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link IngestionController}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup()} — no Spring context, no Ollama,
 * no pgvector required.
 */
class IngestionControllerTest {

    private DocumentIngestor documentIngestor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        documentIngestor = mock(DocumentIngestor.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IngestionController(documentIngestor))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void ingest_returns200_withExpectedBody() throws Exception {
        IngestionResult result = new IngestionResult("src-001", 8, DocumentStatus.COMPLETED, 1200L);
        when(documentIngestor.ingest(any(IngestionRequest.class))).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file", "salary_slip.pdf", "application/pdf", "PDF bytes".getBytes());

        mockMvc.perform(multipart("/api/lending/ingest")
                        .file(file)
                        .param("loanId",           "loan-001")
                        .param("customerId",       "cust-001")
                        .param("documentType",     "SALARY_SLIP")
                        .param("sourceDocumentId", "src-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceDocumentId").value("src-001"))
                .andExpect(jsonPath("$.chunkCount").value(8))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.durationMs").value(1200));
    }

    @Test
    void ingest_forwardsAllParamsToIngestor() throws Exception {
        when(documentIngestor.ingest(any(IngestionRequest.class)))
                .thenReturn(new IngestionResult("src-99", 3, DocumentStatus.COMPLETED, 500L));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/lending/ingest")
                .file(file)
                .param("loanId",           "loan-XYZ")
                .param("customerId",       "cust-ABC")
                .param("documentType",     "BANK_STATEMENT")
                .param("sourceDocumentId", "src-99")
                .param("policyVersion",    "v2.0"));

        ArgumentCaptor<IngestionRequest> captor = ArgumentCaptor.forClass(IngestionRequest.class);
        verify(documentIngestor).ingest(captor.capture());
        IngestionRequest captured = captor.getValue();

        assertEquals("loan-XYZ",      captured.getLoanId());
        assertEquals("cust-ABC",      captured.getCustomerId());
        assertEquals("BANK_STATEMENT", captured.getDocumentType());
        assertEquals("src-99",        captured.getSourceDocumentId());
        assertEquals("v2.0",          captured.getPolicyVersion());
    }

    @Test
    void ingest_withoutPolicyVersion_succeeds() throws Exception {
        when(documentIngestor.ingest(any(IngestionRequest.class)))
                .thenReturn(new IngestionResult("src-1", 5, DocumentStatus.COMPLETED, 800L));

        MockMultipartFile file = new MockMultipartFile(
                "file", "app.pdf", "application/pdf", "bytes".getBytes());

        // policyVersion is optional — omitting it must not cause a 4xx
        mockMvc.perform(multipart("/api/lending/ingest")
                        .file(file)
                        .param("loanId",           "loan-001")
                        .param("customerId",       "cust-001")
                        .param("documentType",     "LOAN_APPLICATION")
                        .param("sourceDocumentId", "src-1"))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // Error path
    // -----------------------------------------------------------------------

    @Test
    void ingest_ingestionThrowsIllegalArgument_returns422() throws Exception {
        when(documentIngestor.ingest(any(IngestionRequest.class)))
                .thenThrow(new IllegalArgumentException("content must not be null or empty"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/api/lending/ingest")
                        .file(file)
                        .param("loanId",           "loan-001")
                        .param("customerId",       "cust-001")
                        .param("documentType",     "SALARY_SLIP")
                        .param("sourceDocumentId", "src-1"))
                .andExpect(status().isUnprocessableEntity());
    }
}
