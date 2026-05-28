package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class DebtToIncomeRule implements Rule {

    private static final BigDecimal EMI_RATE      = new BigDecimal("0.02");
    private static final BigDecimal DTI_THRESHOLD = new BigDecimal("0.45");

    @Override
    public Optional<String> evaluate(RuleInput input) {
        BigDecimal estimatedEmi = input.getRequestedAmount().multiply(EMI_RATE);
        BigDecimal totalDebt    = input.getExistingMonthlyDebt().add(estimatedEmi);
        BigDecimal dti          = totalDebt.divide(input.getMonthlyIncome(), 4, RoundingMode.HALF_UP);

        if (dti.compareTo(DTI_THRESHOLD) > 0) {
            return Optional.of(String.format(
                    "Debt-to-income ratio %.2f exceeds maximum allowed threshold of %.2f",
                    dti.doubleValue(), DTI_THRESHOLD.doubleValue()));
        }
        return Optional.empty();
    }

    @Override
    public String ruleName() {
        return "DEBT_TO_INCOME";
    }
}
