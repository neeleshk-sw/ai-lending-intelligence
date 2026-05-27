package com.ailending.lending.service;

import com.ailending.aicore.rag.RagResponse;

/**
 * Service for AI-assisted risk analysis of loan applications.
 *
 * <p>Risk analysis results are <b>advisory only</b>. The system must never autonomously
 * approve or reject loans — all final decisions are made by human underwriters (ADR-003).
 *
 * <p>Every response includes source references, a confidence indicator, retrieved evidence
 * metadata, and a generation timestamp as required by NFR-5.
 */
public interface RiskAnalysisService {

    /**
     * Performs an AI-assisted underwriting risk analysis for the given loan.
     *
     * <p>Internally builds a RAG request with the "underwriting" prompt template and
     * delegates to {@link com.ailending.aicore.rag.RagEngine}. The returned
     * {@link RagResponse} includes the generated analysis text, retrieved source chunks,
     * confidence score, and the model/duration metadata required by NFR-5.
     *
     * <p>This method is <b>read-only</b> with respect to the loan entity — it does not
     * update the loan's status or persist any changes.
     *
     * @param loanId     identifier of the loan to analyse
     * @param customerId identifier of the borrower
     * @return the advisory {@link RagResponse} containing the risk assessment
     * @throws IllegalStateException if the LLM provider is currently unavailable
     */
    RagResponse analyzeRisk(String loanId, String customerId);
}
