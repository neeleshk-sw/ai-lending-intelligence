package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AmlComplianceRule implements Rule {

    @Override
    public Optional<String> evaluate(RuleInput input) {
        if (!input.isAmlClear()) {
            return Optional.of("AML compliance check has not been cleared for this borrower");
        }
        return Optional.empty();
    }

    @Override
    public String ruleName() {
        return "AML_COMPLIANCE";
    }
}
