package com.ailending.aicore.rag;

import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmRequest;
import com.ailending.aicore.llm.LlmResponse;
import com.ailending.aicore.prompt.PromptBuilder;
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
 * Unit tests for {@link RagEngine}.
 *
 * <p>{@link RetrievalEngine} is a concrete Spring-managed class, so it is replaced by a
 * hand-written {@link StubRetrievalEngine} inner class instead of a Mockito mock — this
 * avoids ByteBuddy inline-instrumentation issues on newer JVMs.
 * {@link LlmProvider} is an interface and is safely mocked via the standard JDK proxy.
 * {@link PromptBuilder} is a real instance (no infrastructure dependencies).
 *
 * <p>No Ollama, no pgvector, no Spring context required.
 */
class RagEngineTest {

    private StubRetrievalEngine retrievalEngine;
    private LlmProvider llmProvider;
    private RagEngine ragEngine;

    // Shared canned fixtures
    private static final String CANNED_TEXT   = "The borrower has stable employment and adequate repayment capacity.";
    private static final String CANNED_MODEL  = "llama3:8b";
    private static final int    CANNED_TOKENS = 120;
    private static final long   LLM_DURATION  = 400L;

    @BeforeEach
    void setUp() {
        retrievalEngine = new StubRetrievalEngine();
        llmProvider     = mock(LlmProvider.class);

        // RagEngine uses a real PromptBuilder (no deps) — avoids over-mocking.
        ragEngine = new RagEngine(retrievalEngine, new PromptBuilder(), llmProvider);

        // Templates are no longer registered by RagEngine itself (moved to
        // PromptTemplateConfig in the lending module). Register them here so
        // tests that drive execute() through the full template path still work.
        ragEngine.registerTemplate("default", new com.ailending.aicore.prompt.PromptTemplate("default",
                "Context:\n{context}\n\nQuestion: {query}\nAnswer:"));
        ragEngine.registerTemplate("loan-summary", new com.ailending.aicore.prompt.PromptTemplate("loan-summary",
                "Summarise as JSON.\nContext:\n{context}\nJSON Output:"));
        ragEngine.registerTemplate("underwriting", new com.ailending.aicore.prompt.PromptTemplate("underwriting",
                "You are an expert underwriter.\nContext:\n{context}\nQuestion: {query}\nUnderwriting Observations:"));
        ragEngine.registerTemplate("policy-qa", new com.ailending.aicore.prompt.PromptTemplate("policy-qa",
                "Policy assistant.\nContext:\n{context}\nQuestion: {query}\nAnswer:"));
    }

    // -----------------------------------------------------------------------
    // Happy path — RagResponse fields
    // -----------------------------------------------------------------------

