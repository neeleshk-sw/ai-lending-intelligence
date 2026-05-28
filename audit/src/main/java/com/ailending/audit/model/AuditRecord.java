package com.ailending.audit.model;

import com.ailending.audit.exception.AuditException;

import java.time.Instant;
import java.util.UUID;

public final class AuditRecord {

    private final String auditId;
    private final String loanId;
    private final String customerId;
    private final AuditEventType eventType;
    private final Instant timestamp;
    private final String performedBy;
    private final String aiGeneratedText;
    private final Double confidenceScore;
    private final String modelUsed;
    private final Long aiDurationMs;
    private final Integer sourceReferenceCount;
    private final String decision;
    private final String notes;

    private AuditRecord(Builder builder) {
        this.auditId              = builder.auditId;
        this.loanId               = builder.loanId;
        this.customerId           = builder.customerId;
        this.eventType            = builder.eventType;
        this.timestamp            = builder.timestamp;
        this.performedBy          = builder.performedBy;
        this.aiGeneratedText      = builder.aiGeneratedText;
        this.confidenceScore      = builder.confidenceScore;
        this.modelUsed            = builder.modelUsed;
        this.aiDurationMs         = builder.aiDurationMs;
        this.sourceReferenceCount = builder.sourceReferenceCount;
        this.decision             = builder.decision;
        this.notes                = builder.notes;
    }

    public String getAuditId()              { return auditId; }
    public String getLoanId()               { return loanId; }
    public String getCustomerId()           { return customerId; }
    public AuditEventType getEventType()    { return eventType; }
    public Instant getTimestamp()           { return timestamp; }
    public String getPerformedBy()          { return performedBy; }
    public String getAiGeneratedText()      { return aiGeneratedText; }
    public Double getConfidenceScore()      { return confidenceScore; }
    public String getModelUsed()            { return modelUsed; }
    public Long getAiDurationMs()           { return aiDurationMs; }
    public Integer getSourceReferenceCount(){ return sourceReferenceCount; }
    public String getDecision()             { return decision; }
    public String getNotes()                { return notes; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String auditId;
        private String loanId;
        private String customerId;
        private AuditEventType eventType;
        private Instant timestamp;
        private String performedBy;
        private String aiGeneratedText;
        private Double confidenceScore;
        private String modelUsed;
        private Long aiDurationMs;
        private Integer sourceReferenceCount;
        private String decision;
        private String notes;

        public Builder loanId(String loanId)                            { this.loanId = loanId; return this; }
        public Builder customerId(String customerId)                    { this.customerId = customerId; return this; }
        public Builder eventType(AuditEventType eventType)              { this.eventType = eventType; return this; }
        public Builder timestamp(Instant timestamp)                     { this.timestamp = timestamp; return this; }
        public Builder performedBy(String performedBy)                  { this.performedBy = performedBy; return this; }
        public Builder aiGeneratedText(String aiGeneratedText)          { this.aiGeneratedText = aiGeneratedText; return this; }
        public Builder confidenceScore(Double confidenceScore)          { this.confidenceScore = confidenceScore; return this; }
        public Builder modelUsed(String modelUsed)                      { this.modelUsed = modelUsed; return this; }
        public Builder aiDurationMs(Long aiDurationMs)                  { this.aiDurationMs = aiDurationMs; return this; }
        public Builder sourceReferenceCount(Integer sourceReferenceCount){ this.sourceReferenceCount = sourceReferenceCount; return this; }
        public Builder decision(String decision)                        { this.decision = decision; return this; }
        public Builder notes(String notes)                              { this.notes = notes; return this; }

        public AuditRecord build() {
            if (loanId == null || loanId.isBlank()) {
                throw new AuditException("loanId is required");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new AuditException("customerId is required");
            }
            if (eventType == null) {
                throw new AuditException("eventType is required");
            }
            this.auditId   = UUID.randomUUID().toString();
            this.timestamp = (timestamp != null) ? timestamp : Instant.now();
            return new AuditRecord(this);
        }
    }
}
