package com.ailending.lending.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity representing a loan application in the ALIM workflow.
 *
 * <p>Immutable fields (set at creation time): {@code loanId}, {@code customerId},
 * {@code amount}, {@code currency}, {@code submittedAt}.
 *
 * <p>Mutable fields (updated as the loan progresses through the workflow):
 * {@code status}, {@code updatedAt}, {@code assignedUnderwriter}.
 *
 * <p>AI is advisory only — this entity tracks the loan state but never makes
 * autonomous approval/rejection decisions (ADR-003).
 */
public class Loan {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** UUID-based primary identifier. */
    private final String loanId;

    /** Links to the borrower / applicant. */
    private final String customerId;

    /** Requested loan amount. */
    private final BigDecimal amount;

    /** ISO 4217 currency code (e.g., "INR", "USD"). */
    private final String currency;

    /** Current workflow state per FR-8. */
    private LoanStatus status;

    /** Wall-clock time when the loan was first submitted. */
    private final Instant submittedAt;

    /** Wall-clock time of the most recent state change. */
    private Instant updatedAt;

    /**
     * Identifier of the underwriter assigned to this loan.
     * {@code null} until the loan enters {@link LoanStatus#UNDERWRITER_REVIEW_PENDING}.
     */
    private String assignedUnderwriter;

    // -----------------------------------------------------------------------
    // Constructor (package-private; callers use the Builder)
    // -----------------------------------------------------------------------

    private Loan(Builder builder) {
        this.loanId               = builder.loanId;
        this.customerId           = builder.customerId;
        this.amount               = builder.amount;
        this.currency             = builder.currency;
        this.status               = builder.status;
        this.submittedAt          = builder.submittedAt;
        this.updatedAt            = builder.updatedAt;
        this.assignedUnderwriter  = builder.assignedUnderwriter;
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getLoanId()              { return loanId; }
    public String getCustomerId()          { return customerId; }
    public BigDecimal getAmount()          { return amount; }
    public String getCurrency()            { return currency; }
    public LoanStatus getStatus()          { return status; }
    public Instant getSubmittedAt()        { return submittedAt; }
    public Instant getUpdatedAt()          { return updatedAt; }
    public String getAssignedUnderwriter() { return assignedUnderwriter; }

    // -----------------------------------------------------------------------
    // Setters (mutable workflow fields only)
    // -----------------------------------------------------------------------

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setAssignedUnderwriter(String assignedUnderwriter) {
        this.assignedUnderwriter = assignedUnderwriter;
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Loan}.
     */
    public static class Builder {

        private String loanId;
        private String customerId;
        private BigDecimal amount;
        private String currency;
        private LoanStatus status = LoanStatus.AI_REVIEW_PENDING;
        private Instant submittedAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private String assignedUnderwriter;

        public Builder loanId(String loanId)                             { this.loanId = loanId; return this; }
        public Builder customerId(String customerId)                     { this.customerId = customerId; return this; }
        public Builder amount(BigDecimal amount)                         { this.amount = amount; return this; }
        public Builder currency(String currency)                         { this.currency = currency; return this; }
        public Builder status(LoanStatus status)                         { this.status = status; return this; }
        public Builder submittedAt(Instant submittedAt)                  { this.submittedAt = submittedAt; return this; }
        public Builder updatedAt(Instant updatedAt)                      { this.updatedAt = updatedAt; return this; }
        public Builder assignedUnderwriter(String assignedUnderwriter)   { this.assignedUnderwriter = assignedUnderwriter; return this; }

        public Loan build() {
            if (loanId == null || loanId.isBlank()) {
                throw new IllegalArgumentException("loanId must not be null or blank");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("customerId must not be null or blank");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("currency must not be null or blank");
            }
            return new Loan(this);
        }
    }

    // -----------------------------------------------------------------------
    // Object
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return "Loan{loanId='" + loanId + "', customerId='" + customerId
                + "', amount=" + amount + ", currency='" + currency
                + "', status=" + status + '}';
    }
}
