package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AmlComplianceRuleTest {

    private AmlComplianceRule rule;

    @BeforeEach
    void setUp() {
        rule = new AmlComplianceRule();
    }

    @Test
    void passes_whenAmlClear() {
        Optional<String> result = rule.evaluate(inputWithAml(true));
        assertTrue(result.isEmpty(), "Clear AML should pass");
    }

    @Test
    void fails_whenAmlNotClear() {
        Optional<String> result = rule.evaluate(inputWithAml(false));
        assertTrue(result.isPresent(), "AML not cleared should produce a violation");
        assertTrue(result.get().contains("AML"));
    }

    @Test
    void ruleName_isCorrect() {
        assertEquals("AML_COMPLIANCE", rule.ruleName());
    }

    private RuleInput inputWithAml(boolean amlClear) {
        return RuleInput.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .monthlyIncome(new BigDecimal("10000"))
                .requestedAmount(new BigDecimal("50000"))
                .creditScore(700)
                .kycVerified(true)
                .amlClear(amlClear)
                .build();
    }
}
