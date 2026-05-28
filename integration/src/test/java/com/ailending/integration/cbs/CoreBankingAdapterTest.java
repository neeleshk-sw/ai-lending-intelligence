package com.ailending.integration.cbs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CoreBankingAdapterTest {

    private CoreBankingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CoreBankingAdapter();
    }

    @Test
    void accountExists_alwaysReturnsTrue() {
        assertTrue(adapter.accountExists("cust-001"));
        assertTrue(adapter.accountExists("any-customer"));
    }

    @Test
    void disburseLoan_recordsLoanId() {
        adapter.disburseLoan("loan-1", "cust-1", new BigDecimal("500000"), "INR");

        assertTrue(adapter.getDisbursedLoanIds().contains("loan-1"),
                "Disbursed loan ID should be recorded");
    }

    @Test
    void disburseLoan_multipleLoanIds_allRecorded() {
        adapter.disburseLoan("loan-A", "cust-1", new BigDecimal("100000"), "INR");
        adapter.disburseLoan("loan-B", "cust-2", new BigDecimal("200000"), "INR");

        assertTrue(adapter.getDisbursedLoanIds().contains("loan-A"));
        assertTrue(adapter.getDisbursedLoanIds().contains("loan-B"));
        assertEquals(2, adapter.getDisbursedLoanIds().size());
    }
}
