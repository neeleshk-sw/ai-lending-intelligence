package com.ailending.ruleengine.service;

import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;

public interface RuleEvaluationService {

    RuleResult evaluate(RuleInput input);
}
