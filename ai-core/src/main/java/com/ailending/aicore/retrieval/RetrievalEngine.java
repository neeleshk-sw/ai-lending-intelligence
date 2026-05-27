package com.ailending.aicore.retrieval;

import com.ailending.aicore.vectorstore.SearchRequest;
import com.ailending.aicore.vectorstore.SearchResult;
import com.ailending.aicore.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the retrieval phase of RAG.
 * Searches the vector store for relevant chunks using metadata filters.
 */
@Service
public class RetrievalEngine {

    private final VectorStore vectorStore;

    public RetrievalEngine(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Executes the retrieval process by generating search criteria and querying the vector store.
     *
     * @param request the retrieval request holding query and filtering parameters
     * @return the result containing matching chunks and performance duration
     */
    public RetrievalResult retrieve(RetrievalRequest request) {
        long startTime = System.currentTimeMillis();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuery())
                .topK(request.getTopK())
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .documentType(request.getDocumentType())
                .policyVersion(request.getPolicyVersion())
                .build();

        List<SearchResult> hits = vectorStore.search(searchRequest);
        long durationMs = System.currentTimeMillis() - startTime;

        return new RetrievalResult(hits, durationMs);
    }
}
