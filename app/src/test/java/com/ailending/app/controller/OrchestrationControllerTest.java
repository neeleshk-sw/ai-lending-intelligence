package com.ailending.app.controller;

import com.ailending.common.enums.DocumentStatus;
import com.ailending.documentintelligence.model.DocumentLayout;
import com.ailending.ingestion.service.IngestionResult;
import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.orchestration.pipeline.DocumentProcessingPipeline;
import com.ailending.orchestration.pipeline.DocumentProcessingResult;
import com.ailending.orchestration.pipeline.LoanSubmissionPipeline;
import com.ailending.orchestration.pipeline.LoanSubmissionResult;
import com.ailending.ruleengine.model.RuleResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrchestrationControllerTest {

    private LoanSubmissionPipeline loanSubmissionPipeline;
    private DocumentProcessingPipeline documentProcessingPipeline;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        loanSubmissionPipeline    = mock(LoanSubmissionPipeline.class);
        documentProcessingPipeline = mock(DocumentProcessingPipeline.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrchestrationController(loanSubmissionPipeline, documentProcessingPipeline))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/lending/orchestration/loans
    // -----------------------------------------------------------------------

    @Test
    void submitLoan_returns201WithLoanIdAndRulesResult() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-1").customerId("cust-1")
                .amount(new BigDecimal("500000")).currency("INR")
                .status(LoanStatus.AI_REVIEW_PENDING).build();
        RuleResult ruleResult = new RuleResult("loan-1", true, Collections.emptyList(), Instant.now());

        when(loanSubmissionPipeline.process(any(), any(), any(), any()))
                .thenReturn(new LoanSubmissionResult(loan, ruleResult, "audit-1", Instant.now()));

        Map<String, Object> body = Map.of(
                "customerId", "cust-1",
                "amount", 500000,
                "currency", "INR",
                "monthlyIncome", 50000,
                "requestedAmount", 500000,
                "creditScore", 750,
                "kycVerified", true,
                "amlClear", true
        );

        mockMvc.perform(post("/api/lending/orchestration/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value("loan-1"))
                .andExpect(jsonPath("$.status").value("AI_REVIEW_PENDING"))
                .andExpect(jsonPath("$.rulesPassed").value(true))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.auditId").value("audit-1"));
    }

    @Test
    void submitLoan_rulesFail_returnsViolations() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-2").customerId("cust-2")
                .amount(new BigDecimal("100000")).currency("INR")
                .status(LoanStatus.AI_REVIEW_PENDING).build();
        RuleResult failed = new RuleResult("loan-2", false,
                java.util.List.of("Credit score too low"), Instant.now());

        when(loanSubmissionPipeline.process(any(), any(), any(), any()))
                .thenReturn(new LoanSubmissionResult(loan, failed, "audit-2", Instant.now()));

        Map<String, Object> body = Map.of(
                "customerId", "cust-2",
                "amount", 100000,
                "currency", "INR",
                "monthlyIncome", 10000,
                "requestedAmount", 100000,
                "creditScore", 500,
                "kycVerified", true,
                "amlClear", true
        );

        mockMvc.perform(post("/api/lending/orchestration/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rulesPassed").value(false))
                .andExpect(jsonPath("$.violations.length()").value(1));
    }

    // -----------------------------------------------------------------------
    // POST /api/lending/orchestration/documents
    // -----------------------------------------------------------------------

    @Test
    void processDocument_returns201WithCounts() throws Exception {
        IngestionResult ingestionResult = new IngestionResult("src-1", 3, DocumentStatus.COMPLETED, 100L);
        DocumentLayout layout = DocumentLayout.empty();

        when(documentProcessingPipeline.process(any()))
                .thenReturn(new DocumentProcessingResult(ingestionResult, layout, "audit-doc-1", Instant.now()));

        MockMultipartFile file = new MockMultipartFile(
                "file", "salary.txt", "text/plain", "Monthly Income: 75000".getBytes());

        mockMvc.perform(multipart("/api/lending/orchestration/documents")
                        .file(file)
                        .param("loanId",           "loan-1")
                        .param("customerId",        "cust-1")
                        .param("documentType",      "SALARY_SLIP")
                        .param("sourceDocumentId",  "src-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chunkCount").value(3))
                .andExpect(jsonPath("$.ingestionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.auditId").value("audit-doc-1"));
    }
}
