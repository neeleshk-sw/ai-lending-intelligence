package com.ailending.app.controller.dto.orchestration;

import java.math.BigDecimal;

public class LoanSubmissionRequest {
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal monthlyIncome;
    private BigDecimal requestedAmount;
    private BigDecimal existingMonthlyDebt;
    private int creditScore;
    private boolean kycVerified;
    private boolean amlClear;

    public String getCustomerId()               { return customerId; }
    public void setCustomerId(String v)         { this.customerId = v; }
    public BigDecimal getAmount()               { return amount; }
    public void setAmount(BigDecimal v)         { this.amount = v; }
    public String getCurrency()                 { return currency; }
    public void setCurrency(String v)           { this.currency = v; }
    public BigDecimal getMonthlyIncome()        { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal v)  { this.monthlyIncome = v; }
    public BigDecimal getRequestedAmount()      { return requestedAmount; }
    public void setRequestedAmount(BigDecimal v){ this.requestedAmount = v; }
    public BigDecimal getExistingMonthlyDebt()  { return existingMonthlyDebt; }
    public void setExistingMonthlyDebt(BigDecimal v){ this.existingMonthlyDebt = v; }
    public int getCreditScore()                 { return creditScore; }
    public void setCreditScore(int v)           { this.creditScore = v; }
    public boolean isKycVerified()              { return kycVerified; }
    public void setKycVerified(boolean v)       { this.kycVerified = v; }
    public boolean isAmlClear()                 { return amlClear; }
    public void setAmlClear(boolean v)          { this.amlClear = v; }
}
