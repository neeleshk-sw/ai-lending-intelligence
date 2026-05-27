package com.ailending.ingestion.service;

import com.ailending.aicore.embedding.EmbeddingProvider;
import com.ailending.aicore.embedding.EmbeddingRequest;
import com.ailending.aicore.embedding.EmbeddingResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import com.ailending.aicore.vectorstore.VectorStore;
import com.ailending.common.enums.DocumentStatus;
import com.ailending.ingestion.chunking.ChunkConfig;
import com.ailending.ingestion.chunking.Chunker;
import com.ailending.ingestion.chunking.DocumentChunk;
import com.ailending.ingestion.parser.DocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentIngestorImplTest {

    private DocumentParser parser;
    private Chunker chunker;
    private EmbeddingProvider embeddingProvider;
    private VectorStore vectorStore;
    private DocumentIngestorImpl ingestor;

    @BeforeEach
    void setUp() {
        parser = mock(DocumentParser.class);
        chunker = mock(Chunker.class);
        embeddingProvider = mock(EmbeddingProvider.class);
        vectorStore = mock(VectorStore.class);
        ingestor = new DocumentIngestorImpl(parser, chunker, embeddingProvider, vectorStore);
    }

    @Test
    void ingestStoresTwoChunksWithCorrectMetadata() {
        byte[] content = "some text".getBytes(StandardCharsets.UTF_8);
        IngestionRequest request = IngestionRequest.builder()
                .content(content)
                .mimeType("text/plain")
                .loanId("L001")
                .customerId("C001")
                .documentType("SALARY_SLIP")
                .sourceDocumentId("DOC-1")
                .policyVersion("v2")
                .build();

        when(parser.parse(content, "text/plain")).thenReturn("parsed text");
        when(chunker.chunk(eq("parsed text"), any(ChunkConfig.class)))
                .thenReturn(List.of(new DocumentChunk("chunk one", 0), new DocumentChunk("chunk two", 1)));

        List<Double> vector = List.of(0.1, 0.2, 0.3);
        when(embeddingProvider.embed(any(EmbeddingRequest.class)))
                .thenReturn(new EmbeddingResult(vector, 3, 50L));

        IngestionResult result = ingestor.ingest(request);

        assertEquals(DocumentStatus.COMPLETED, result.getStatus());
        assertEquals("DOC-1", result.getSourceDocumentId());
        assertEquals(2, result.getChunkCount());
        assertTrue(result.getDurationMs() >= 0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VectorDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).store(captor.capture());

        List<VectorDocument> stored = captor.getValue();
        assertEquals(2, stored.size());

        VectorDocument first = stored.get(0);
        assertEquals("L001", first.getLoanId());
        assertEquals("C001", first.getCustomerId());
        assertEquals("SALARY_SLIP", first.getDocumentType());
        assertEquals("DOC-1", first.getSourceDocumentId());
        assertEquals("v2", first.getPolicyVersion());
        assertEquals(0, first.getChunkSequence());
        assertEquals("chunk one", first.getContent());
        assertEquals(vector, first.getEmbedding());

        assertEquals(1, stored.get(1).getChunkSequence());
    }

    @Test
    void emptyChunkListSkipsStoreAndReturnsCompleted() {
        byte[] content = "  ".getBytes(StandardCharsets.UTF_8);
        IngestionRequest request = IngestionRequest.builder()
                .content(content)
                .sourceDocumentId("DOC-2")
                .build();

        when(parser.parse(any(), any())).thenReturn("  ");
        when(chunker.chunk(any(), any())).thenReturn(List.of());

        IngestionResult result = ingestor.ingest(request);

        assertEquals(DocumentStatus.COMPLETED, result.getStatus());
        assertEquals(0, result.getChunkCount());
        verifyNoInteractions(embeddingProvider);
        verifyNoInteractions(vectorStore);
    }

    @Test
    void parseFailurePropagatesAsIngestionException() {
        byte[] content = "data".getBytes(StandardCharsets.UTF_8);
        IngestionRequest request = IngestionRequest.builder()
                .content(content)
                .sourceDocumentId("DOC-3")
                .build();

        when(parser.parse(any(), any())).thenThrow(new RuntimeException("corrupt file"));

        IngestionException ex = assertThrows(IngestionException.class, () -> ingestor.ingest(request));
        assertTrue(ex.getMessage().contains("DOC-3"));
    }
}
