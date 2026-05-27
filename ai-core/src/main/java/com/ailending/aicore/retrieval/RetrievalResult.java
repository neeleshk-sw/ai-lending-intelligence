package com.ailending.aicore.retrieval;

import com.ailending.aicore.vectorstore.SearchResult;
import java.util.List;

/**
 * Result DTO containing the matching context chunks and timing details.
 */
public class RetrievalResult {

    private final List<SearchResult> hits;
    private final long searchDurationMs;

    public RetrievalResult(List<SearchResult> hits, long searchDurationMs) {
        this.hits = hits;
        this.searchDurationMs = searchDurationMs;
    }

    public List<SearchResult> getHits() {
        return hits;
    }

    public long getSearchDurationMs() {
        return searchDurationMs;
    }
}
