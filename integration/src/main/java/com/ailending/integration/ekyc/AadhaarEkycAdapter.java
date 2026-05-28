package com.ailending.integration.ekyc;

import com.ailending.integration.exception.IntegrationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AadhaarEkycAdapter implements EkycPort {

    @Override
    public EkycResult verify(String customerId, String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
            throw new IntegrationException(
                    "Aadhaar number must not be blank for eKYC verification of customer: " + customerId);
        }
        return new EkycResult(customerId, true, Instant.now());
    }
}
