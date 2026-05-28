package com.ailending.workflow.event;

import com.ailending.lending.domain.LoanStatus;

import java.time.Instant;

public class DecisionRecordedEvent extends WorkflowEvent {

    private final LoanStatus finalStatus;
    private final String reason;

    public DecisionRecordedEvent(String loanId, String customerId, Instant occurredAt,
                                 LoanStatus finalStatus, String reason) {
        super(loanId, customerId, occurredAt);
        this.finalStatus = finalStatus;
        this.reason      = reason;
    }

    public LoanStatus getFinalStatus() { return finalStatus; }
    public String getReason()          { return reason; }
}
