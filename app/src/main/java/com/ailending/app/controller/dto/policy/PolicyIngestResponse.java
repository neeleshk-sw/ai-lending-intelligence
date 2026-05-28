package com.ailending.app.controller.dto.policy;

import com.ailending.policy.model.PolicyVersion;

import java.time.Instant;

public class PolicyIngestResponse {
    private final String version;
    private final String policyName;
    private final Instant ingestedAt;
    private final int chunkCount;

    private PolicyIngestResponse(PolicyVersion v) {
        this.version    = v.getVersion();
        this.policyName = v.getPolicyName();
        this.ingestedAt = v.getIngestedAt();
        this.chunkCount = v.getChunkCount();
    }

    public static PolicyIngestResponse from(PolicyVersion v) {
        return new PolicyIngestResponse(v);
    }

    public String getVersion()    { return version; }
    public String getPolicyName() { return policyName; }
    public Instant getIngestedAt(){ return ingestedAt; }
    public int getChunkCount()    { return chunkCount; }
}
