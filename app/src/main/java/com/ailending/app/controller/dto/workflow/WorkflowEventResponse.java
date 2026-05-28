package com.ailending.app.controller.dto.workflow;

import com.ailending.workflow.event.WorkflowEvent;

import java.time.Instant;

public class WorkflowEventResponse {
    private final String eventType;
    private final String loanId;
    private final String customerId;
    private final Instant occurredAt;

    private WorkflowEventResponse(WorkflowEvent e) {
        this.eventType  = e.getClass().getSimpleName();
        this.loanId     = e.getLoanId();
        this.customerId = e.getCustomerId();
        this.occurredAt = e.getOccurredAt();
    }

    public static WorkflowEventResponse from(WorkflowEvent e) {
        return new WorkflowEventResponse(e);
    }

    public String getEventType()    { return eventType; }
    public String getLoanId()       { return loanId; }
    public String getCustomerId()   { return customerId; }
    public Instant getOccurredAt()  { return occurredAt; }
}
