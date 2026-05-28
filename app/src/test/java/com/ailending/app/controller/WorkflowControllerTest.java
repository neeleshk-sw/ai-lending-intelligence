package com.ailending.app.controller;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.workflow.event.LoanSubmittedEvent;
import com.ailending.workflow.event.WorkflowEvent;
import com.ailending.workflow.exception.WorkflowException;
import com.ailending.workflow.service.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkflowControllerTest {

    private WorkflowService workflowService;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        workflowService = mock(WorkflowService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WorkflowController(workflowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/lending/workflow/loans — submit
    // -----------------------------------------------------------------------

    @Test
    void submitLoan_returns201WithLoan() throws Exception {
        when(workflowService.submitLoan("cust-1", new BigDecimal("500000"), "INR"))
                .thenReturn(loan("loan-1", "cust-1", LoanStatus.AI_REVIEW_PENDING));

        Map<String, Object> body = Map.of(
                "customerId", "cust-1",
                "amount", 500000,
                "currency", "INR"
        );

        mockMvc.perform(post("/api/lending/workflow/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value("loan-1"))
                .andExpect(jsonPath("$.status").value("AI_REVIEW_PENDING"));
    }

    // -----------------------------------------------------------------------
    // PATCH .../ai-review
    // -----------------------------------------------------------------------

    @Test
    void completeAiReview_returns200() throws Exception {
        when(workflowService.completeAiReview("loan-1", 0.88, "llama3:8b"))
                .thenReturn(loan("loan-1", "cust-1", LoanStatus.AI_REVIEW_COMPLETED));

        Map<String, Object> body = Map.of("confidenceScore", 0.88, "modelUsed", "llama3:8b");

        mockMvc.perform(patch("/api/lending/workflow/loans/loan-1/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AI_REVIEW_COMPLETED"));
    }

    // -----------------------------------------------------------------------
    // PATCH .../underwriter
    // -----------------------------------------------------------------------

    @Test
    void assignUnderwriter_returns200() throws Exception {
        when(workflowService.assignUnderwriter("loan-1", "uw-99"))
                .thenReturn(loan("loan-1", "cust-1", LoanStatus.UNDERWRITER_REVIEW_PENDING));

        Map<String, Object> body = Map.of("underwriterId", "uw-99");

        mockMvc.perform(patch("/api/lending/workflow/loans/loan-1/underwriter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDERWRITER_REVIEW_PENDING"));
    }

    // -----------------------------------------------------------------------
    // PATCH .../decision
    // -----------------------------------------------------------------------

    @Test
    void recordDecision_returns200() throws Exception {
        when(workflowService.recordDecision("loan-1", LoanStatus.FINAL_DECISION_COMPLETED, "Approved"))
                .thenReturn(loan("loan-1", "cust-1", LoanStatus.FINAL_DECISION_COMPLETED));

        Map<String, Object> body = Map.of("finalStatus", "FINAL_DECISION_COMPLETED", "reason", "Approved");

        mockMvc.perform(patch("/api/lending/workflow/loans/loan-1/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINAL_DECISION_COMPLETED"));
    }

    @Test
    void recordDecision_workflowException_returns400() throws Exception {
        when(workflowService.recordDecision(any(), any(), any()))
                .thenThrow(new WorkflowException("Cannot record decision: invalid state"));

        Map<String, Object> body = Map.of("finalStatus", "FINAL_DECISION_COMPLETED", "reason", "test");

        mockMvc.perform(patch("/api/lending/workflow/loans/loan-bad/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET .../history
    // -----------------------------------------------------------------------

    @Test
    void getHistory_returnsEventList() throws Exception {
        WorkflowEvent event = new LoanSubmittedEvent(
                "loan-1", "cust-1", Instant.now(), new BigDecimal("500000"), "INR");
        when(workflowService.getHistory("loan-1")).thenReturn(List.of(event));

        mockMvc.perform(get("/api/lending/workflow/loans/loan-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("LoanSubmittedEvent"))
                .andExpect(jsonPath("$[0].loanId").value("loan-1"));
    }

    @Test
    void getHistory_emptyHistory_returnsEmptyArray() throws Exception {
        when(workflowService.getHistory("loan-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/lending/workflow/loans/loan-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Loan loan(String loanId, String customerId, LoanStatus status) {
        return Loan.builder()
                .loanId(loanId)
                .customerId(customerId)
                .amount(new BigDecimal("500000"))
                .currency("INR")
                .status(status)
                .build();
    }
}
