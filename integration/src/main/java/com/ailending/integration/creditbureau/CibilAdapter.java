package com.ailending.integration.creditbureau;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CibilAdapter implements CreditBureauPort {

    private static final int STUB_SCORE = 720;

    @Override
    public CreditReport fetchReport(String customerId, String pan) {
        return new CreditReport(customerId, pan, STUB_SCORE, Instant.now());
    }
}
