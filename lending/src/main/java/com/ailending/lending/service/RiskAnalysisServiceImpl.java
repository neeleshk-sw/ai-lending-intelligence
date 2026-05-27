package com.ailending.lending.service;

import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.rag.RagRequest;
import com.ailending.aicore.rag.RagResponse;
import org.springframework.stereotype.Service;

/**
 * Delegates AI-assisted risk analysis to the {@link RagEngine}.
 *
 * <p>The "underwriting" prompt template is used so the LLM is instructed to act
 * as an expert underwriter and produce detailed risk observations.
 *
 * <p><b>Advisory only:</b> this service never modifies the loan record or its workflow
 * status — it is strictly a read-through analysis layer (ADR-003).
 */
@Service
public class RiskAnalysisServiceImpl implements RiskAnalysisService {

    private static final String UNDERWRITING_QUERY =
            "Provide an underwriting risk assessment for this loan application.";

    private final RagEngine ragEngine;

    public RiskAnalysisServiceImpl(RagEngine ragEngine) {
        this.ragEngine = ragEngine;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a {@link RagRequest} with:
     * <ul>
     *   <li>{@code query} — fixed underwriting assessment question</li>
     *   <li>{@code loanId} / {@code customerId} — forwarded as metadata filters so only
     *       documents belonging to this loan are retrieved</li>
     *   <li>{@code promptTemplateName} — {@code "underwriting"}</li>
     * </ul>
     */
    @Override
    public RagResponse analyzeRisk(String loanId, String customerId) {
        RagRequest request = RagRequest.builder()
                .query(UNDERWRITING_QUERY)
                .loanId(loanId)
                .customerId(customerId)
                .promptTemplateName("underwriting")
                .build();

        return ragEngine.execute(request);
    }
}
