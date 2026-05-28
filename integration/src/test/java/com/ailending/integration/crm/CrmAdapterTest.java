package com.ailending.integration.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CrmAdapterTest {

    private CrmAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CrmAdapter();
    }

    @Test
    void getProfile_validCustomerId_returnsNonEmptyOptional() {
        Optional<CustomerProfile> profile = adapter.getProfile("cust-42");

        assertTrue(profile.isPresent(), "Profile should be present for a valid customerId");
        assertEquals("cust-42", profile.get().getCustomerId());
    }

    @Test
    void getProfile_profileContainsSyntheticData() {
        CustomerProfile profile = adapter.getProfile("cust-99").orElseThrow();

        assertNotNull(profile.getFullName(), "fullName should not be null");
        assertNotNull(profile.getEmail(), "email should not be null");
        assertNotNull(profile.getPhone(), "phone should not be null");
        assertTrue(profile.getEmail().contains("cust-99"), "email should reference customerId");
    }

    @Test
    void getProfile_blankCustomerId_returnsEmptyOptional() {
        assertTrue(adapter.getProfile("").isEmpty());
        assertTrue(adapter.getProfile(null).isEmpty());
    }
}
