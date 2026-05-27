package com.ailending.aicore.vectorstore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a document stored in the vector store.
 * Contains the original content, its vector embedding, and filterable metadata.
 */
public class VectorDocument {

    private final String id;
    private final String content;
    private final List<Double> embedding;

    // Metadata Design (TDD Section 9.4)
    private final String loanId;
    private final String customerId;
    private final String documentType;
    private final String sourceDocumentId;
    private final long ingestionTimestamp;
    private final int chunkSequence;
    private final String policyVersion;

    private VectorDocument(Builder builder) {
        this.id = builder.id;
        this.content = builder.content;
        this.embedding = builder.embedding;
        this.loanId = builder.loanId;
        this.customerId = builder.customerId;
        this.documentType = builder.documentType;
        this.sourceDocumentId = builder.sourceDocumentId;
        this.ingestionTimestamp = builder.ingestionTimestamp;
        this.chunkSequence = builder.chunkSequence;
        this.policyVersion = builder.policyVersion;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public List<Double> getEmbedding() { return embedding; }
    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getSourceDocumentId() { return sourceDocumentId; }
    public long getIngestionTimestamp() { return ingestionTimestamp; }
    public int getChunkSequence() { return chunkSequence; }
    public String getPolicyVersion() { return policyVersion; }

    /**
     * Converts metadata fields into a Map for Spring AI integration.
     */
    public Map<String, Object> getMetadataMap() {
        Map<String, Object> metadata = new HashMap<>();
        if (loanId != null) metadata.put("loanId", loanId);
        if (customerId != null) metadata.put("customerId", customerId);
        if (documentType != null) metadata.put("documentType", documentType);
        if (sourceDocumentId != null) metadata.put("sourceDocumentId", sourceDocumentId);
        metadata.put("ingestionTimestamp", ingestionTimestamp);
        metadata.put("chunkSequence", chunkSequence);
        if (policyVersion != null) metadata.put("policyVersion", policyVersion);
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent Builder for VectorDocument.
     */
    public static class Builder {
        private String id;
        private String content;
        private List<Double> embedding;
        private String loanId;
        private String customerId;
        private String documentType;
        private String sourceDocumentId;
        private long ingestionTimestamp = System.currentTimeMillis();
        private int chunkSequence = 0;
        private String policyVersion;

        public Builder id(String id) { this.id = id; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder embedding(List<Double> embedding) { this.embedding = embedding; return this; }
        public Builder loanId(String loanId) { this.loanId = loanId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder sourceDocumentId(String sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; return this; }
        public Builder ingestionTimestamp(long ingestionTimestamp) { this.ingestionTimestamp = ingestionTimestamp; return this; }
        public Builder chunkSequence(int chunkSequence) { this.chunkSequence = chunkSequence; return this; }
        public Builder policyVersion(String policyVersion) { this.policyVersion = policyVersion; return this; }

        public VectorDocument build() {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Content must not be null or blank");
            }
            return new VectorDocument(this);
        }
    }
}
