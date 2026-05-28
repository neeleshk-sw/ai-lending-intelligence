package com.ailending.ruleengine.service;

import com.ailending.ruleengine.exception.RuleEvaluationException;
import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.rule.AmlComplianceRule;
import com.ailending.ruleengine.rule.DebtToIncomeRule;
import com.ailending.ruleengine.rule.KycVerificationRule;
import com.ailending.ruleengine.rule.MinimumCreditScoreRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluationServiceImplTest {

    private RuleEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new RuleEvaluationServiceImpl(List.of(
                new DebtToIncomeRule(),
                new MinimumCreditScoreRule(),
                new KycVerificationRule(),
                new AmlComplianceRule()
        ));
    }

    // -----------------------------------------------------------------------
    // Happy path — all rules pass
    // -----------------------------------------------------------------------

    @Test
    void allRulesPass_resultIsPassed() {
        RuleResult result = service.evaluate(passingInput().build());

        assertTrue(result.isPassed(), "All rules should pass for a clean application");
        assertTrue(result.getViolations().isEmpty(), "No violations expected");
        assertEquals("loan-1", result.getLoanId());
        assertNotNull(result.getEvaluatedAt());
    }

    // -----------------------------------------------------------------------
    // Single rule failures
    // -----------------------------------------------------------------------

    @Test
    void lowCreditScore_producesOneViolation() {
        RuleInput input = passingInput().creditScore(600).build();
        RuleResult result = service.evaluate(input);

        assertFalse(result.isPassed());
        assertEquals(1, result.getViolations().size(), "Only credit score rule should fail");
        assertTrue(result.getViolations().get(0).contains("Credit score"));
    }

    @Test
    void kycNotVerified_producesOneViolation() {
        RuleInput input = passingInput().kycVerified(false).build();
        RuleResult result = service.evaluate(input);

        assertFalse(result.isPassed());
        assertEquals(1, result.getViolations().size());
        assertTrue(result.getViolations().get(0).contains("KYC"));
    }

    @Test
    void amlNotClear_producesOneViolation() {
        RuleInput input = passingInput().amlClear(false).build();
        RuleResult result = service.evaluate(input);

        assertFalse(result.isPassed());
        assertEquals(1, result.getViolations().size());
        assertTrue(result.getViolations().get(0).contains("AML"));
    }

    // -----------------------------------------------------------------------
    // Multiple rule failures
    // -----------------------------------------------------------------------

    @Test
    void multipleFailures_allViolationsCollected() {
        RuleInput input = RuleInput.builder()
                .loanId("loan-2")
                .customerId("cust-2")
                .monthlyIncome(new BigDecimal("5000"))
                .requestedAmount(new BigDecimal("500000"))   // very high → DTI violation
                .existingMonthlyDebt(new BigDecimal("1000"))
                .creditScore(500)                            // below 650 → credit score violation
                .kycVerified(false)                          // kyc violation
                .amlClear(false)                             // aml violation
                .build();

        RuleResult result = service.evaluate(input);

        assertFalse(result.isPassed());
        assertEquals(4, result.getViolations().size(), "All 4 rules should produce violations");
    }

    // -----------------------------------------------------------------------
    // Null input
    // -----------------------------------------------------------------------

    @Test
    void nullInput_throwsRuleEvaluationException() {
        assertThrows(RuleEvaluationException.class, () -> service.evaluate(null));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private RuleInput.Builder passingInput() {
        return RuleInput.builder()
                .loanId("loan-1")
                .customerId("cust-1")
                .monthlyIncome(new BigDecimal("50000"))
                .requestedAmount(new BigDecimal("100000"))   // EMI=2000, DTI=0.04 — well under 0.45
                .existingMonthlyDebt(BigDecimal.ZERO)
                .creditScore(750)
                .kycVerified(true)
                .amlClear(true);
    }
}
