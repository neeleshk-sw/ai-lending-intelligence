package com.ailending.app.providers.vectorstore;

import com.ailending.aicore.vectorstore.SearchRequest;
import com.ailending.aicore.vectorstore.SearchResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import com.ailending.aicore.vectorstore.VectorStore;
import com.ailending.aicore.vectorstore.VectorStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of VectorStore using Spring AI's VectorStore delegate (backed by pgvector).
 * Lives in the app module so that ai-core remains Spring-free.
 */
@Service
public class SpringVectorStoreWrapper implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(SpringVectorStoreWrapper.class);

    private final org.springframework.ai.vectorstore.VectorStore delegate;

    public SpringVectorStoreWrapper(org.springframework.ai.vectorstore.VectorStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public void store(List<VectorDocument> documents) {
        try {
            List<Document> springDocs = documents.stream()
                .map(doc -> new Document(doc.getId(), doc.getContent(), doc.getMetadataMap()))
                .collect(Collectors.toList());

            delegate.add(springDocs);
            log.info("Successfully stored {} chunks in vector store", springDocs.size());
        } catch (Exception e) {
            log.error("Failed to store documents in vector store", e);
            throw new VectorStoreException("Vector storage failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        try {
            org.springframework.ai.vectorstore.SearchRequest springRequest =
                org.springframework.ai.vectorstore.SearchRequest.defaults()
                    .withQuery(request.getQuery() != null ? request.getQuery() : "")
                    .withTopK(request.getTopK());

            List<FilterExpressionBuilder.Op> expressions = new ArrayList<>();
            FilterExpressionBuilder feb = new FilterExpressionBuilder();

            if (request.getLoanId() != null && !request.getLoanId().isBlank()) {
                expressions.add(feb.eq("loanId", request.getLoanId()));
            }
            if (request.getCustomerId() != null && !request.getCustomerId().isBlank()) {
                expressions.add(feb.eq("customerId", request.getCustomerId()));
            }
            if (request.getDocumentType() != null && !request.getDocumentType().isBlank()) {
                expressions.add(feb.eq("documentType", request.getDocumentType()));
            }
            if (request.getPolicyVersion() != null && !request.getPolicyVersion().isBlank()) {
                expressions.add(feb.eq("policyVersion", request.getPolicyVersion()));
            }

            if (!expressions.isEmpty()) {
                FilterExpressionBuilder.Op combined = expressions.get(0);
                for (int i = 1; i < expressions.size(); i++) {
                    combined = feb.and(combined, expressions.get(i));
                }
                springRequest = springRequest.withFilterExpression(combined.build());
            }

            List<Document> results = delegate.similaritySearch(springRequest);

            return results.stream().map(doc -> {
                Map<String, Object> metadata = doc.getMetadata();

                String loanId       = (String) metadata.get("loanId");
                String customerId   = (String) metadata.get("customerId");
                String documentType = (String) metadata.get("documentType");
                String sourceDocId  = (String) metadata.get("sourceDocumentId");
                String policyVer    = (String) metadata.get("policyVersion");

                Number timestampVal = (Number) metadata.get("ingestionTimestamp");
                long ingestionTimestamp = timestampVal != null ? timestampVal.longValue() : 0L;

                Number sequenceVal = (Number) metadata.get("chunkSequence");
                int chunkSequence = sequenceVal != null ? sequenceVal.intValue() : 0;

                double similarityScore = toSimilarityScore(metadata.get("distance"));

                VectorDocument vecDoc = VectorDocument.builder()
                    .id(doc.getId())
                    .content(doc.getContent())
                    .loanId(loanId)
                    .customerId(customerId)
                    .documentType(documentType)
                    .sourceDocumentId(sourceDocId)
                    .ingestionTimestamp(ingestionTimestamp)
                    .chunkSequence(chunkSequence)
                    .policyVersion(policyVer)
                    .build();

                return new SearchResult(vecDoc, similarityScore);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed similarity search in vector store", e);
            throw new VectorStoreException("Vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByMetadata(String key, String value) {
        try {
            log.info("Deleting documents from vector store where {}={}", key, value);
            // Reuse the search path: build a metadata-filtered query and collect matching IDs.
            // topK=1000 is a safe upper bound for a single loan/customer's document chunks.
            SearchRequest searchRequest = buildMetadataSearchRequest(key, value);
            List<SearchResult> hits = search(searchRequest);
            if (!hits.isEmpty()) {
                List<String> ids = hits.stream()
                    .map(h -> h.getDocument().getId())
                    .collect(Collectors.toList());
                delegate.delete(ids);
                log.info("Successfully deleted {} chunks where {}={}", ids.size(), key, value);
            }
        } catch (Exception e) {
            log.error("Failed to delete documents where {}={}", key, value, e);
            throw new VectorStoreException("Vector deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a SearchRequest pre-populated with a single metadata filter.
     * Supports the four indexed metadata fields: loanId, customerId, documentType, policyVersion.
     */
    private SearchRequest buildMetadataSearchRequest(String key, String value) {
        SearchRequest.Builder builder = SearchRequest.builder().topK(1000);
        switch (key) {
            case "loanId":         builder.loanId(value);        break;
            case "customerId":     builder.customerId(value);    break;
            case "documentType":   builder.documentType(value);  break;
            case "policyVersion":  builder.policyVersion(value); break;
            default:
                throw new VectorStoreException(
                    "Unsupported metadata filter key for deletion: '" + key + "'. " +
                    "Supported keys: loanId, customerId, documentType, policyVersion."
                );
        }
        return builder.build();
    }

    private double toSimilarityScore(Object distanceValue) {
        if (!(distanceValue instanceof Number)) {
            return 0.0;
        }
        double distance = ((Number) distanceValue).doubleValue();
        return Math.min(1.0, Math.max(0.0, 1.0 - distance));
    }
}
