package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class KycVerificationRule implements Rule {

    @Override
    public Optional<String> evaluate(RuleInput input) {
        if (!input.isKycVerified()) {
            return Optional.of("KYC verification has not been completed for this borrower");
        }
        return Optional.empty();
    }

    @Override
    public String ruleName() {
        return "KYC_VERIFICATION";
    }
}
