package com.ailending.ruleengine.rule;

import com.ailending.ruleengine.model.RuleInput;

import java.util.Optional;

public interface Rule {

    Optional<String> evaluate(RuleInput input);

    String ruleName();
}
