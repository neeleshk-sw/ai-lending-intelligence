package com.ailending.lending.service;

import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmResponse;
import com.ailending.aicore.prompt.PromptBuilder;
import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.rag.RagResponse;
import com.ailending.aicore.retrieval.RetrievalEngine;
import com.ailending.aicore.retrieval.RetrievalRequest;
import com.ailending.aicore.retrieval.RetrievalResult;
import com.ailending.aicore.vectorstore.SearchResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RiskAnalysisServiceImpl}.
 *
 * <p>Uses a hand-written {@link StubRetrievalEngine} (concrete class — avoids ByteBuddy
 * inline-mock issues on JVM 17+/25) and Mockito-mocked {@link LlmProvider} (interface).
 * A real {@link PromptBuilder} and {@link RagEngine} are used to keep the test realistic
 * without requiring Ollama or any infrastructure.
 */
class RiskAnalysisServiceImplTest {

    private StubRetrievalEngine retrievalEngine;
    private LlmProvider llmProvider;
    private RagEngine ragEngine;
    private RiskAnalysisServiceImpl service;

    private static final String CANNED_ANSWER = "Moderate risk: borrower income is stable but debt ratio is elevated.";
    private static final String CANNED_MODEL  = "llama3:8b";
    private static final int    CANNED_TOKENS = 80;
    private static final long   LLM_DURATION  = 300L;

    @BeforeEach
    void setUp() {
        retrievalEngine = new StubRetrievalEngine();
        llmProvider     = mock(LlmProvider.class);

        ragEngine = new RagEngine(retrievalEngine, new PromptBuilder(), llmProvider);

        // Templates are now registered externally (PromptTemplateConfig in lending).
        // Register the templates needed by these tests directly so no Spring context
        // or PromptTemplateConfig is required at test time.
        ragEngine.registerTemplate("default", new com.ailending.aicore.prompt.PromptTemplate("default",
                "Context:\n{context}\n\nQuestion: {query}\nAnswer:"));
        ragEngine.registerTemplate("underwriting", new com.ailending.aicore.prompt.PromptTemplate("underwriting",
                "You are an expert underwriter.\nContext:\n{context}\nQuestion: {query}\nUnderwriting Observations:"));

        service = new RiskAnalysisServiceImpl(ragEngine);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void analyzeRisk_returnsRagResponse() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        RagResponse response = service.analyzeRisk("loan-001", "cust-001");

        assertNotNull(response, "analyzeRisk must return a non-null RagResponse");
    }

    @Test
    void analyzeRisk_responseContainsGeneratedText() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        RagResponse response = service.analyzeRisk("loan-001", "cust-001");

