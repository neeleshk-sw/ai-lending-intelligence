package com.ailending.app.controller.dto.query;

import com.ailending.aicore.vectorstore.SearchResult;

/**
 * A single retrieved evidence chunk included in a {@link QueryResponse}.
 * Satisfies NFR-5: every AI response must include source references and retrieved evidence metadata.
 */
public class HitDto {

    private final String content;
    private final double similarityScore;
    private final String documentType;
    private final String loanId;
    private final String customerId;
    private final String sourceDocumentId;

    private HitDto(String content, double similarityScore, String documentType,
                   String loanId, String customerId, String sourceDocumentId) {
        this.content          = content;
        this.similarityScore  = similarityScore;
        this.documentType     = documentType;
        this.loanId           = loanId;
        this.customerId       = customerId;
        this.sourceDocumentId = sourceDocumentId;
    }

    public static HitDto from(SearchResult result) {
        return new HitDto(
                result.getDocument().getContent(),
                result.getSimilarityScore(),
                result.getDocument().getDocumentType(),
                result.getDocument().getLoanId(),
                result.getDocument().getCustomerId(),
                result.getDocument().getSourceDocumentId()
        );
    }

    public String getContent()          { return content; }
    public double getSimilarityScore()  { return similarityScore; }
    public String getDocumentType()     { return documentType; }
    public String getLoanId()           { return loanId; }
    public String getCustomerId()       { return customerId; }
    public String getSourceDocumentId() { return sourceDocumentId; }
}
