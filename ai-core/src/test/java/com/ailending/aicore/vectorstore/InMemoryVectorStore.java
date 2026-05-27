package com.ailending.aicore.vectorstore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link VectorStore} for unit testing only.
 *
 * <p>Documents are stored in a plain {@code List}. Similarity search uses brute-force
 * cosine similarity when a {@code queryVector} is present in the {@link SearchRequest};
 * otherwise, every metadata-matching document receives a fixed score of {@code 1.0}.
 *
 * <p>Metadata filtering honours the four indexed fields:
 * {@code loanId}, {@code customerId}, {@code documentType}, {@code policyVersion}.
 * {@code null} / blank filter values in the request are ignored (no-filter behaviour).
 *
 * <p><b>Not for production use.</b>
 */
public class InMemoryVectorStore implements VectorStore {

    private final List<VectorDocument> store = new ArrayList<>();

    // -----------------------------------------------------------------------
    // VectorStore interface
    // -----------------------------------------------------------------------

    @Override
    public void store(List<VectorDocument> documents) {
        if (documents != null) {
            store.addAll(documents);
        }
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        List<VectorDocument> candidates = store.stream()
                .filter(doc -> matches(doc, request))
                .collect(Collectors.toList());

        List<SearchResult> results;
        if (request.getQueryVector() != null && !request.getQueryVector().isEmpty()) {
            results = candidates.stream()
                    .map(doc -> new SearchResult(doc, cosine(request.getQueryVector(), doc.getEmbedding())))
                    .collect(Collectors.toList());
        } else {
            // No query vector supplied — return all matching docs with a fixed score so
            // callers can still verify metadata filtering without an embedding model.
            results = candidates.stream()
                    .map(doc -> new SearchResult(doc, 1.0))
                    .collect(Collectors.toList());
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getSimilarityScore).reversed())
                .limit(request.getTopK())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByMetadata(String key, String value) {
        Iterator<VectorDocument> it = store.iterator();
        while (it.hasNext()) {
            VectorDocument doc = it.next();
            String docValue = metadataValue(doc, key);
            if (value != null && value.equals(docValue)) {
                it.remove();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test helpers
    // -----------------------------------------------------------------------

    /** Returns all currently stored documents (for assertion convenience). */
    public List<VectorDocument> getAll() {
        return new ArrayList<>(store);
    }

    /** Removes everything from the store. */
    public void clear() {
        store.clear();
    }

    /** Returns the number of stored documents. */
    public int size() {
        return store.size();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the document satisfies every non-null filter
     * field in the request.
     */
    private boolean matches(VectorDocument doc, SearchRequest request) {
        if (isSet(request.getLoanId()) && !request.getLoanId().equals(doc.getLoanId())) {
            return false;
        }
        if (isSet(request.getCustomerId()) && !request.getCustomerId().equals(doc.getCustomerId())) {
            return false;
        }
        if (isSet(request.getDocumentType()) && !request.getDocumentType().equals(doc.getDocumentType())) {
            return false;
        }
        if (isSet(request.getPolicyVersion()) && !request.getPolicyVersion().equals(doc.getPolicyVersion())) {
            return false;
        }
        return true;
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Brute-force cosine similarity between two equal-length vectors.
     * Returns {@code 0.0} if either vector is null, empty, or of different length.
     */
    static double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || a.size() != b.size()) {
            return 0.0;
        }
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double ai = a.get(i);
            double bi = b.get(i);
            dot   += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Retrieves the metadata value for a given key from a {@link VectorDocument}.
     * Mirrors the four indexed keys supported by {@link com.ailending.aicore.vectorstore.VectorStore}.
     */
    private String metadataValue(VectorDocument doc, String key) {
        switch (key) {
            case "loanId":        return doc.getLoanId();
            case "customerId":    return doc.getCustomerId();
            case "documentType":  return doc.getDocumentType();
            case "policyVersion": return doc.getPolicyVersion();
            default:              return null;
        }
    }
}
