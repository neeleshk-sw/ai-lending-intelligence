package com.ailending.integration.crm;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CrmAdapter implements CrmPort {

    @Override
    public Optional<CustomerProfile> getProfile(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CustomerProfile(
                customerId,
                "Customer " + customerId,
                customerId.toLowerCase() + "@example.com",
                "+91-9000000000"
        ));
    }
}