        assertEquals(CANNED_ANSWER, response.getAnswer(),
                "RagResponse answer must be what the LLM returned");
    }

    @Test
    void analyzeRisk_responseHasNonNullSourceReferences() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        RagResponse response = service.analyzeRisk("loan-001", "cust-001");

        assertNotNull(response.getSourceReferences(),
                "RagResponse must include source references (NFR-5)");
    }

    @Test
    void analyzeRisk_responseHasNonNegativeConfidence() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        RagResponse response = service.analyzeRisk("loan-001", "cust-001");

        assertTrue(response.getConfidenceScore() >= 0.0 && response.getConfidenceScore() <= 1.0,
                "Confidence score must be in [0, 1] (NFR-5)");
    }

    @Test
    void analyzeRisk_responseHasNonNegativeDuration() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        RagResponse response = service.analyzeRisk("loan-001", "cust-001");

        assertTrue(response.getDurationMs() >= 0,
                "durationMs must be non-negative (NFR-5)");
    }

    // -----------------------------------------------------------------------
    // Correct RAG request construction
    // -----------------------------------------------------------------------

    @Test
    void analyzeRisk_forwardsLoanIdToRagEngine() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L));

        service.analyzeRisk("loan-XYZ", "cust-ABC");

        assertEquals("loan-XYZ", retrievalEngine.getLastRequest().getLoanId(),
                "loanId must be forwarded to the RAG retrieval engine");
    }

    @Test
    void analyzeRisk_forwardsCustomerIdToRagEngine() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L));

        service.analyzeRisk("loan-XYZ", "cust-ABC");

        assertEquals("cust-ABC", retrievalEngine.getLastRequest().getCustomerId(),
                "customerId must be forwarded to the RAG retrieval engine");
    }

    @Test
    void analyzeRisk_usesUnderwritingTemplate() {
        prepareAvailableLlm(CANNED_ANSWER);

        // Capture the LlmRequest to verify the underwriting template was used.
        // The "underwriting" template prompt text includes "underwriter" — check it.
        final List<String> capturedPrompts = new ArrayList<String>();
        when(llmProvider.generate(any(com.ailending.aicore.llm.LlmRequest.class)))
                .thenAnswer(invocation -> {
                    com.ailending.aicore.llm.LlmRequest req = invocation.getArgument(0);
                    capturedPrompts.add(req.getPrompt());
                    return new LlmResponse(CANNED_ANSWER, CANNED_MODEL, CANNED_TOKENS, LLM_DURATION);
                });
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L));

        service.analyzeRisk("loan-001", "cust-001");

        assertEquals(1, capturedPrompts.size(), "LLM must be called exactly once");
        assertTrue(capturedPrompts.get(0).toLowerCase().contains("underwriter"),
                "The prompt must use the 'underwriting' template (must contain 'underwriter')");
    }

    @Test
    void analyzeRisk_callsRagEngineExactlyOnce() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        service.analyzeRisk("loan-001", "cust-001");

        assertEquals(1, retrievalEngine.getCallCount(),
                "RetrievalEngine must be called exactly once per analyzeRisk invocation");
    }

    // -----------------------------------------------------------------------
    // Graceful degradation — LLM offline
    // -----------------------------------------------------------------------

    @Test
    void analyzeRisk_throwsIllegalStateException_whenLlmUnavailable() {
        when(llmProvider.isAvailable()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.analyzeRisk("loan-001", "cust-001"),
                "analyzeRisk must propagate IllegalStateException from RagEngine when LLM is offline");
    }

    // -----------------------------------------------------------------------
    // Advisory-only enforcement
    // -----------------------------------------------------------------------

    @Test
    void analyzeRisk_doesNotThrow_forValidInput() {
        prepareAvailableLlm(CANNED_ANSWER);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 30L));

        // Should not throw — no status updates attempted
        assertDoesNotThrow(() -> service.analyzeRisk("loan-001", "cust-001"),
                "analyzeRisk must not throw for valid inputs when LLM is available");
    }

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    private void prepareAvailableLlm(String text) {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(any(com.ailending.aicore.llm.LlmRequest.class)))
                .thenReturn(new LlmResponse(text, CANNED_MODEL, CANNED_TOKENS, LLM_DURATION));
    }

    private List<SearchResult> twoHits() {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (int i = 0; i < 2; i++) {
            VectorDocument doc = VectorDocument.builder()
                    .id("doc-" + i)
                    .content("Chunk " + i + ": relevant loan excerpt.")
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
    // Hand-written stub — avoids ByteBuddy inline-mock issues on JVM 17+/25
    // -----------------------------------------------------------------------

    /**
     * Minimal stub for {@link RetrievalEngine} — records invocations and returns
     * a preconfigured {@link RetrievalResult} without touching any real infrastructure.
     */
    static class StubRetrievalEngine extends RetrievalEngine {

        private RetrievalResult stubbedResult = new RetrievalResult(List.of(), 0L);
        private RetrievalRequest lastRequest;
        private int callCount = 0;

        StubRetrievalEngine() {
            // Parent constructor requires a VectorStore; null is safe here because
            // we override retrieve() completely — the parent field is never accessed.
            super(null);
        }

        void setResult(RetrievalResult result) {
            this.stubbedResult = result;
        }

        RetrievalRequest getLastRequest() {
            return lastRequest;
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public RetrievalResult retrieve(RetrievalRequest request) {
            this.lastRequest = request;
            this.callCount++;
            return stubbedResult;
        }
    }
}
