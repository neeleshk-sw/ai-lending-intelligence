package com.ailending.app.controller;

import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmResponse;
import com.ailending.aicore.prompt.PromptBuilder;
import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.retrieval.RetrievalEngine;
import com.ailending.aicore.retrieval.RetrievalRequest;
import com.ailending.aicore.retrieval.RetrievalResult;
import com.ailending.aicore.vectorstore.SearchResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link QueryController}.
 *
 * <p>Uses standalone MockMvc + real {@link RagEngine} (wired with stubs) so the
 * request mapping, JSON serialization, and RagEngine orchestration are all exercised
 * without a Spring context, Ollama, or pgvector.
 */
class QueryControllerTest {

    private MockMvc mockMvc;
    private LlmProvider llmProvider;
    private StubRetrievalEngine retrievalEngine;

    @BeforeEach
    void setUp() {
        retrievalEngine = new StubRetrievalEngine();
        llmProvider     = mock(LlmProvider.class);
        RagEngine ragEngine = new RagEngine(retrievalEngine, new PromptBuilder(), llmProvider);

        // Templates are now owned by PromptTemplateConfig (lending module).
        // Register the ones exercised by these controller tests.
        ragEngine.registerTemplate("default", new com.ailending.aicore.prompt.PromptTemplate("default",
                "Context:\n{context}\n\nQuestion: {query}\nAnswer:"));
        ragEngine.registerTemplate("loan-summary", new com.ailending.aicore.prompt.PromptTemplate("loan-summary",
                "Summarise as JSON.\nContext:\n{context}\nJSON Output:"));
        ragEngine.registerTemplate("underwriting", new com.ailending.aicore.prompt.PromptTemplate("underwriting",
                "You are an expert underwriter.\nContext:\n{context}\nQuestion: {query}\nObservations:"));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new QueryController(ragEngine))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void query_returns200_withGeneratedText() throws Exception {
        prepareAvailableLlm("Income is stable at 80,000/month.");
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 40L));

        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the income?\",\"loanId\":\"loan-001\",\"customerId\":\"cust-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedText").value("Income is stable at 80,000/month."))
                .andExpect(jsonPath("$.confidenceScore").isNumber())
                .andExpect(jsonPath("$.model").isString())
                .andExpect(jsonPath("$.totalDurationMs").isNumber())
                .andExpect(jsonPath("$.hits").isArray());
    }

    @Test
    void query_hitsContainExpectedFields() throws Exception {
        prepareAvailableLlm("Answer text.");
        List<SearchResult> hits = twoHits();
        retrievalEngine.setResult(new RetrievalResult(hits, 30L));

        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"income?\",\"loanId\":\"loan-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits[0].content").isString())
                .andExpect(jsonPath("$.hits[0].similarityScore").isNumber())
                .andExpect(jsonPath("$.hits[0].documentType").value("SALARY_SLIP"));
    }

    @Test
    void query_withOptionalTemplateName_succeeds() throws Exception {
        prepareAvailableLlm("Summary.");
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L));

        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Compile summary.\",\"loanId\":\"loan-001\",\"templateName\":\"loan-summary\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void query_withoutTemplateName_defaultsToDefaultTemplate() throws Exception {
        prepareAvailableLlm("Default answer.");
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L));

        // templateName absent — must not cause a 4xx
        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the risk?\",\"loanId\":\"loan-001\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void query_emptyHits_returnsEmptyHitsArray() throws Exception {
        prepareAvailableLlm("No context found.");
        retrievalEngine.setResult(new RetrievalResult(List.of(), 5L));

        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"income?\",\"loanId\":\"loan-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits").isEmpty());
    }

    // -----------------------------------------------------------------------
    // Error path — LLM unavailable → 503
    // -----------------------------------------------------------------------

    @Test
    void query_llmUnavailable_returns503() throws Exception {
        when(llmProvider.isAvailable()).thenReturn(false);

        mockMvc.perform(post("/api/lending/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"any?\",\"loanId\":\"loan-001\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    private void prepareAvailableLlm(String text) {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(any(com.ailending.aicore.llm.LlmRequest.class)))
                .thenReturn(new LlmResponse(text, "llama3:8b", 50, 200L));
    }

    private List<SearchResult> twoHits() {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (int i = 0; i < 2; i++) {
            VectorDocument doc = VectorDocument.builder()
                    .id("doc-" + i)
                    .content("Chunk " + i + ": relevant content.")
                    .loanId("loan-001")
                    .customerId("cust-001")
                    .documentType("SALARY_SLIP")
                    .sourceDocumentId("src-" + i)
                    .build();
            results.add(new SearchResult(doc, 0.8 + i * 0.05));
        }
        return results;
    }

    // -----------------------------------------------------------------------
    // Hand-written stub for RetrievalEngine (avoids ByteBuddy issues on JVM 25)
    // -----------------------------------------------------------------------

    static class StubRetrievalEngine extends RetrievalEngine {
        private RetrievalResult stubbedResult = new RetrievalResult(List.of(), 0L);

        StubRetrievalEngine() { super(null); }

        void setResult(RetrievalResult result) { this.stubbedResult = result; }

        @Override
        public RetrievalResult retrieve(RetrievalRequest request) {
            return stubbedResult;
        }
    }
}
