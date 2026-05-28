package com.ailending.integration.creditbureau;

public interface CreditBureauPort {

    CreditReport fetchReport(String customerId, String pan);
}
