package com.ailending.integration.cbs;

import java.math.BigDecimal;

public interface CoreBankingPort {

    boolean accountExists(String customerId);

    void disburseLoan(String loanId, String customerId, BigDecimal amount, String currency);
}
