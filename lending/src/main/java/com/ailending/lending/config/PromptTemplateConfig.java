package com.ailending.lending.config;

import com.ailending.aicore.prompt.PromptTemplate;
import com.ailending.aicore.rag.RagEngine;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers all business-domain prompt templates with the {@link RagEngine}.
 *
 * <p>Business prompt templates belong in the {@code lending} module, not in {@code ai-core},
 * because they encode domain knowledge about the lending process (underwriting rules,
 * loan summary formats, policy Q&A style). Keeping them here lets {@code ai-core} remain
 * a generic RAG infrastructure layer that knows nothing about lending.
 *
 * <p>Four templates are registered on application startup:
 * <ul>
 *   <li>{@code default}      — general-purpose Q&A against loan documents</li>
 *   <li>{@code loan-summary} — structured JSON loan summary (uses low temperature = 0.1)</li>
 *   <li>{@code underwriting} — expert underwriter risk observations</li>
 *   <li>{@code policy-qa}    — policy interpretation with mandatory section citations</li>
 * </ul>
 */
@Configuration
public class PromptTemplateConfig {

    private final RagEngine ragEngine;

    public PromptTemplateConfig(RagEngine ragEngine) {
        this.ragEngine = ragEngine;
    }

    @PostConstruct
    public void registerTemplates() {
        ragEngine.registerTemplate("default", new PromptTemplate("default",
                "You are an AI lending assistant evaluating a loan application.\n"
                + "Use the following retrieved context documents to answer the question.\n"
                + "If the answer cannot be found in the context, say "
                + "\"I cannot find the answer in the provided documents.\"\n\n"
                + "Context:\n{context}\n\n"
                + "Question: {query}\n"
                + "Answer:"
        ));

        ragEngine.registerTemplate("loan-summary", new PromptTemplate("loan-summary",
                "You are an AI lending assistant. Generate a structured JSON loan summary "
                + "based on the provided documents.\n"
                + "The output must strictly be a valid JSON matching the following structure:\n"
                + "{\n"
                + "  \"borrowerProfile\": \"brief description of the borrower's background and employment\",\n"
                + "  \"monthlyIncome\": number,\n"
                + "  \"riskIndicators\": [\n"
                + "    \"any risk flags such as high-value deposits, inconsistent income, or missing docs\"\n"
                + "  ],\n"
                + "  \"manualReviewRecommended\": true/false\n"
                + "}\n\n"
                + "Context:\n{context}\n\n"
                + "Question: Compile the structured loan summary.\n"
                + "JSON Output:"
        ));

        ragEngine.registerTemplate("underwriting", new PromptTemplate("underwriting",
                "You are an expert lending underwriter. Analyze the loan application documents "
                + "and policy references.\n"
                + "Provide detailed underwriting observations regarding repayment risks, "
                + "income inconsistencies, and compliance flags.\n"
                + "Make sure to reference specific context pieces where possible.\n\n"
                + "Context:\n{context}\n\n"
                + "Question: {query}\n"
                + "Underwriting Observations:"
        ));

        ragEngine.registerTemplate("policy-qa", new PromptTemplate("policy-qa",
                "You are an AI policy interpretation assistant. Answer user questions regarding "
                + "lending policies based on the retrieved policy chunks.\n"
                + "Always cite the policy section or version in your answer.\n\n"
                + "Context:\n{context}\n\n"
                + "Question: {query}\n"
                + "Answer:"
        ));
    }
}
