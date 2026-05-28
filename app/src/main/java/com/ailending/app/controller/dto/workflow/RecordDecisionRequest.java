package com.ailending.app.controller.dto.workflow;

public class RecordDecisionRequest {
    private String finalStatus;
    private String reason;

    public String getFinalStatus()      { return finalStatus; }
    public void setFinalStatus(String v){ this.finalStatus = v; }
    public String getReason()           { return reason; }
    public void setReason(String v)     { this.reason = v; }
}
