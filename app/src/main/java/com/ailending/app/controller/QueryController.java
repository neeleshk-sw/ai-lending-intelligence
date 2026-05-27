package com.ailending.app.controller;

import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.rag.RagRequest;
import com.ailending.aicore.rag.RagResponse;
import com.ailending.app.controller.dto.query.QueryRequest;
import com.ailending.app.controller.dto.query.QueryResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for RAG-based document query.
 *
 * <p>Accepts a natural language question with optional metadata filters, retrieves
 * relevant document chunks from the vector store, and returns an AI-generated answer
 * together with full source evidence and confidence metadata (NFR-5).
 *
 * <p>The response is <b>advisory only</b> — no loan state is mutated (ADR-003).
 * All inference runs on organization-controlled infrastructure (ADR-006).
 */
@RestController
@RequestMapping("/api/lending")
public class QueryController {

    private final RagEngine ragEngine;

    public QueryController(RagEngine ragEngine) {
        this.ragEngine = ragEngine;
    }

    /**
     * Execute a RAG query against stored loan documents.
     *
     * @param request query parameters including the question and optional metadata filters
     * @return AI-generated answer with source references, confidence score, and timing (NFR-5)
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        RagRequest ragRequest = RagRequest.builder()
                .query(request.getQuery())
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .documentType(request.getDocumentType())
                .promptTemplateName(
                        request.getTemplateName() != null ? request.getTemplateName() : "default")
                .build();

        RagResponse ragResponse = ragEngine.execute(ragRequest);
        return QueryResponse.from(ragResponse);
    }
}
