package com.ailending.policy.service;

import com.ailending.aicore.rag.RagEngine;
import com.ailending.aicore.rag.RagRequest;
import com.ailending.aicore.rag.RagResponse;
import com.ailending.policy.exception.PolicyException;
import org.springframework.stereotype.Service;

@Service
public class PolicyQueryServiceImpl implements PolicyQueryService {

    private final RagEngine ragEngine;

    public PolicyQueryServiceImpl(RagEngine ragEngine) {
        this.ragEngine = ragEngine;
    }

    @Override
    public RagResponse query(String question, String policyVersion) {
        if (question == null || question.isBlank()) {
            throw new PolicyException("question must not be blank");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new PolicyException("policyVersion must not be blank");
        }

        RagRequest request = RagRequest.builder()
                .query(question)
                .documentType("POLICY_DOCUMENT")
                .policyVersion(policyVersion)
                .promptTemplateName("policy-qa")
                .build();

        return ragEngine.execute(request);
    }
}
