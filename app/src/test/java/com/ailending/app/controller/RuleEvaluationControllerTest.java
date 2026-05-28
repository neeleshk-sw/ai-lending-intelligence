package com.ailending.app.controller;

import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.service.RuleEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RuleEvaluationControllerTest {

    private RuleEvaluationService ruleEvaluationService;
    private MockMvc mockMvc;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ruleEvaluationService = mock(RuleEvaluationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RuleEvaluationController(ruleEvaluationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void evaluate_allPassing_returnsPassed() throws Exception {
        when(ruleEvaluationService.evaluate(any()))
                .thenReturn(new RuleResult("loan-1", true, Collections.emptyList(), Instant.now()));

        Map<String, Object> body = Map.of(
                "loanId", "loan-1",
                "customerId", "cust-1",
                "monthlyIncome", 50000,
                "requestedAmount", 100000,
                "existingMonthlyDebt", 0,
                "creditScore", 750,
                "kycVerified", true,
                "amlClear", true
        );

        mockMvc.perform(post("/api/lending/rules/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("loan-1"))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(0));
    }

    @Test
    void evaluate_withViolations_returnsFailedWithMessages() throws Exception {
        when(ruleEvaluationService.evaluate(any()))
                .thenReturn(new RuleResult("loan-2", false,
                        List.of("Credit score too low", "KYC not verified"), Instant.now()));

        Map<String, Object> body = Map.of(
                "loanId", "loan-2",
                "customerId", "cust-2",
                "monthlyIncome", 10000,
                "requestedAmount", 50000,
                "creditScore", 500,
                "kycVerified", false,
                "amlClear", true
        );

        mockMvc.perform(post("/api/lending/rules/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    void evaluate_serviceThrowsIllegalArgument_returns422() throws Exception {
        when(ruleEvaluationService.evaluate(any()))
                .thenThrow(new IllegalArgumentException("monthlyIncome must be positive"));

        Map<String, Object> body = Map.of(
                "loanId", "loan-3",
                "customerId", "cust-3",
                "monthlyIncome", -1,
                "requestedAmount", 50000,
                "creditScore", 700,
                "kycVerified", true,
                "amlClear", true
        );

        mockMvc.perform(post("/api/lending/rules/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }
}
