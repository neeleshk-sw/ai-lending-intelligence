package com.ailending.ruleengine.model;

import com.ailending.ruleengine.exception.RuleEvaluationException;

import java.math.BigDecimal;

public final class RuleInput {

    private final String loanId;
    private final String customerId;
    private final BigDecimal monthlyIncome;
    private final BigDecimal requestedAmount;
    private final BigDecimal existingMonthlyDebt;
    private final int creditScore;
    private final boolean kycVerified;
    private final boolean amlClear;

    private RuleInput(Builder builder) {
        this.loanId              = builder.loanId;
        this.customerId          = builder.customerId;
        this.monthlyIncome       = builder.monthlyIncome;
        this.requestedAmount     = builder.requestedAmount;
        this.existingMonthlyDebt = builder.existingMonthlyDebt;
        this.creditScore         = builder.creditScore;
        this.kycVerified         = builder.kycVerified;
        this.amlClear            = builder.amlClear;
    }

    public String getLoanId()                   { return loanId; }
    public String getCustomerId()               { return customerId; }
    public BigDecimal getMonthlyIncome()        { return monthlyIncome; }
    public BigDecimal getRequestedAmount()      { return requestedAmount; }
    public BigDecimal getExistingMonthlyDebt()  { return existingMonthlyDebt; }
    public int getCreditScore()                 { return creditScore; }
    public boolean isKycVerified()              { return kycVerified; }
    public boolean isAmlClear()                 { return amlClear; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String loanId;
        private String customerId;
        private BigDecimal monthlyIncome;
        private BigDecimal requestedAmount;
        private BigDecimal existingMonthlyDebt = BigDecimal.ZERO;
        private int creditScore;
        private boolean kycVerified;
        private boolean amlClear;

        public Builder loanId(String loanId)                            { this.loanId = loanId; return this; }
        public Builder customerId(String customerId)                    { this.customerId = customerId; return this; }
        public Builder monthlyIncome(BigDecimal monthlyIncome)          { this.monthlyIncome = monthlyIncome; return this; }
        public Builder requestedAmount(BigDecimal requestedAmount)      { this.requestedAmount = requestedAmount; return this; }
        public Builder existingMonthlyDebt(BigDecimal existingMonthlyDebt){ this.existingMonthlyDebt = existingMonthlyDebt; return this; }
        public Builder creditScore(int creditScore)                     { this.creditScore = creditScore; return this; }
        public Builder kycVerified(boolean kycVerified)                 { this.kycVerified = kycVerified; return this; }
        public Builder amlClear(boolean amlClear)                       { this.amlClear = amlClear; return this; }

        public RuleInput build() {
            if (loanId == null || loanId.isBlank()) {
                throw new RuleEvaluationException("loanId is required");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new RuleEvaluationException("customerId is required");
            }
            if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuleEvaluationException("monthlyIncome must be positive");
            }
            if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuleEvaluationException("requestedAmount must be positive");
            }
            return new RuleInput(this);
        }
    }
}
