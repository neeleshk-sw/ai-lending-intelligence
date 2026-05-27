package com.ailending.app.controller.dto.query;

/**
 * HTTP request body for {@code POST /api/lending/query}.
 */
public class QueryRequest {

    /** Natural language question to answer via RAG. Required. */
    private String query;

    /** Metadata filter — only chunks belonging to this loan are retrieved. Required. */
    private String loanId;

    /** Metadata filter — only chunks belonging to this customer are retrieved. Optional. */
    private String customerId;

    /** Metadata filter — restrict retrieval to a specific document type. Optional. */
    private String documentType;

    /** Prompt template name to use (default: {@code "default"}). Optional. */
    private String templateName;

    // Jackson no-arg constructor
    public QueryRequest() {}

    public String getQuery()        { return query; }
    public String getLoanId()       { return loanId; }
    public String getCustomerId()   { return customerId; }
    public String getDocumentType() { return documentType; }
    public String getTemplateName() { return templateName; }

    public void setQuery(String query)               { this.query = query; }
    public void setLoanId(String loanId)             { this.loanId = loanId; }
    public void setCustomerId(String customerId)     { this.customerId = customerId; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
}
