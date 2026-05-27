package com.ailending.app.controller;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;
import com.ailending.lending.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link LoanController}.
 *
 * <p>Uses standalone MockMvc — no Spring context required.
 */
class LoanControllerTest {

    private LoanService loanService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        loanService = mock(LoanService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoanController(loanService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/lending/loans  — createLoan
    // -----------------------------------------------------------------------

    @Test
    void createLoan_returns201_withLoanBody() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-uuid-1")
                .customerId("cust-99")
                .amount(new BigDecimal("75000"))
                .currency("INR")
                .build();
        when(loanService.createLoan(eq("cust-99"), eq(new BigDecimal("75000")), eq("INR")))
                .thenReturn(loan);

        mockMvc.perform(post("/api/lending/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"cust-99\",\"amount\":75000,\"currency\":\"INR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value("loan-uuid-1"))
                .andExpect(jsonPath("$.customerId").value("cust-99"))
                .andExpect(jsonPath("$.status").value("AI_REVIEW_PENDING"));
    }

    @Test
    void createLoan_invalidArgument_returns422() throws Exception {
        when(loanService.createLoan(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("amount must be positive"));

        mockMvc.perform(post("/api/lending/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"cust-1\",\"amount\":-1,\"currency\":\"INR\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    // -----------------------------------------------------------------------
    // GET /api/lending/loans/{loanId}  — getLoan
    // -----------------------------------------------------------------------

    @Test
    void getLoan_returns200_withLoanBody() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-42")
                .customerId("cust-5")
                .amount(new BigDecimal("50000"))
                .currency("USD")
                .build();
        when(loanService.getLoan("loan-42")).thenReturn(loan);

        mockMvc.perform(get("/api/lending/loans/loan-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("loan-42"))
                .andExpect(jsonPath("$.customerId").value("cust-5"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getLoan_unknownId_returns404() throws Exception {
        when(loanService.getLoan("ghost-id"))
                .thenThrow(new LoanNotFoundException("ghost-id"));

        mockMvc.perform(get("/api/lending/loans/ghost-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Loan not found: ghost-id"));
    }

    @Test
    void getLoan_responseContainsTimestampField() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-ts")
                .customerId("cust-1")
                .amount(new BigDecimal("10000"))
                .currency("INR")
                .build();
        when(loanService.getLoan("loan-ts")).thenReturn(loan);

        mockMvc.perform(get("/api/lending/loans/loan-ts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/lending/loans/{loanId}/status  — updateStatus
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_returns200_withUpdatedLoan() throws Exception {
        Loan loan = Loan.builder()
                .loanId("loan-7")
                .customerId("cust-7")
                .amount(new BigDecimal("30000"))
                .currency("INR")
                .status(LoanStatus.AI_REVIEW_COMPLETED)
                .build();
        when(loanService.updateStatus("loan-7", LoanStatus.AI_REVIEW_COMPLETED)).thenReturn(loan);

        mockMvc.perform(patch("/api/lending/loans/loan-7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_REVIEW_COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AI_REVIEW_COMPLETED"));
    }

    @Test
    void updateStatus_invalidTransition_returns400() throws Exception {
        when(loanService.updateStatus(eq("loan-7"), eq(LoanStatus.FINAL_DECISION_COMPLETED)))
                .thenThrow(new InvalidStatusTransitionException(
                        LoanStatus.AI_REVIEW_PENDING, LoanStatus.FINAL_DECISION_COMPLETED));

        mockMvc.perform(patch("/api/lending/loans/loan-7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINAL_DECISION_COMPLETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatus_unknownLoan_returns404() throws Exception {
        when(loanService.updateStatus(eq("missing"), any()))
                .thenThrow(new LoanNotFoundException("missing"));

        mockMvc.perform(patch("/api/lending/loans/missing/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_REVIEW_COMPLETED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_errorBodyContainsTimestamp() throws Exception {
        when(loanService.updateStatus(any(), any()))
                .thenThrow(new LoanNotFoundException("no-loan"));

        mockMvc.perform(patch("/api/lending/loans/no-loan/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_REVIEW_COMPLETED\"}"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
