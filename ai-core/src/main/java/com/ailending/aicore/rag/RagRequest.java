package com.ailending.aicore.rag;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for executing RAG orchestration.
 */
public class RagRequest {

    private final String query;
    private final String loanId;
    private final String customerId;
    private final String documentType;
    private final String policyVersion;
    private final String promptTemplateName;
    private final Map<String, Object> additionalVariables;

    private RagRequest(Builder builder) {
        this.query = builder.query;
        this.loanId = builder.loanId;
        this.customerId = builder.customerId;
        this.documentType = builder.documentType;
        this.policyVersion = builder.policyVersion;
        this.promptTemplateName = builder.promptTemplateName;
        this.additionalVariables = builder.additionalVariables;
    }

    public String getQuery() { return query; }
    public String getLoanId() { return loanId; }
    public String getCustomerId() { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getPolicyVersion() { return policyVersion; }
    public String getPromptTemplateName() { return promptTemplateName; }
    public Map<String, Object> getAdditionalVariables() { return additionalVariables; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent Builder for RagRequest.
     */
    public static class Builder {
        private String query;
        private String loanId;
        private String customerId;
        private String documentType;
        private String policyVersion;
        private String promptTemplateName = "default";
        private Map<String, Object> additionalVariables = new HashMap<>();

        public Builder query(String query) { this.query = query; return this; }
        public Builder loanId(String loanId) { this.loanId = loanId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder policyVersion(String policyVersion) { this.policyVersion = policyVersion; return this; }
        public Builder promptTemplateName(String promptTemplateName) { this.promptTemplateName = promptTemplateName; return this; }
        public Builder additionalVariables(Map<String, Object> additionalVariables) { this.additionalVariables = additionalVariables; return this; }

        public RagRequest build() {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query must not be null or blank");
            }
            return new RagRequest(this);
        }
    }
}
