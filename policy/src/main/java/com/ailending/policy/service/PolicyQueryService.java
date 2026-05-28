package com.ailending.policy.service;

import com.ailending.aicore.rag.RagResponse;

public interface PolicyQueryService {

    RagResponse query(String question, String policyVersion);
}
