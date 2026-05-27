package com.ailending.ingestion.service;

/**
 * Service responsible for orchestrating the document ingestion process.
 * Pipeline: parse → chunk → embed → store in vector store.
 */
public interface DocumentIngestor {

    /**
     * Ingests a document: parses its content, splits it into overlapping chunks,
     * generates embeddings for each chunk, and stores them in the vector store.
     *
     * @param request carries raw document bytes and required metadata
     * @return result containing chunk count, status, and elapsed time
     * @throws IngestionException if any pipeline stage fails
     */
    IngestionResult ingest(IngestionRequest request);
}
