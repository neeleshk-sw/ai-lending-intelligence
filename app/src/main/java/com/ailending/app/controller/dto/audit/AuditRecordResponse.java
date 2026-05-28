package com.ailending.app.controller.dto.audit;

import com.ailending.audit.model.AuditRecord;

import java.time.Instant;

public class AuditRecordResponse {
    private final String auditId;
    private final String loanId;
    private final String customerId;
    private final String eventType;
    private final Instant timestamp;
    private final String performedBy;
    private final String aiGeneratedText;
    private final Double confidenceScore;
    private final String modelUsed;
    private final Long aiDurationMs;
    private final Integer sourceReferenceCount;
    private final String decision;
    private final String notes;

    private AuditRecordResponse(AuditRecord r) {
        this.auditId              = r.getAuditId();
        this.loanId               = r.getLoanId();
        this.customerId           = r.getCustomerId();
        this.eventType            = r.getEventType().name();
        this.timestamp            = r.getTimestamp();
        this.performedBy          = r.getPerformedBy();
        this.aiGeneratedText      = r.getAiGeneratedText();
        this.confidenceScore      = r.getConfidenceScore();
        this.modelUsed            = r.getModelUsed();
        this.aiDurationMs         = r.getAiDurationMs();
        this.sourceReferenceCount = r.getSourceReferenceCount();
        this.decision             = r.getDecision();
        this.notes                = r.getNotes();
    }

    public static AuditRecordResponse from(AuditRecord r) {
        return new AuditRecordResponse(r);
    }

    public String getAuditId()               { return auditId; }
    public String getLoanId()                { return loanId; }
    public String getCustomerId()            { return customerId; }
    public String getEventType()             { return eventType; }
    public Instant getTimestamp()            { return timestamp; }
    public String getPerformedBy()           { return performedBy; }
    public String getAiGeneratedText()       { return aiGeneratedText; }
    public Double getConfidenceScore()       { return confidenceScore; }
    public String getModelUsed()             { return modelUsed; }
    public Long getAiDurationMs()            { return aiDurationMs; }
    public Integer getSourceReferenceCount() { return sourceReferenceCount; }
    public String getDecision()              { return decision; }
    public String getNotes()                 { return notes; }
}
