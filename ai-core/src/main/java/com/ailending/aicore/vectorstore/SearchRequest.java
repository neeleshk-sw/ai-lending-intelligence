package com.ailending.aicore.vectorstore;

import java.util.List;

/**
 * Request DTO for vector store searches.
 * Carries search query vector, top-K limit, and metadata filter criteria.
 */
public class SearchRequest {

    private final String query;
    private final List<Double> queryVector;
    private final int topK;

    // Filters
    private final String loanId;
    private final String customerId;
    private final String documentType;
    private final String policyVersion;

    private SearchRequest(Builder builder) {
        this.query = builder.query;
        this.queryVector = builder.queryVector;
        this.topK = builder.topK;
        this.loanId = builder.loanId;
        this.customerId = builder.customerId;
        this.documentType = builder.documentType;
        this.policyVersion = builder.policyVersion;
    }

    public String getQuery() { return query; }
    public List<Double> getQueryVector() { return queryVector; }
    public int getTopK() { return topK; }
    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getPolicyVersion() { return policyVersion; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private List<Double> queryVector;
        private int topK = 4;
        private String loanId;
        private String customerId;
        private String documentType;
        private String policyVersion;

        public Builder query(String query) { this.query = query; return this; }
        public Builder queryVector(List<Double> queryVector) { this.queryVector = queryVector; return this; }
        public Builder topK(int topK) { this.topK = topK; return this; }
        public Builder loanId(String loanId) { this.loanId = loanId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder policyVersion(String policyVersion) { this.policyVersion = policyVersion; return this; }

        public SearchRequest build() {
            return new SearchRequest(this);
        }
    }
}
