package com.ailending.policy.service;

import com.ailending.aicore.embedding.EmbeddingProvider;
import com.ailending.aicore.embedding.EmbeddingRequest;
import com.ailending.aicore.embedding.EmbeddingResult;
import com.ailending.aicore.vectorstore.VectorDocument;
import com.ailending.aicore.vectorstore.VectorStore;
import com.ailending.policy.exception.PolicyException;
import com.ailending.policy.model.PolicyVersion;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PolicyIngestorServiceImpl implements PolicyIngestorService {

    private static final int MAX_WORDS_PER_CHUNK = 500;

    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;

    public PolicyIngestorServiceImpl(EmbeddingProvider embeddingProvider, VectorStore vectorStore) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore       = vectorStore;
    }

    @Override
    public PolicyVersion ingest(String policyName, String version, String textContent) {
        if (textContent == null || textContent.isBlank()) {
            throw new PolicyException("textContent must not be blank for policy: " + policyName);
        }
        if (policyName == null || policyName.isBlank()) {
            throw new PolicyException("policyName is required");
        }
        if (version == null || version.isBlank()) {
            throw new PolicyException("version is required");
        }

        List<String> chunks = splitIntoChunks(textContent);

        List<VectorDocument> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            EmbeddingResult embedding = embeddingProvider.embed(new EmbeddingRequest(chunkText, null));

            VectorDocument doc = VectorDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .content(chunkText)
                    .embedding(embedding.getVector())
                    .documentType("POLICY_DOCUMENT")
                    .policyVersion(version)
                    .sourceDocumentId(policyName)
                    .chunkSequence(i)
                    .build();

            documents.add(doc);
        }

        vectorStore.store(documents);

        return new PolicyVersion(version, policyName, Instant.now(), documents.size());
    }

    private List<String> splitIntoChunks(String text) {
        // Split on blank lines to get paragraphs, then merge until MAX_WORDS_PER_CHUNK
        String[] paragraphs = text.split("\\n\\s*\\n");

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentWordCount = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            int wordCount = countWords(trimmed);

            if (currentWordCount + wordCount > MAX_WORDS_PER_CHUNK && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
                currentWordCount = 0;
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(trimmed);
            currentWordCount += wordCount;
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return Arrays.stream(text.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toList())
                .size();
    }
}
