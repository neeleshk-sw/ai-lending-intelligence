package com.ailending.policy.service;

import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmResponse;
import com.ailending.aicore.prompt.PromptBuilder;
import com.ailending.aicore.prompt.PromptTemplate;
import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.rag.RagResponse;
import com.ailending.aicore.retrieval.RetrievalEngine;
import com.ailending.aicore.retrieval.RetrievalRequest;
import com.ailending.aicore.retrieval.RetrievalResult;
import com.ailending.policy.exception.PolicyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PolicyQueryServiceImpl}.
 *
 * <p>Uses a real {@link RagEngine} wired with a {@link StubRetrievalEngine} and a mocked
 * {@link LlmProvider} — the same pattern used in QueryControllerTest and
 * RiskAnalysisServiceImplTest to avoid ByteBuddy inline-mocking issues on JVM 17+.
 */
class PolicyQueryServiceImplTest {

    private LlmProvider llmProvider;
    private StubRetrievalEngine retrievalEngine;
    private RagEngine ragEngine;
    private PolicyQueryService service;

    @BeforeEach
    void setUp() {
        retrievalEngine = new StubRetrievalEngine();
        llmProvider     = mock(LlmProvider.class);
        ragEngine       = new RagEngine(retrievalEngine, new PromptBuilder(), llmProvider);
        ragEngine.registerTemplate("policy-qa", new PromptTemplate("policy-qa",
                "Answer policy questions.\nContext:\n{context}\nQuestion: {query}\nAnswer:"));
        service = new PolicyQueryServiceImpl(ragEngine);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void query_returnsRagResponse_withGeneratedText() {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(any())).thenReturn(
                new LlmResponse("Maximum DTI is 45%.", "llama3:8b", 10, 100L));

        RagResponse result = service.query("What is the maximum DTI?", "v2.0");

        assertNotNull(result);
        assertEquals("Maximum DTI is 45%.", result.getAnswer());
    }

    @Test
    void query_ragEngineReceivesCorrectFilters() {
        when(llmProvider.isAvailable()).thenReturn(true);
        when(llmProvider.generate(any())).thenReturn(
                new LlmResponse("Answer.", "llama3:8b", 5, 50L));

        service.query("What is the minimum credit score?", "v3.0");

        // RagEngine converts RagRequest → RetrievalRequest before calling the engine;
        // verifying the RetrievalRequest fields confirms the metadata filters were wired.
        RetrievalRequest captured = retrievalEngine.getCapturedRequest();
        assertNotNull(captured, "RetrievalEngine should have been invoked");
        assertEquals("What is the minimum credit score?", captured.getQuery());
        assertEquals("POLICY_DOCUMENT", captured.getDocumentType(), "documentType filter must be POLICY_DOCUMENT");
        assertEquals("v3.0", captured.getPolicyVersion(), "policyVersion filter must be set");
    }

    @Test
    void query_llmUnavailable_throwsIllegalStateException() {
        when(llmProvider.isAvailable()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.query("Some policy question?", "v1.0"));
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    void query_blankQuestion_throwsPolicyException() {
        assertThrows(PolicyException.class, () -> service.query("  ", "v1.0"));
    }

    @Test
    void query_nullQuestion_throwsPolicyException() {
        assertThrows(PolicyException.class, () -> service.query(null, "v1.0"));
    }

    @Test
    void query_blankPolicyVersion_throwsPolicyException() {
        assertThrows(PolicyException.class, () -> service.query("Some question?", ""));
    }

    // -----------------------------------------------------------------------
    // Stub inner class — avoids ByteBuddy inline-mocking on JVM 17+
    // -----------------------------------------------------------------------

    private static class StubRetrievalEngine extends RetrievalEngine {
        private RetrievalRequest capturedRequest;

        StubRetrievalEngine() {
            super(null);
        }

        @Override
        public RetrievalResult retrieve(RetrievalRequest request) {
            this.capturedRequest = request;
            return new RetrievalResult(Collections.emptyList(), 0L);
        }

        public RetrievalRequest getCapturedRequest() {
            return capturedRequest;
        }
    }
}
