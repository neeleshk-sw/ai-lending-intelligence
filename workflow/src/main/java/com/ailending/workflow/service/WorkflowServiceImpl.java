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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final LoanService loanService;
    private final ConcurrentHashMap<String, List<WorkflowEvent>> eventHistory = new ConcurrentHashMap<>();

    public WorkflowServiceImpl(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public Loan submitLoan(String customerId, BigDecimal amount, String currency) {
        try {
            Loan loan = loanService.createLoan(customerId, amount, currency);
            appendEvent(loan.getLoanId(), new LoanSubmittedEvent(
                    loan.getLoanId(), loan.getCustomerId(), Instant.now(),
                    loan.getAmount(), loan.getCurrency()));
            return loan;
        } catch (Exception e) {
            throw new WorkflowException("Failed to submit loan for customer " + customerId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Loan completeAiReview(String loanId, double confidenceScore, String modelUsed) {
        try {
            Loan loan = loanService.updateStatus(loanId, LoanStatus.AI_REVIEW_COMPLETED);
            appendEvent(loanId, new AiReviewCompletedEvent(
                    loanId, loan.getCustomerId(), Instant.now(), confidenceScore, modelUsed));
            return loan;
        } catch (LoanNotFoundException | InvalidStatusTransitionException e) {
            throw new WorkflowException("Cannot complete AI review for loan " + loanId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Loan assignUnderwriter(String loanId, String underwriterId) {
        try {
            Loan loan = loanService.updateStatus(loanId, LoanStatus.UNDERWRITER_REVIEW_PENDING);
            appendEvent(loanId, new UnderwriterAssignedEvent(
                    loanId, loan.getCustomerId(), Instant.now(), underwriterId));
            return loan;
        } catch (LoanNotFoundException | InvalidStatusTransitionException e) {
            throw new WorkflowException("Cannot assign underwriter for loan " + loanId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Loan recordDecision(String loanId, LoanStatus finalStatus, String reason) {
        if (finalStatus != LoanStatus.FINAL_DECISION_COMPLETED && finalStatus != LoanStatus.MANUAL_OVERRIDE) {
            throw new WorkflowException("recordDecision only accepts terminal statuses FINAL_DECISION_COMPLETED or MANUAL_OVERRIDE, got: " + finalStatus);
        }
        try {
            Loan loan = loanService.updateStatus(loanId, finalStatus);
            appendEvent(loanId, new DecisionRecordedEvent(
                    loanId, loan.getCustomerId(), Instant.now(), finalStatus, reason));
            return loan;
        } catch (LoanNotFoundException | InvalidStatusTransitionException e) {
            throw new WorkflowException("Cannot record decision for loan " + loanId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<WorkflowEvent> getHistory(String loanId) {
        return Collections.unmodifiableList(
                eventHistory.getOrDefault(loanId, Collections.emptyList()));
    }

    private void appendEvent(String loanId, WorkflowEvent event) {
        eventHistory.computeIfAbsent(loanId, k -> new ArrayList<>()).add(event);
    }
}
