package com.ailending.aicore.retrieval;

/**
 * Request DTO for document retrieval.
 * Holds user query and metadata filters to query the vector store.
 */
public class RetrievalRequest {

    private final String query;
    private final String loanId;
    private final String customerId;
    private final String documentType;
    private final String policyVersion;
    private final int topK;

    private RetrievalRequest(Builder builder) {
        this.query = builder.query;
        this.loanId = builder.loanId;
        this.customerId = builder.customerId;
        this.documentType = builder.documentType;
        this.policyVersion = builder.policyVersion;
        this.topK = builder.topK;
    }

    public String getQuery() { return query; }
    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getPolicyVersion() { return policyVersion; }
    public int getTopK() { return topK; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private String loanId;
        private String customerId;
        private String documentType;
        private String policyVersion;
        private int topK = 4;

        public Builder query(String query) { this.query = query; return this; }
        public Builder loanId(String loanId) { this.loanId = loanId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder policyVersion(String policyVersion) { this.policyVersion = policyVersion; return this; }
        public Builder topK(int topK) { this.topK = topK; return this; }

        public RetrievalRequest build() {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query must not be null or blank");
            }
            return new RetrievalRequest(this);
        }
    }
}
