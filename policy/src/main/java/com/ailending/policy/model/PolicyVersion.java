package com.ailending.policy.model;

import java.time.Instant;

public final class PolicyVersion {

    private final String version;
    private final String policyName;
    private final Instant ingestedAt;
    private final int chunkCount;

    public PolicyVersion(String version, String policyName, Instant ingestedAt, int chunkCount) {
        this.version    = version;
        this.policyName = policyName;
        this.ingestedAt = ingestedAt;
        this.chunkCount = chunkCount;
    }

    public String getVersion()    { return version; }
    public String getPolicyName() { return policyName; }
    public Instant getIngestedAt(){ return ingestedAt; }
    public int getChunkCount()    { return chunkCount; }
}
