package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MinimumCreditScoreRule implements Rule {

    private static final int MINIMUM_CREDIT_SCORE = 650;

    @Override
    public Optional<String> evaluate(RuleInput input) {
        if (input.getCreditScore() < MINIMUM_CREDIT_SCORE) {
            return Optional.of(String.format(
                    "Credit score %d is below the minimum required score of %d",
                    input.getCreditScore(), MINIMUM_CREDIT_SCORE));
        }
        return Optional.empty();
    }

    @Override
    public String ruleName() {
        return "MINIMUM_CREDIT_SCORE";
    }
}
