package com.ailending.app.controller.dto.audit;

public class RecordAuditRequest {
    private String loanId;
    private String customerId;
    private String eventType;
    private String performedBy;
    private String aiGeneratedText;
    private Double confidenceScore;
    private String modelUsed;
    private Long aiDurationMs;
    private Integer sourceReferenceCount;
    private String decision;
    private String notes;

    public String getLoanId()                   { return loanId; }
    public void setLoanId(String loanId)        { this.loanId = loanId; }
    public String getCustomerId()               { return customerId; }
    public void setCustomerId(String v)         { this.customerId = v; }
    public String getEventType()                { return eventType; }
    public void setEventType(String v)          { this.eventType = v; }
    public String getPerformedBy()              { return performedBy; }
    public void setPerformedBy(String v)        { this.performedBy = v; }
    public String getAiGeneratedText()          { return aiGeneratedText; }
    public void setAiGeneratedText(String v)    { this.aiGeneratedText = v; }
    public Double getConfidenceScore()          { return confidenceScore; }
    public void setConfidenceScore(Double v)    { this.confidenceScore = v; }
    public String getModelUsed()                { return modelUsed; }
    public void setModelUsed(String v)          { this.modelUsed = v; }
    public Long getAiDurationMs()               { return aiDurationMs; }
    public void setAiDurationMs(Long v)         { this.aiDurationMs = v; }
    public Integer getSourceReferenceCount()    { return sourceReferenceCount; }
    public void setSourceReferenceCount(Integer v){ this.sourceReferenceCount = v; }
    public String getDecision()                 { return decision; }
    public void setDecision(String v)           { this.decision = v; }
    public String getNotes()                    { return notes; }
    public void setNotes(String v)              { this.notes = v; }
}
