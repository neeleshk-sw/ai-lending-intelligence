package com.ailending.integration.creditbureau;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CibilAdapterTest {

    private CibilAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CibilAdapter();
    }

    @Test
    void fetchReport_returnsStubScore720() {
        CreditReport report = adapter.fetchReport("cust-1", "ABCDE1234F");

        assertEquals(720, report.getScore(), "Stub adapter should return score 720");
    }

    @Test
    void fetchReport_fetchedAtIsNotNull() {
        CreditReport report = adapter.fetchReport("cust-1", "ABCDE1234F");

        assertNotNull(report.getFetchedAt(), "fetchedAt should be populated");
    }

    @Test
    void fetchReport_customerIdAndPanPreserved() {
        CreditReport report = adapter.fetchReport("cust-99", "XYZAB9876K");

        assertEquals("cust-99",    report.getCustomerId());
        assertEquals("XYZAB9876K", report.getPan());
    }
}
