package com.ailending.app.controller.dto.workflow;

import java.math.BigDecimal;

public class SubmitLoanRequest {
    private String customerId;
    private BigDecimal amount;
    private String currency;

    public String getCustomerId()           { return customerId; }
    public void setCustomerId(String v)     { this.customerId = v; }
    public BigDecimal getAmount()           { return amount; }
    public void setAmount(BigDecimal v)     { this.amount = v; }
    public String getCurrency()             { return currency; }
    public void setCurrency(String v)       { this.currency = v; }
}
