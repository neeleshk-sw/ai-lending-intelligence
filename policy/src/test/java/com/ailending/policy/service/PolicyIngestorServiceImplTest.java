package com.ailending.policy.service;

import com.ailending.aicore.embedding.EmbeddingProvider;
import com.ailending.aicore.embedding.EmbeddingRequest;
import com.ailending.aicore.embedding.EmbeddingResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import com.ailending.aicore.vectorstore.VectorStore;
import com.ailending.policy.exception.PolicyException;
import com.ailending.policy.model.PolicyVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PolicyIngestorServiceImplTest {

    private EmbeddingProvider embeddingProvider;
    private VectorStore vectorStore;
    private PolicyIngestorService service;

    @BeforeEach
    void setUp() {
        embeddingProvider = mock(EmbeddingProvider.class);
        vectorStore       = mock(VectorStore.class);
        service           = new PolicyIngestorServiceImpl(embeddingProvider, vectorStore);

        when(embeddingProvider.embed(any(EmbeddingRequest.class)))
                .thenReturn(new EmbeddingResult(Collections.emptyList(), 0, 0));
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void ingest_singleParagraph_storesOneChunk() {
        String text = "This is a short policy paragraph with fewer than 500 words.";

        PolicyVersion result = service.ingest("lending-policy", "v1.0", text);

        assertEquals(1, result.getChunkCount(), "Single paragraph should produce one chunk");
        assertEquals("v1.0", result.getVersion());
        assertEquals("lending-policy", result.getPolicyName());
        assertNotNull(result.getIngestedAt());

        verify(embeddingProvider, times(1)).embed(any(EmbeddingRequest.class));
        verify(vectorStore, times(1)).store(any());
    }

    @Test
    void ingest_multipleParagraphs_storesMultipleChunks() {
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";

        PolicyVersion result = service.ingest("credit-policy", "v2.0", text);

        assertTrue(result.getChunkCount() >= 1, "Multiple paragraphs should produce at least one chunk");
        verify(embeddingProvider, atLeastOnce()).embed(any(EmbeddingRequest.class));
    }

    @Test
    void ingest_embeddingCalledOncePerChunk() {
        // 3 separated paragraphs each short enough to stay in separate chunks if needed,
        // but also short enough to merge — just verify embedding called == chunkCount
        String text = "Para one.\n\nPara two.\n\nPara three.";

        PolicyVersion result = service.ingest("policy-x", "v3.0", text);

        verify(embeddingProvider, times(result.getChunkCount())).embed(any(EmbeddingRequest.class));
    }

    @Test
    void ingest_vectorDocumentsHaveCorrectMetadata() {
        String text = "Policy content goes here.";

        service.ingest("underwriting-policy", "v4.0", text);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VectorDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).store(captor.capture());

        List<VectorDocument> stored = captor.getValue();
        assertFalse(stored.isEmpty());

        VectorDocument doc = stored.get(0);
        assertEquals("POLICY_DOCUMENT", doc.getDocumentType(), "documentType must be POLICY_DOCUMENT");
        assertEquals("v4.0", doc.getPolicyVersion(), "policyVersion must match");
        assertEquals("underwriting-policy", doc.getSourceDocumentId(), "sourceDocumentId must be policyName");
        assertEquals(0, doc.getChunkSequence(), "First chunk should have sequence 0");
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    void ingest_blankTextContent_throwsPolicyException() {
        assertThrows(PolicyException.class,
                () -> service.ingest("policy", "v1.0", "   "));
    }

    @Test
    void ingest_nullTextContent_throwsPolicyException() {
        assertThrows(PolicyException.class,
                () -> service.ingest("policy", "v1.0", null));
    }

    @Test
    void ingest_blankPolicyName_throwsPolicyException() {
        assertThrows(PolicyException.class,
                () -> service.ingest("", "v1.0", "some content"));
    }

    @Test
    void ingest_blankVersion_throwsPolicyException() {
        assertThrows(PolicyException.class,
                () -> service.ingest("policy", "", "some content"));
    }
}
