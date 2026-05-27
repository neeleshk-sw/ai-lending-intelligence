package com.ailending.app.providers.vectorstore;

import com.ailending.aicore.vectorstore.SearchRequest;
import com.ailending.aicore.vectorstore.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpringVectorStoreWrapper}.
 *
 * <p>Verifies that metadata filters on a {@link SearchRequest} are translated into the
 * correct Spring AI {@link Filter.Expression} objects before being forwarded to the
 * delegate {@link org.springframework.ai.vectorstore.VectorStore}.
 *
 * <p>The Spring AI delegate is mocked — no pgvector or Postgres is required.
 */
class SpringVectorStoreWrapperTest {

    /** Mocked Spring AI vector store — the real infrastructure dependency. */
    private org.springframework.ai.vectorstore.VectorStore delegate;
    private SpringVectorStoreWrapper wrapper;

    @BeforeEach
    void setUp() {
        delegate = mock(org.springframework.ai.vectorstore.VectorStore.class);
        wrapper  = new SpringVectorStoreWrapper(delegate);

        // Default: return an empty list so search() never throws on the result mapping path.
        when(delegate.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // Filter expression presence
    // -----------------------------------------------------------------------

    @Test
    void search_noFilterFields_passesNullFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .query("What is the repayment capacity?")
                .topK(5)
                // loanId, customerId, documentType, policyVersion all null
                .build();

        wrapper.search(request);

        org.springframework.ai.vectorstore.SearchRequest captured = captureSearchRequest();
        assertNull(captured.getFilterExpression(),
                "No metadata filters set → delegate must receive a null filter expression");
    }

    @Test
    void search_singleFilter_loanId_passesNonNullFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .query("income verification")
                .loanId("loan-001")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "A loanId filter must produce a non-null filter expression");
    }

    @Test
    void search_singleFilter_customerId_passesNonNullFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .query("bank statement")
                .customerId("cust-42")
                .topK(3)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "A customerId filter must produce a non-null filter expression");
    }

    @Test
    void search_singleFilter_documentType_passesNonNullFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .query("salary")
                .documentType("SALARY_SLIP")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "A documentType filter must produce a non-null filter expression");
    }

    @Test
    void search_singleFilter_policyVersion_passesNonNullFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .query("policy rule")
                .policyVersion("v2.1")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "A policyVersion filter must produce a non-null filter expression");
    }

    // -----------------------------------------------------------------------
    // Single EQ filter — type check
    // -----------------------------------------------------------------------

    @Test
    void search_singleFilter_expressionTypeIsEQ() {
        SearchRequest request = SearchRequest.builder()
                .query("risk factors")
                .loanId("loan-007")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr);
        assertEquals(Filter.ExpressionType.EQ, expr.type(),
                "A single metadata filter must produce an EQ expression");
    }

    @Test
    void search_singleFilter_expressionKeyMatchesField() {
        SearchRequest request = SearchRequest.builder()
                .query("underwriting notes")
                .loanId("loan-007")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr);
        Filter.Key key = (Filter.Key) expr.left();
        assertEquals("loanId", key.key(),
                "The EQ expression key must be 'loanId'");
        Filter.Value value = (Filter.Value) expr.right();
        assertEquals("loan-007", value.value(),
                "The EQ expression value must match the loanId supplied in the request");
    }

    // -----------------------------------------------------------------------
    // Multiple filters — AND composition
    // -----------------------------------------------------------------------

    @Test
    void search_twoFilters_expressionTypeIsAND() {
        SearchRequest request = SearchRequest.builder()
                .query("income")
                .loanId("loan-002")
                .customerId("cust-99")
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "Two filters must produce a non-null AND expression");
        assertEquals(Filter.ExpressionType.AND, expr.type(),
                "Two metadata filters must be composed with AND");
    }

    @Test
    void search_allFourFilters_expressionTypeIsAND() {
        SearchRequest request = SearchRequest.builder()
                .query("policy compliance")
                .loanId("loan-003")
                .customerId("cust-10")
                .documentType("POLICY_DOCUMENT")
                .policyVersion("v3.0")
                .topK(10)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr, "Four filters must produce a non-null AND expression");
        assertEquals(Filter.ExpressionType.AND, expr.type(),
                "Four metadata filters must be composed with AND");
    }

    // -----------------------------------------------------------------------
    // Null / blank values are omitted from the filter
    // -----------------------------------------------------------------------

    @Test
    void search_blankLoanId_treatedAsNoFilter() {
        // blank string → should not add an EQ predicate → filter expression should be null
        SearchRequest request = SearchRequest.builder()
                .query("income")
                .loanId("   ") // blank, not null
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNull(expr,
                "A blank loanId must be treated as 'no filter' — filter expression must be null");
    }

    @Test
    void search_oneNullOneSet_onlySetFieldProducesFilter() {
        // customerId is null → only loanId produces a filter → result is EQ, not AND
        SearchRequest request = SearchRequest.builder()
                .query("documents")
                .loanId("loan-100")
                // customerId not set → null
                .topK(5)
                .build();

        wrapper.search(request);

        Filter.Expression expr = captureSearchRequest().getFilterExpression();
        assertNotNull(expr);
        assertEquals(Filter.ExpressionType.EQ, expr.type(),
                "Only one non-null filter field → expression must be EQ, not AND");
    }

    // -----------------------------------------------------------------------
    // Delegate interaction
    // -----------------------------------------------------------------------

    @Test
    void store_delegatesToSpringAiAdd() {
        com.ailending.aicore.vectorstore.VectorDocument doc =
                com.ailending.aicore.vectorstore.VectorDocument.builder()
                        .id("doc-1")
                        .content("Sample content for storage test")
                        .loanId("loan-999")
                        .customerId("cust-999")
                        .documentType("BANK_STATEMENT")
                        .build();

        wrapper.store(List.of(doc));

        verify(delegate, times(1)).add(anyList());
    }

    @Test
    void search_topKIsForwardedToDelegate() {
        SearchRequest request = SearchRequest.builder()
                .query("any")
                .topK(7)
                .build();

        wrapper.search(request);

        org.springframework.ai.vectorstore.SearchRequest captured = captureSearchRequest();
        assertEquals(7, captured.getTopK(),
                "topK must be forwarded verbatim to the Spring AI SearchRequest");
    }

    @Test
    void search_emptyResultFromDelegate_returnsEmptyList() {
        when(delegate.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of());

        SearchRequest request = SearchRequest.builder().query("anything").topK(5).build();
        List<SearchResult> results = wrapper.search(request);

        assertTrue(results.isEmpty(), "Empty delegate response must produce an empty result list");
    }

    @Test
    void search_delegateResultIsMappedToSearchResult() {
        Document springDoc = new Document(
                "doc-id-1",
                "Borrower earns 80,000 per month",
                Map.of(
                        "loanId",           "loan-55",
                        "customerId",       "cust-55",
                        "documentType",     "SALARY_SLIP",
                        "sourceDocumentId", "src-1",
                        "policyVersion",    "v1",
                        "ingestionTimestamp", 1700000000L,
                        "chunkSequence",    0,
                        "distance",         0.2   // similarity = 1 - 0.2 = 0.8
                )
        );
        when(delegate.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(springDoc));

        SearchRequest request = SearchRequest.builder().query("monthly income").topK(5).build();
        List<SearchResult> results = wrapper.search(request);

        assertEquals(1, results.size());
        SearchResult result = results.get(0);
        assertEquals("doc-id-1",    result.getDocument().getId());
        assertEquals("loan-55",     result.getDocument().getLoanId());
        assertEquals("cust-55",     result.getDocument().getCustomerId());
        assertEquals("SALARY_SLIP", result.getDocument().getDocumentType());
        assertEquals(0.8,           result.getSimilarityScore(), 1e-9,
                "Similarity score must be computed as 1 - distance");
    }

    // -----------------------------------------------------------------------
    // Private helper
    // -----------------------------------------------------------------------

    private org.springframework.ai.vectorstore.SearchRequest captureSearchRequest() {
        ArgumentCaptor<org.springframework.ai.vectorstore.SearchRequest> captor =
                ArgumentCaptor.forClass(org.springframework.ai.vectorstore.SearchRequest.class);
        // Cast to the correct overload — VectorStore also has similaritySearch(String),
        // so the explicit cast is required to resolve the ambiguity for the compiler.
        verify(delegate).similaritySearch(
                (org.springframework.ai.vectorstore.SearchRequest) captor.capture());
        return captor.getValue();
    }
}
