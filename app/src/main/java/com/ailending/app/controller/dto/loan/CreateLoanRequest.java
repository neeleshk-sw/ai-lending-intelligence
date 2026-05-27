package com.ailending.app.controller.dto.loan;

import java.math.BigDecimal;

/**
 * HTTP request body for {@code POST /api/lending/loans}.
 */
public class CreateLoanRequest {

    private String customerId;
    private BigDecimal amount;
    private String currency;

    // Jackson no-arg constructor
    public CreateLoanRequest() {}

    public String     getCustomerId() { return customerId; }
    public BigDecimal getAmount()     { return amount; }
    public String     getCurrency()   { return currency; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setAmount(BigDecimal amount)     { this.amount = amount; }
    public void setCurrency(String currency)     { this.currency = currency; }
}
