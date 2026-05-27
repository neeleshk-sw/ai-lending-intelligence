package com.ailending.aicore.vectorstore;

/**
 * DTO representing a search match returned by a vector store search.
 */
public class SearchResult {

    private final VectorDocument document;
    private final double similarityScore;

    public SearchResult(VectorDocument document, double similarityScore) {
        this.document = document;
        this.similarityScore = similarityScore;
    }

    public VectorDocument getDocument() {
        return document;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }
}
