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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestorImpl implements DocumentIngestor {

    private final DocumentParser documentParser;
    private final Chunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;

    public DocumentIngestorImpl(DocumentParser documentParser,
                                Chunker chunker,
                                EmbeddingProvider embeddingProvider,
                                VectorStore vectorStore) {
        this.documentParser = documentParser;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
    }

    @Override
    public IngestionResult ingest(IngestionRequest request) {
        long startMs = System.currentTimeMillis();

        try {
            String text = documentParser.parse(request.getContent(), request.getMimeType());

            List<DocumentChunk> chunks = chunker.chunk(text, ChunkConfig.defaults());

            if (chunks.isEmpty()) {
                return new IngestionResult(request.getSourceDocumentId(), 0, DocumentStatus.COMPLETED,
                        System.currentTimeMillis() - startMs);
            }

            List<VectorDocument> vectorDocs = new ArrayList<>(chunks.size());
            long ingestionTimestamp = System.currentTimeMillis();

            for (DocumentChunk chunk : chunks) {
                EmbeddingResult embedding = embeddingProvider.embed(
                        new EmbeddingRequest(chunk.getText(), null));

                VectorDocument doc = VectorDocument.builder()
                        .id(UUID.randomUUID().toString())
                        .content(chunk.getText())
                        .embedding(embedding.getVector())
                        .loanId(request.getLoanId())
                        .customerId(request.getCustomerId())
                        .documentType(request.getDocumentType())
                        .sourceDocumentId(request.getSourceDocumentId())
                        .chunkSequence(chunk.getSequence())
                        .policyVersion(request.getPolicyVersion())
                        .ingestionTimestamp(ingestionTimestamp)
                        .build();

                vectorDocs.add(doc);
            }

            vectorStore.store(vectorDocs);

            return new IngestionResult(request.getSourceDocumentId(), chunks.size(),
                    DocumentStatus.COMPLETED, System.currentTimeMillis() - startMs);

        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Ingestion failed for document " + request.getSourceDocumentId() + ": " + e.getMessage());
        }
    }
}
