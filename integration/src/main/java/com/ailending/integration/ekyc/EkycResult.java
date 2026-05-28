package com.ailending.integration.ekyc;

import java.time.Instant;

public final class EkycResult {

    private final String customerId;
    private final boolean verified;
    private final Instant verifiedAt;

    public EkycResult(String customerId, boolean verified, Instant verifiedAt) {
        this.customerId = customerId;
        this.verified   = verified;
        this.verifiedAt = verifiedAt;
    }

    public String getCustomerId()  { return customerId; }
    public boolean isVerified()    { return verified; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
