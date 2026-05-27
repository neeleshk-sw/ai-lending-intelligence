package com.ailending.aicore.rag;

import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmRequest;
import com.ailending.aicore.llm.LlmResponse;
import com.ailending.aicore.prompt.PromptBuilder;
import com.ailending.aicore.prompt.PromptContext;
import com.ailending.aicore.prompt.PromptTemplate;
import com.ailending.aicore.retrieval.RetrievalEngine;
import com.ailending.aicore.retrieval.RetrievalRequest;
import com.ailending.aicore.retrieval.RetrievalResult;
import com.ailending.aicore.vectorstore.SearchResult;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Top-level RAG orchestrator.
 *
 * <p>Composes {@link RetrievalEngine}, {@link PromptBuilder}, and {@link LlmProvider}
 * into a single {@link #execute(RagRequest)} operation.
 *
 * <p><b>Template-agnostic by design.</b> No prompt templates are hard-coded here.
 * Business-domain templates must be registered externally (e.g., from a
 * {@code @Configuration} class in the {@code lending} module) by calling
 * {@link #registerTemplate(String, PromptTemplate)} before any {@code execute()} call.
 * This keeps {@code ai-core} free of business-domain concerns.
 */
@Service
public class RagEngine {

    private static final Logger log = LoggerFactory.getLogger(RagEngine.class);

    private final RetrievalEngine retrievalEngine;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;

    /** Mutable template registry — populated externally via {@link #registerTemplate}. */
    private final Map<String, PromptTemplate> templates = new HashMap<>();

    public RagEngine(RetrievalEngine retrievalEngine, PromptBuilder promptBuilder, LlmProvider llmProvider) {
        this.retrievalEngine = retrievalEngine;
        this.promptBuilder   = promptBuilder;
        this.llmProvider     = llmProvider;
    }

    // -----------------------------------------------------------------------
    // Template registry
    // -----------------------------------------------------------------------

    /**
     * Registers a named prompt template.
     *
     * <p>Call this from a startup configuration class (e.g., a Spring
     * {@code @Configuration} bean with {@code @PostConstruct}) before the first
     * {@link #execute(RagRequest)} call. Registering a name that already exists
     * overwrites the previous entry.
     *
     * @param name     logical template name used in {@link RagRequest#getPromptTemplateName()}
     * @param template the prompt template to associate with that name
     * @throws IllegalArgumentException if {@code name} is blank or {@code template} is null
     */
    public void registerTemplate(String name, PromptTemplate template) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be null or blank");
        }
        if (template == null) {
            throw new IllegalArgumentException("Template must not be null");
        }
        templates.put(name, template);
        log.debug("Registered prompt template '{}'", name);
    }

    // -----------------------------------------------------------------------
    // RAG execution
    // -----------------------------------------------------------------------

    /**
     * Executes the end-to-end Retrieval-Augmented Generation (RAG) process.
     *
     * @param request configuration and query settings for this execution
     * @return the response containing generated text, confidence metrics, and source references
     * @throws IllegalStateException if the LLM provider is unavailable or no matching
     *                               template (and no "default" fallback) has been registered
     */
    public RagResponse execute(RagRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Graceful degradation: check if LLM provider is available
        if (!llmProvider.isAvailable()) {
            log.warn("LLM provider is not available. Gracefully failing RAG request.");
            throw new IllegalStateException(
                    "AI model serving is currently offline. Please review loan documents manually.");
        }

        // 2. Retrieve relevant context document chunks (top 5)
        RetrievalRequest retrievalRequest = RetrievalRequest.builder()
                .query(request.getQuery())
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .documentType(request.getDocumentType())
                .policyVersion(request.getPolicyVersion())
                .topK(5)
                .build();

        RetrievalResult retrievalResult = retrievalEngine.retrieve(retrievalRequest);
        List<SearchResult> hits = retrievalResult.getHits();

        // 3. Assemble chunks as a single context block
        String contextText = hits.stream()
                .map(h -> String.format("[Doc: %s] %s", h.getDocument().getId(), h.getDocument().getContent()))
                .collect(Collectors.joining("\n\n"));

        // 4. Resolve the requested template; fall back to "default" if unknown
        String templateName = request.getPromptTemplateName();
        PromptTemplate template = templates.getOrDefault(templateName, templates.get("default"));

        if (template == null) {
            throw new IllegalStateException(
                    "No prompt template registered for '" + templateName
                    + "' and no 'default' template has been registered. "
                    + "Register templates via RagEngine.registerTemplate() before calling execute().");
        }

        // 5. Build prompt
        PromptContext promptContext = new PromptContext();
        promptContext.set("query", request.getQuery());
        if (request.getAdditionalVariables() != null) {
            request.getAdditionalVariables().forEach(promptContext::set);
        }

        String finalPrompt = promptBuilder.buildPrompt(template, promptContext, contextText);

        // 6. Generate from LLM (lower temperature for JSON-output templates)
        LlmRequest llmRequest = new LlmRequest.Builder()
                .prompt(finalPrompt)
                .temperature(templateName.equals("loan-summary") ? 0.1 : 0.7)
                .build();

        LlmResponse llmResponse = llmProvider.generate(llmRequest);
        long durationMs = System.currentTimeMillis() - startTime;

        // 7. Compute heuristic confidence score
        double confidenceScore = calculateConfidence(hits);

        return new RagResponse(
                llmResponse.getText(),
                hits,
                llmResponse.getModel(),
                confidenceScore,
                durationMs
        );
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private double calculateConfidence(List<SearchResult> hits) {
        if (hits.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (SearchResult hit : hits) {
            sum += hit.getSimilarityScore();
        }
        double avg = sum / hits.size();
        return Math.min(1.0, Math.max(0.0, avg));
    }
}
