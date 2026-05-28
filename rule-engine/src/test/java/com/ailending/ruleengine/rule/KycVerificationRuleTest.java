package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KycVerificationRuleTest {

    private KycVerificationRule rule;

    @BeforeEach
    void setUp() {
        rule = new KycVerificationRule();
    }

    @Test
    void passes_whenKycVerified() {
        Optional<String> result = rule.evaluate(inputWithKyc(true));
        assertTrue(result.isEmpty(), "Verified KYC should pass");
    }

    @Test
    void fails_whenKycNotVerified() {
        Optional<String> result = rule.evaluate(inputWithKyc(false));
        assertTrue(result.isPresent(), "Unverified KYC should produce a violation");
        assertTrue(result.get().contains("KYC"));
    }

    @Test
    void ruleName_isCorrect() {
        assertEquals("KYC_VERIFICATION", rule.ruleName());
    }

    private RuleInput inputWithKyc(boolean kycVerified) {
        return RuleInput.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .monthlyIncome(new BigDecimal("10000"))
                .requestedAmount(new BigDecimal("50000"))
                .creditScore(700)
                .kycVerified(kycVerified)
                .amlClear(true)
                .build();
    }
}
