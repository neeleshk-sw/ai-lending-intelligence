package com.ailending.workflow.event;

import java.time.Instant;

public abstract class WorkflowEvent {

    private final String loanId;
    private final String customerId;
    private final Instant occurredAt;

    protected WorkflowEvent(String loanId, String customerId, Instant occurredAt) {
        this.loanId      = loanId;
        this.customerId  = customerId;
        this.occurredAt  = occurredAt;
    }

    public String getLoanId()       { return loanId; }
    public String getCustomerId()   { return customerId; }
    public Instant getOccurredAt()  { return occurredAt; }
}
