package com.ailending.workflow.event;

import java.time.Instant;

public class UnderwriterAssignedEvent extends WorkflowEvent {

    private final String underwriterId;

    public UnderwriterAssignedEvent(String loanId, String customerId, Instant occurredAt,
                                    String underwriterId) {
        super(loanId, customerId, occurredAt);
        this.underwriterId = underwriterId;
    }

    public String getUnderwriterId() { return underwriterId; }
}
