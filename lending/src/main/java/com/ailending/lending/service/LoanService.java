package com.ailending.lending.service;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;

import java.math.BigDecimal;

/**
 * Business operations for managing loan applications.
 *
 * <p>This service covers the creation and lifecycle management of {@link Loan} entities
 * through the FR-8 workflow states. All final lending decisions are made by human
 * underwriters; this service never approves or rejects loans autonomously (ADR-003).
 */
public interface LoanService {

    /**
     * Creates a new loan application with status {@link LoanStatus#AI_REVIEW_PENDING}.
     *
     * @param customerId borrower identifier (must not be blank)
     * @param amount     requested loan amount (must be positive)
     * @param currency   ISO 4217 currency code (must not be blank)
     * @return the newly created {@link Loan}
     */
    Loan createLoan(String customerId, BigDecimal amount, String currency);

    /**
     * Retrieves a loan by its unique ID.
     *
     * @param loanId the loan's UUID
     * @return the matching {@link Loan}
     * @throws LoanNotFoundException if no loan exists with the given ID
     */
    Loan getLoan(String loanId);

    /**
     * Transitions a loan to a new workflow status.
     *
     * <p>Only transitions that are legal per the FR-8 workflow graph are allowed:
     * <pre>
     *   AI_REVIEW_PENDING → AI_REVIEW_COMPLETED
     *   AI_REVIEW_COMPLETED → UNDERWRITER_REVIEW_PENDING
     *   UNDERWRITER_REVIEW_PENDING → MANUAL_OVERRIDE
     *   UNDERWRITER_REVIEW_PENDING → FINAL_DECISION_COMPLETED
     * </pre>
     *
     * @param loanId    the loan to update
     * @param newStatus the target status
     * @return the updated {@link Loan}
     * @throws LoanNotFoundException           if no loan exists with the given ID
     * @throws InvalidStatusTransitionException if the transition is not permitted
     */
    Loan updateStatus(String loanId, LoanStatus newStatus);
}
