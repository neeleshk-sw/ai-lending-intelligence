package com.ailending.integration.crm;

import java.util.Optional;

public interface CrmPort {

    Optional<CustomerProfile> getProfile(String customerId);
}
