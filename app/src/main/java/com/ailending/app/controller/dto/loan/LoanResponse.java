package com.ailending.app.controller.dto.loan;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * HTTP response body for all loan endpoints.
 */
public class LoanResponse {

    private final String loanId;
    private final String customerId;
    private final BigDecimal amount;
    private final String currency;
    private final LoanStatus status;
    private final Instant submittedAt;
    private final Instant updatedAt;
    private final String assignedUnderwriter;

    private LoanResponse(String loanId, String customerId, BigDecimal amount, String currency,
                         LoanStatus status, Instant submittedAt, Instant updatedAt,
                         String assignedUnderwriter) {
        this.loanId               = loanId;
        this.customerId           = customerId;
        this.amount               = amount;
        this.currency             = currency;
        this.status               = status;
        this.submittedAt          = submittedAt;
        this.updatedAt            = updatedAt;
        this.assignedUnderwriter  = assignedUnderwriter;
    }

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getLoanId(),
                loan.getCustomerId(),
                loan.getAmount(),
                loan.getCurrency(),
                loan.getStatus(),
                loan.getSubmittedAt(),
                loan.getUpdatedAt(),
                loan.getAssignedUnderwriter()
        );
    }

    public String     getLoanId()               { return loanId; }
    public String     getCustomerId()           { return customerId; }
    public BigDecimal getAmount()               { return amount; }
    public String     getCurrency()             { return currency; }
    public LoanStatus getStatus()               { return status; }
    public Instant    getSubmittedAt()          { return submittedAt; }
    public Instant    getUpdatedAt()            { return updatedAt; }
    public String     getAssignedUnderwriter()  { return assignedUnderwriter; }
}
