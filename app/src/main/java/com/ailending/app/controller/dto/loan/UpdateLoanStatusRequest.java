package com.ailending.app.controller.dto.loan;

import com.ailending.lending.domain.LoanStatus;

/**
 * HTTP request body for {@code PATCH /api/lending/loans/{loanId}/status}.
 */
public class UpdateLoanStatusRequest {

    private LoanStatus status;

    // Jackson no-arg constructor
    public UpdateLoanStatusRequest() {}

    public LoanStatus getStatus()              { return status; }
    public void setStatus(LoanStatus status)   { this.status = status; }
}
