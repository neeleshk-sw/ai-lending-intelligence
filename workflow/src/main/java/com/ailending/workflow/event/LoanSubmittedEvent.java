package com.ailending.workflow.event;

import java.math.BigDecimal;
import java.time.Instant;

public class LoanSubmittedEvent extends WorkflowEvent {

    private final BigDecimal amount;
    private final String currency;

    public LoanSubmittedEvent(String loanId, String customerId, Instant occurredAt,
                              BigDecimal amount, String currency) {
        super(loanId, customerId, occurredAt);
        this.amount   = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount()  { return amount; }
    public String getCurrency()    { return currency; }
}
