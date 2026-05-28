package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DebtToIncomeRuleTest {

    private DebtToIncomeRule rule;

    @BeforeEach
    void setUp() {
        rule = new DebtToIncomeRule();
    }

    @Test
    void passes_whenDtiExactlyAtThreshold() {
        // income=10000, existingDebt=0, requestedAmount=22500
        // estimatedEMI = 22500 * 0.02 = 450 → DTI = 450/10000 = 0.045 — well under 0.45
        // To hit exactly 0.45: totalDebt = 0.45 * 10000 = 4500
        // existingDebt=0, EMI=requestedAmount*0.02 → requestedAmount = 4500/0.02 = 225000
        RuleInput input = validInput()
                .monthlyIncome(new BigDecimal("10000"))
                .existingMonthlyDebt(BigDecimal.ZERO)
                .requestedAmount(new BigDecimal("225000"))
                .build();

        Optional<String> result = rule.evaluate(input);

        assertTrue(result.isEmpty(), "DTI exactly at 0.45 should pass");
    }

    @Test
    void fails_whenDtiExceedsThreshold() {
        // income=10000, existingDebt=0, requestedAmount=250000
        // estimatedEMI = 250000 * 0.02 = 5000 → DTI = 5000/10000 = 0.5000 > 0.45
        RuleInput input = validInput()
                .monthlyIncome(new BigDecimal("10000"))
                .existingMonthlyDebt(BigDecimal.ZERO)
                .requestedAmount(new BigDecimal("250000"))
                .build();

        Optional<String> result = rule.evaluate(input);

        assertTrue(result.isPresent(), "DTI above 0.45 should produce a violation");
        assertTrue(result.get().contains("Debt-to-income ratio"));
    }

    @Test
    void fails_whenExistingDebtPushesOverThreshold() {
        // income=10000, requestedAmount=100000 → EMI=2000, existingDebt=2600 → total=4600, DTI=0.46
        RuleInput input = validInput()
                .monthlyIncome(new BigDecimal("10000"))
                .requestedAmount(new BigDecimal("100000"))
                .existingMonthlyDebt(new BigDecimal("2600"))
                .build();

        Optional<String> result = rule.evaluate(input);

        assertTrue(result.isPresent(), "High existing debt should push DTI over threshold");
    }

    @Test
    void passes_whenWellBelowThreshold() {
        // income=100000, requestedAmount=10000 → EMI=200, DTI=0.002
        RuleInput input = validInput()
                .monthlyIncome(new BigDecimal("100000"))
                .requestedAmount(new BigDecimal("10000"))
                .existingMonthlyDebt(BigDecimal.ZERO)
                .build();

        Optional<String> result = rule.evaluate(input);

        assertTrue(result.isEmpty(), "Low DTI should pass");
    }

    @Test
    void ruleName_isCorrect() {
        assertEquals("DEBT_TO_INCOME", rule.ruleName());
    }

    private RuleInput.Builder validInput() {
        return RuleInput.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .creditScore(700)
                .kycVerified(true)
                .amlClear(true);
    }
}
