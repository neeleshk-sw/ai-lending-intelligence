package com.ailending.integration.creditbureau;

import java.time.Instant;

public final class CreditReport {

    private final String customerId;
    private final String pan;
    private final int score;
    private final Instant fetchedAt;

    public CreditReport(String customerId, String pan, int score, Instant fetchedAt) {
        this.customerId = customerId;
        this.pan        = pan;
        this.score      = score;
        this.fetchedAt  = fetchedAt;
    }

    public String getCustomerId() { return customerId; }
    public String getPan()        { return pan; }
    public int getScore()         { return score; }
    public Instant getFetchedAt() { return fetchedAt; }
}
