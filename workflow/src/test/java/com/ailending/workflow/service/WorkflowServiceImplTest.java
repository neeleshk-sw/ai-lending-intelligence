package com.ailending.workflow.service;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;
import com.ailending.lending.service.LoanService;
import com.ailending.workflow.event.AiReviewCompletedEvent;
import com.ailending.workflow.event.DecisionRecordedEvent;
import com.ailending.workflow.event.LoanSubmittedEvent;
import com.ailending.workflow.event.UnderwriterAssignedEvent;
import com.ailending.workflow.event.WorkflowEvent;
import com.ailending.workflow.exception.WorkflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkflowServiceImplTest {

    private LoanService loanService;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        loanService     = mock(LoanService.class);
        workflowService = new WorkflowServiceImpl(loanService);
    }

    // -----------------------------------------------------------------------
    // submitLoan
    // -----------------------------------------------------------------------

    @Test
    void submitLoan_delegatesToLoanService_andRecordsEvent() {
        Loan loan = Loan.builder()
                .loanId("loan-1").customerId("cust-1")
                .amount(new BigDecimal("500000")).currency("INR")
                .status(LoanStatus.AI_REVIEW_PENDING).build();
        when(loanService.createLoan("cust-1", new BigDecimal("500000"), "INR")).thenReturn(loan);

        Loan result = workflowService.submitLoan("cust-1", new BigDecimal("500000"), "INR");

        assertEquals(LoanStatus.AI_REVIEW_PENDING, result.getStatus());
        verify(loanService).createLoan("cust-1", new BigDecimal("500000"), "INR");

        List<WorkflowEvent> history = workflowService.getHistory("loan-1");
        assertEquals(1, history.size());
        assertInstanceOf(LoanSubmittedEvent.class, history.get(0));
        LoanSubmittedEvent event = (LoanSubmittedEvent) history.get(0);
        assertEquals("loan-1", event.getLoanId());
        assertEquals("cust-1", event.getCustomerId());
        assertEquals(new BigDecimal("500000"), event.getAmount());
        assertEquals("INR", event.getCurrency());
    }

    // -----------------------------------------------------------------------
    // completeAiReview
    // -----------------------------------------------------------------------

    @Test
    void completeAiReview_transitionsStatusAndRecordsEvent() {
        Loan loan = stubLoan("loan-2", "cust-2", LoanStatus.AI_REVIEW_COMPLETED);
        when(loanService.updateStatus("loan-2", LoanStatus.AI_REVIEW_COMPLETED)).thenReturn(loan);

        workflowService.completeAiReview("loan-2", 0.87, "llama3:8b");

        verify(loanService).updateStatus("loan-2", LoanStatus.AI_REVIEW_COMPLETED);

        List<WorkflowEvent> history = workflowService.getHistory("loan-2");
        assertEquals(1, history.size());
        assertInstanceOf(AiReviewCompletedEvent.class, history.get(0));
        AiReviewCompletedEvent event = (AiReviewCompletedEvent) history.get(0);
        assertEquals(0.87, event.getConfidenceScore(), 0.001);
        assertEquals("llama3:8b", event.getModelUsed());
    }

    // -----------------------------------------------------------------------
    // assignUnderwriter
    // -----------------------------------------------------------------------

    @Test
    void assignUnderwriter_transitionsStatusAndRecordsEvent() {
        Loan loan = stubLoan("loan-3", "cust-3", LoanStatus.UNDERWRITER_REVIEW_PENDING);
        when(loanService.updateStatus("loan-3", LoanStatus.UNDERWRITER_REVIEW_PENDING)).thenReturn(loan);

        workflowService.assignUnderwriter("loan-3", "underwriter-42");

        verify(loanService).updateStatus("loan-3", LoanStatus.UNDERWRITER_REVIEW_PENDING);

        List<WorkflowEvent> history = workflowService.getHistory("loan-3");
        assertEquals(1, history.size());
        assertInstanceOf(UnderwriterAssignedEvent.class, history.get(0));
        assertEquals("underwriter-42", ((UnderwriterAssignedEvent) history.get(0)).getUnderwriterId());
    }

    // -----------------------------------------------------------------------
    // recordDecision
    // -----------------------------------------------------------------------

    @Test
    void recordDecision_finalDecision_transitionsAndRecordsEvent() {
        Loan loan = stubLoan("loan-4", "cust-4", LoanStatus.FINAL_DECISION_COMPLETED);
        when(loanService.updateStatus("loan-4", LoanStatus.FINAL_DECISION_COMPLETED)).thenReturn(loan);

        workflowService.recordDecision("loan-4", LoanStatus.FINAL_DECISION_COMPLETED, "All checks passed");

        verify(loanService).updateStatus("loan-4", LoanStatus.FINAL_DECISION_COMPLETED);

        List<WorkflowEvent> history = workflowService.getHistory("loan-4");
        assertEquals(1, history.size());
        assertInstanceOf(DecisionRecordedEvent.class, history.get(0));
        DecisionRecordedEvent event = (DecisionRecordedEvent) history.get(0);
        assertEquals(LoanStatus.FINAL_DECISION_COMPLETED, event.getFinalStatus());
        assertEquals("All checks passed", event.getReason());
    }

    @Test
    void recordDecision_manualOverride_transitionsAndRecordsEvent() {
        Loan loan = stubLoan("loan-5", "cust-5", LoanStatus.MANUAL_OVERRIDE);
        when(loanService.updateStatus("loan-5", LoanStatus.MANUAL_OVERRIDE)).thenReturn(loan);

        workflowService.recordDecision("loan-5", LoanStatus.MANUAL_OVERRIDE, "Overridden by manager");

        verify(loanService).updateStatus("loan-5", LoanStatus.MANUAL_OVERRIDE);

        List<WorkflowEvent> history = workflowService.getHistory("loan-5");
        DecisionRecordedEvent event = (DecisionRecordedEvent) history.get(0);
        assertEquals(LoanStatus.MANUAL_OVERRIDE, event.getFinalStatus());
    }

    @Test
    void recordDecision_nonTerminalStatus_throwsWorkflowException() {
        assertThrows(WorkflowException.class,
                () -> workflowService.recordDecision("loan-6", LoanStatus.AI_REVIEW_PENDING, "bad"));
        verifyNoInteractions(loanService);
    }

    // -----------------------------------------------------------------------
    // getHistory — ordering
    // -----------------------------------------------------------------------

    @Test
    void getHistory_returnsEventsInInsertionOrder() {
        Loan loan = stubLoan("loan-7", "cust-7", LoanStatus.AI_REVIEW_PENDING);
        when(loanService.createLoan(any(), any(), any())).thenReturn(loan);

        Loan aiLoan = stubLoan("loan-7", "cust-7", LoanStatus.AI_REVIEW_COMPLETED);
        when(loanService.updateStatus("loan-7", LoanStatus.AI_REVIEW_COMPLETED)).thenReturn(aiLoan);

        Loan uwLoan = stubLoan("loan-7", "cust-7", LoanStatus.UNDERWRITER_REVIEW_PENDING);
        when(loanService.updateStatus("loan-7", LoanStatus.UNDERWRITER_REVIEW_PENDING)).thenReturn(uwLoan);

        workflowService.submitLoan("cust-7", new BigDecimal("100000"), "INR");
        workflowService.completeAiReview("loan-7", 0.9, "llama3:8b");
        workflowService.assignUnderwriter("loan-7", "uw-1");

        List<WorkflowEvent> history = workflowService.getHistory("loan-7");

        assertEquals(3, history.size(), "Three events should be recorded in order");
        assertInstanceOf(LoanSubmittedEvent.class, history.get(0));
        assertInstanceOf(AiReviewCompletedEvent.class, history.get(1));
        assertInstanceOf(UnderwriterAssignedEvent.class, history.get(2));
    }

    @Test
    void getHistory_unknownLoanId_returnsEmptyList() {
        List<WorkflowEvent> history = workflowService.getHistory("no-such-loan");
        assertTrue(history.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Error wrapping
    // -----------------------------------------------------------------------

    @Test
    void completeAiReview_loanNotFound_throwsWorkflowException() {
        when(loanService.updateStatus(eq("missing"), eq(LoanStatus.AI_REVIEW_COMPLETED)))
                .thenThrow(new LoanNotFoundException("missing"));

        assertThrows(WorkflowException.class,
                () -> workflowService.completeAiReview("missing", 0.5, "llama3:8b"));
    }

    @Test
    void assignUnderwriter_illegalTransition_throwsWorkflowException() {
        when(loanService.updateStatus(eq("loan-bad"), eq(LoanStatus.UNDERWRITER_REVIEW_PENDING)))
                .thenThrow(new InvalidStatusTransitionException(LoanStatus.AI_REVIEW_PENDING, LoanStatus.UNDERWRITER_REVIEW_PENDING));

        assertThrows(WorkflowException.class,
                () -> workflowService.assignUnderwriter("loan-bad", "uw-1"));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Loan stubLoan(String loanId, String customerId, LoanStatus status) {
        return Loan.builder()
                .loanId(loanId)
                .customerId(customerId)
                .amount(new BigDecimal("100000"))
                .currency("INR")
                .status(status)
                .build();
    }
}
