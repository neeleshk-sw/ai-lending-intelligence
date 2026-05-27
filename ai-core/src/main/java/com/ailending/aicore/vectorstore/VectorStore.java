package com.ailending.aicore.vectorstore;

import java.util.List;

/**
 * Abstraction for vector storage and similarity search.
 * Implementations must handle specific backends (in-memory, pgvector, ChromaDB, etc.).
 */
public interface VectorStore {

    /**
     * Stores a list of vector documents with their embeddings and metadata.
     *
     * @param documents the list of vector documents to store
     * @throws VectorStoreException if storage fails
     */
    void store(List<VectorDocument> documents);

    /**
     * Performs similarity search based on the request's vector and metadata filters.
     *
     * @param request the search request carrying target vector and filters
     * @return the list of matched results containing documents and similarity scores
     * @throws VectorStoreException if search fails
     */
    List<SearchResult> search(SearchRequest request);

    /**
     * Deletes all stored document chunks that match a specific metadata key-value pair.
     * Callers supply domain-specific keys (e.g. {@code "loanId"}, {@code "customerId"})
     * keeping this interface free of lending-domain concepts.
     *
     * @param key   the metadata field name to filter on (e.g. {@code "loanId"})
     * @param value the value to match
     * @throws VectorStoreException if deletion fails
     */
    void deleteByMetadata(String key, String value);
}
