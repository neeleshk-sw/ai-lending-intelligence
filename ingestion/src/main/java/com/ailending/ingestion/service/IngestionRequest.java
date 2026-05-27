package com.ailending.ingestion.service;

/**
 * Input DTO for the document ingestion pipeline.
 * Carries raw document bytes plus all required vector metadata fields (TDD §9.4).
 */
public class IngestionRequest {

    private final byte[] content;
    private final String mimeType;
    private final String loanId;
    private final String customerId;
    private final String documentType;
    private final String sourceDocumentId;
    private final String policyVersion;

    private IngestionRequest(Builder builder) {
        this.content = builder.content;
        this.mimeType = builder.mimeType;
        this.loanId = builder.loanId;
        this.customerId = builder.customerId;
        this.documentType = builder.documentType;
        this.sourceDocumentId = builder.sourceDocumentId;
        this.policyVersion = builder.policyVersion;
    }

    public byte[] getContent() { return content; }
    public String getMimeType() { return mimeType; }
    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getSourceDocumentId() { return sourceDocumentId; }
    public String getPolicyVersion() { return policyVersion; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private byte[] content;
        private String mimeType = "application/octet-stream";
        private String loanId;
        private String customerId;
        private String documentType;
        private String sourceDocumentId;
        private String policyVersion;

        public Builder content(byte[] content) { this.content = content; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder loanId(String loanId) { this.loanId = loanId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder sourceDocumentId(String sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; return this; }
        public Builder policyVersion(String policyVersion) { this.policyVersion = policyVersion; return this; }

        public IngestionRequest build() {
            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("content must not be null or empty");
            }
            if (sourceDocumentId == null || sourceDocumentId.isBlank()) {
                throw new IllegalArgumentException("sourceDocumentId must not be blank");
            }
            return new IngestionRequest(this);
        }
    }
}