    @Test
    void execute_returnsGeneratedTextFromLlm() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(CANNED_TEXT, response.getAnswer(),
                "generatedText must be exactly what the LLM returned");
    }

    @Test
    void execute_returnsSourceReferencesFromRetrieval() {
        List<SearchResult> hits = twoHits();
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(hits, 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(hits, response.getSourceReferences(),
                "sourceReferences must be the exact hit list returned by RetrievalEngine");
    }

    @Test
    void execute_returnsModelNameFromLlmResponse() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(CANNED_MODEL, response.getModelUsed(),
                "modelUsed must be propagated from the LlmResponse");
    }

    @Test
    void execute_totalDurationMsIsNonNegative() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertTrue(response.getDurationMs() >= 0,
                "totalDurationMs must be non-negative");
    }

    // -----------------------------------------------------------------------
    // Confidence score calculation
    // -----------------------------------------------------------------------

    @Test
    void execute_confidenceScore_isAverageOfHitScores() {
        // Two hits with scores 0.6 and 0.8 → average = 0.7
        List<SearchResult> hits = hitsWithScores(0.6, 0.8);
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(hits, 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(0.7, response.getConfidenceScore(), 1e-9,
                "confidenceScore must be the average similarity score of the retrieved hits");
    }

    @Test
    void execute_confidenceScore_clampedToOne_whenAverageExceedsOne() {
        // Artificially high scores — average 1.0 must be the ceiling
        List<SearchResult> hits = hitsWithScores(0.95, 1.05);
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(hits, 50L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertTrue(response.getConfidenceScore() <= 1.0,
                "confidenceScore must be clamped to [0, 1]");
    }

    @Test
    void execute_confidenceScore_isZero_whenNoHitsReturned() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(List.of(), 10L)); // empty

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(0.0, response.getConfidenceScore(), 1e-9,
                "confidenceScore must be 0.0 when no context documents are retrieved");
    }

    @Test
    void execute_confidenceScore_isSingleHitScore_forOneHit() {
        List<SearchResult> hits = hitsWithScores(0.82);
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(hits, 30L));

        RagResponse response = ragEngine.execute(defaultRequest());

        assertEquals(0.82, response.getConfidenceScore(), 1e-9,
                "confidenceScore for a single hit must equal that hit's similarity score");
    }

    // -----------------------------------------------------------------------
    // Template selection
    // -----------------------------------------------------------------------

    @Test
    void execute_usesDefaultTemplate_withoutThrowing() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        assertDoesNotThrow(() -> ragEngine.execute(defaultRequest()),
                "Executing with the 'default' template must not throw");
    }

    @Test
    void execute_fallsBackToDefaultTemplate_forUnknownTemplateName() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagRequest request = RagRequest.builder()
                .query("What is the borrower's income?")
                .loanId("loan-001")
                .promptTemplateName("non-existent-template")
                .build();

        // Must not throw — unknown template name falls back to "default"
        assertDoesNotThrow(() -> ragEngine.execute(request),
                "Unknown template name must fall back to 'default' without throwing");
    }

    @Test
    void execute_loanSummaryTemplate_doesNotThrow() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagRequest request = RagRequest.builder()
                .query("Compile the loan summary.")
                .loanId("loan-001")
                .promptTemplateName("loan-summary")
                .build();

        assertDoesNotThrow(() -> ragEngine.execute(request),
                "loan-summary template must be available and must not throw");
    }

    @Test
    void execute_underwritingTemplate_doesNotThrow() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagRequest request = RagRequest.builder()
                .query("Provide an underwriting risk assessment.")
                .loanId("loan-001")
                .promptTemplateName("underwriting")
                .build();

        assertDoesNotThrow(() -> ragEngine.execute(request));
    }

    // -----------------------------------------------------------------------
    // Graceful degradation — LLM unavailable
    // -----------------------------------------------------------------------

    @Test
    void execute_throwsIllegalStateException_whenLlmProviderIsUnavailable() {
        when(llmProvider.isAvailable()).thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> ragEngine.execute(defaultRequest()),
                "execute() must throw IllegalStateException when the LLM is offline"
        );

        assertNotNull(ex.getMessage(), "Exception message must not be null");
    }

    @Test
    void execute_doesNotCallRetrievalEngine_whenLlmProviderIsUnavailable() {
        when(llmProvider.isAvailable()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> ragEngine.execute(defaultRequest()));

        assertEquals(0, retrievalEngine.getCallCount(),
                "RetrievalEngine must NOT be called once the LLM is known to be offline");
    }

    @Test
    void execute_doesNotCallLlmGenerate_whenProviderIsUnavailable() {
        when(llmProvider.isAvailable()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> ragEngine.execute(defaultRequest()));

        verify(llmProvider, never()).generate(any(LlmRequest.class));
    }

    // -----------------------------------------------------------------------
    // registerTemplate / template-registry API
    // -----------------------------------------------------------------------

    @Test
    void execute_throwsIllegalStateException_whenNoMatchingTemplateAndNoDefault() {
        // Create a fresh engine with no templates registered at all.
        RagEngine freshEngine = new RagEngine(retrievalEngine, new PromptBuilder(), llmProvider);
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> freshEngine.execute(defaultRequest()),
                "execute() must throw when no template is registered and no 'default' fallback exists"
        );
        assertNotNull(ex.getMessage(), "Exception message must not be null");
        assertTrue(ex.getMessage().contains("registerTemplate"),
                "Exception message should guide callers to registerTemplate()");
    }

    @Test
    void registerTemplate_throwsIllegalArgument_whenNameIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> ragEngine.registerTemplate("  ", new com.ailending.aicore.prompt.PromptTemplate("x", "t")),
                "Blank template name must be rejected");
    }

    @Test
    void registerTemplate_throwsIllegalArgument_whenTemplateIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ragEngine.registerTemplate("my-template", null),
                "Null template must be rejected");
    }

    @Test
    void registerTemplate_overwritesExistingEntry() {
        // Register "default" again — should not throw; new template takes effect.
        ragEngine.registerTemplate("default", new com.ailending.aicore.prompt.PromptTemplate("default",
                "Overwritten: Context:\n{context}\nQuestion: {query}\nAnswer:"));
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        assertDoesNotThrow(() -> ragEngine.execute(defaultRequest()),
                "Re-registering a template name must overwrite the existing entry without throwing");
    }

    // -----------------------------------------------------------------------
    // RetrievalEngine delegation
    // -----------------------------------------------------------------------

    @Test
    void execute_callsRetrievalEngineExactlyOnce() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        ragEngine.execute(defaultRequest());

        assertEquals(1, retrievalEngine.getCallCount(),
                "RetrievalEngine.retrieve() must be called exactly once per RAG execution");
    }

    @Test
    void execute_forwardsQueryToRetrievalEngine() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        String query = "Is the income sufficient for loan repayment?";
        RagRequest request = RagRequest.builder()
                .query(query)
                .loanId("loan-XYZ")
                .build();

        ragEngine.execute(request);

        assertEquals(query, retrievalEngine.getLastRequest().getQuery(),
                "The query in the RagRequest must be forwarded to RetrievalEngine");
    }

    @Test
    void execute_forwardsLoanIdToRetrievalEngine() {
        prepareAvailableLlm(CANNED_TEXT);
        retrievalEngine.setResult(new RetrievalResult(twoHits(), 50L));

        RagRequest request = RagRequest.builder()
                .query("What is the repayment capacity?")
                .loanId("loan-XYZ")
                .customerId("cust-ABC")
                .build();

        ragEngine.execute(request);

        assertEquals("loan-XYZ", retrievalEngine.getLastRequest().getLoanId(),
                "loanId must be forwarded from RagRequest to RetrievalRequest");
    }

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    private RagRequest defaultRequest() {
        return RagRequest.builder()
                .query("What are the main risk factors for this loan application?")
                .loanId("loan-001")
                .customerId("cust-001")
                .build();
    }

    private void prepareAvailableLlm(String generatedText) {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(any(LlmRequest.class)))
                .thenReturn(new LlmResponse(generatedText, CANNED_MODEL, CANNED_TOKENS, LLM_DURATION));
    }

    /** Returns two SearchResult fixtures with distinct similarity scores. */
    private List<SearchResult> twoHits() {
        return hitsWithScores(0.90, 0.75);
    }

    /** Returns a list of SearchResult fixtures with the given similarity scores. */
    private List<SearchResult> hitsWithScores(double... scores) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (int i = 0; i < scores.length; i++) {
            VectorDocument doc = VectorDocument.builder()
                    .id("doc-" + i)
                    .content("Context chunk " + i + ": relevant loan document excerpt.")
                    .loanId("loan-001")
                    .customerId("cust-001")
                    .documentType("SALARY_SLIP")
                    .sourceDocumentId("src-" + i)
                    .build();
            results.add(new SearchResult(doc, scores[i]));
        }
        return results;
    }

    // -----------------------------------------------------------------------
    // Hand-written stub — avoids ByteBuddy inline-mock issues on JVM 17+/25
    // -----------------------------------------------------------------------

    /**
     * Minimal stub for {@link RetrievalEngine} that records invocations and
     * returns a pre-configured {@link RetrievalResult} without touching any
     * real infrastructure.
     */
    static class StubRetrievalEngine extends RetrievalEngine {

        private RetrievalResult stubbedResult = new RetrievalResult(List.of(), 0L);
        private RetrievalRequest lastRequest;
        private int callCount = 0;

        StubRetrievalEngine() {
            // Parent constructor requires a VectorStore; null is safe because
            // we override retrieve() completely — the parent field is never used.
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
