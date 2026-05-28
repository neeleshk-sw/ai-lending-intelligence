package com.ailending.orchestration.pipeline;

import com.ailending.ruleengine.model.RuleInput;

import java.math.BigDecimal;

public interface LoanSubmissionPipeline {

    LoanSubmissionResult process(String customerId, BigDecimal amount, String currency,
                                  RuleInput ruleInput);
}
