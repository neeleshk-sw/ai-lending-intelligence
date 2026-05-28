package com.ailending.orchestration.pipeline;

import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.orchestration.exception.OrchestrationException;
import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.service.RuleEvaluationService;
import com.ailending.workflow.exception.WorkflowException;
import com.ailending.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanSubmissionPipelineImplTest {

    private WorkflowService workflowService;
    private RuleEvaluationService ruleEvaluationService;
    private AuditService auditService;
    private LoanSubmissionPipeline pipeline;

    @BeforeEach
    void setUp() {
        workflowService       = mock(WorkflowService.class);
        ruleEvaluationService = mock(RuleEvaluationService.class);
        auditService          = mock(AuditService.class);
        pipeline = new LoanSubmissionPipelineImpl(workflowService, ruleEvaluationService, auditService);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void process_allServicesCalledInOrder_returnsResult() {
        Loan loan = stubLoan("loan-1", "cust-1");
        RuleResult ruleResult = new RuleResult("loan-1", true, Collections.emptyList(), Instant.now());

        when(workflowService.submitLoan("cust-1", new BigDecimal("500000"), "INR")).thenReturn(loan);
        when(ruleEvaluationService.evaluate(any())).thenReturn(ruleResult);

        LoanSubmissionResult result = pipeline.process("cust-1", new BigDecimal("500000"), "INR", passingRuleInput());

        assertNotNull(result);
        assertEquals("loan-1", result.getLoan().getLoanId());
        assertTrue(result.getRuleResult().isPassed());
        assertNotNull(result.getAuditId(), "auditId must be populated");
        assertNotNull(result.getProcessedAt(), "processedAt must be set");

        // Verify call order
        verify(workflowService).submitLoan("cust-1", new BigDecimal("500000"), "INR");
        verify(ruleEvaluationService).evaluate(any());
        verify(auditService).record(any(AuditRecord.class));
    }

    @Test
    void process_rulesPass_auditDecisionIsRulesPassed() {
        when(workflowService.submitLoan(any(), any(), any())).thenReturn(stubLoan("loan-1", "cust-1"));
        when(ruleEvaluationService.evaluate(any()))
                .thenReturn(new RuleResult("loan-1", true, Collections.emptyList(), Instant.now()));

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        pipeline.process("cust-1", new BigDecimal("100000"), "INR", passingRuleInput());

        verify(auditService).record(captor.capture());
        assertEquals("RULES_PASSED", captor.getValue().getDecision());
    }

    @Test
    void process_rulesFail_auditDecisionIsRulesFailed() {
        when(workflowService.submitLoan(any(), any(), any())).thenReturn(stubLoan("loan-1", "cust-1"));
        when(ruleEvaluationService.evaluate(any()))
                .thenReturn(new RuleResult("loan-1", false, List.of("Credit score too low"), Instant.now()));

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        pipeline.process("cust-1", new BigDecimal("100000"), "INR", passingRuleInput());

        verify(auditService).record(captor.capture());
        assertEquals("RULES_FAILED", captor.getValue().getDecision());
        assertNotNull(captor.getValue().getNotes(), "Violation messages should be in notes");
    }

    // -----------------------------------------------------------------------
    // Error handling
    // -----------------------------------------------------------------------

    @Test
    void process_workflowExceptionThrown_wrappedAsOrchestrationException() {
        when(workflowService.submitLoan(any(), any(), any()))
                .thenThrow(new WorkflowException("Loan creation failed"));

        assertThrows(OrchestrationException.class,
                () -> pipeline.process("cust-1", new BigDecimal("100000"), "INR", passingRuleInput()));

        verifyNoInteractions(ruleEvaluationService, auditService);
    }

    @Test
    void process_ruleEvaluationThrows_wrappedAsOrchestrationException() {
        when(workflowService.submitLoan(any(), any(), any())).thenReturn(stubLoan("loan-1", "cust-1"));
        when(ruleEvaluationService.evaluate(any())).thenThrow(new RuntimeException("evaluation error"));

        assertThrows(OrchestrationException.class,
                () -> pipeline.process("cust-1", new BigDecimal("100000"), "INR", passingRuleInput()));

        verifyNoInteractions(auditService);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Loan stubLoan(String loanId, String customerId) {
        return Loan.builder()
                .loanId(loanId).customerId(customerId)
                .amount(new BigDecimal("100000")).currency("INR")
                .status(LoanStatus.AI_REVIEW_PENDING).build();
    }

    private RuleInput passingRuleInput() {
        return RuleInput.builder()
                .loanId("loan-1").customerId("cust-1")
                .monthlyIncome(new BigDecimal("50000"))
                .requestedAmount(new BigDecimal("100000"))
                .creditScore(750).kycVerified(true).amlClear(true).build();
    }
}
