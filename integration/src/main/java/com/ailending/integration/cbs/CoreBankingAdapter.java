package com.ailending.integration.cbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoreBankingAdapter implements CoreBankingPort {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingAdapter.class);

    private final Set<String> disbursedLoanIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public boolean accountExists(String customerId) {
        log.debug("CBS stub: accountExists check for customer {}", customerId);
        return true;
    }

    @Override
    public void disburseLoan(String loanId, String customerId, BigDecimal amount, String currency) {
        log.info("CBS stub: disbursing loan {} for customer {} — {} {}", loanId, customerId, amount, currency);
        disbursedLoanIds.add(loanId);
    }

    public Set<String> getDisbursedLoanIds() {
        return Collections.unmodifiableSet(disbursedLoanIds);
    }
}
