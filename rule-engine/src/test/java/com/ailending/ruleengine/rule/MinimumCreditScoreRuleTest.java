package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MinimumCreditScoreRuleTest {

    private MinimumCreditScoreRule rule;

    @BeforeEach
    void setUp() {
        rule = new MinimumCreditScoreRule();
    }

    @Test
    void passes_atExactMinimum() {
        Optional<String> result = rule.evaluate(inputWithScore(650));
        assertTrue(result.isEmpty(), "Score of 650 should pass");
    }

    @Test
    void passes_aboveMinimum() {
        Optional<String> result = rule.evaluate(inputWithScore(750));
        assertTrue(result.isEmpty(), "Score above 650 should pass");
    }

    @Test
    void fails_oneBelowMinimum() {
        Optional<String> result = rule.evaluate(inputWithScore(649));
        assertTrue(result.isPresent(), "Score of 649 should fail");
        assertTrue(result.get().contains("649"));
        assertTrue(result.get().contains("650"));
    }

    @Test
    void fails_wellBelowMinimum() {
        Optional<String> result = rule.evaluate(inputWithScore(300));
        assertTrue(result.isPresent(), "Score of 300 should fail");
    }

    @Test
    void ruleName_isCorrect() {
        assertEquals("MINIMUM_CREDIT_SCORE", rule.ruleName());
    }

    private RuleInput inputWithScore(int score) {
        return RuleInput.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .monthlyIncome(new BigDecimal("10000"))
                .requestedAmount(new BigDecimal("50000"))
                .creditScore(score)
                .kycVerified(true)
                .amlClear(true)
                .build();
    }
}
