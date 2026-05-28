package com.ailending.workflow.service;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.workflow.event.WorkflowEvent;

import java.math.BigDecimal;
import java.util.List;

public interface WorkflowService {

    Loan submitLoan(String customerId, BigDecimal amount, String currency);

    Loan completeAiReview(String loanId, double confidenceScore, String modelUsed);

    Loan assignUnderwriter(String loanId, String underwriterId);

    Loan recordDecision(String loanId, LoanStatus finalStatus, String reason);

    List<WorkflowEvent> getHistory(String loanId);
}
