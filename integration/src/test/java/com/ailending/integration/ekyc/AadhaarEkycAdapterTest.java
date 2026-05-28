package com.ailending.integration.ekyc;

import com.ailending.integration.exception.IntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AadhaarEkycAdapterTest {

    private AadhaarEkycAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AadhaarEkycAdapter();
    }

    @Test
    void verify_validAadhaar_returnsVerifiedTrue() {
        EkycResult result = adapter.verify("cust-1", "1234-5678-9012");

        assertTrue(result.isVerified(), "Valid Aadhaar should produce verified=true");
        assertEquals("cust-1", result.getCustomerId());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void verify_blankAadhaar_throwsIntegrationException() {
        assertThrows(IntegrationException.class,
                () -> adapter.verify("cust-1", "   "));
    }

    @Test
    void verify_nullAadhaar_throwsIntegrationException() {
        assertThrows(IntegrationException.class,
                () -> adapter.verify("cust-1", null));
    }
}
