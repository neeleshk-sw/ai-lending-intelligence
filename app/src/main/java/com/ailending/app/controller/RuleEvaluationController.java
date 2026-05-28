package com.ailending.app.controller;

import com.ailending.app.controller.dto.rules.EvaluateRulesRequest;
import com.ailending.app.controller.dto.rules.RuleResultResponse;
import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.service.RuleEvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lending/rules")
public class RuleEvaluationController {

    private final RuleEvaluationService ruleEvaluationService;

    public RuleEvaluationController(RuleEvaluationService ruleEvaluationService) {
        this.ruleEvaluationService = ruleEvaluationService;
    }

    @PostMapping("/evaluate")
    public RuleResultResponse evaluate(@RequestBody EvaluateRulesRequest request) {
        RuleInput input = RuleInput.builder()
                .loanId(request.getLoanId())
                .customerId(request.getCustomerId())
                .monthlyIncome(request.getMonthlyIncome())
                .requestedAmount(request.getRequestedAmount())
                .existingMonthlyDebt(
                        request.getExistingMonthlyDebt() != null
                        ? request.getExistingMonthlyDebt()
                        : java.math.BigDecimal.ZERO)
                .creditScore(request.getCreditScore())
                .kycVerified(request.isKycVerified())
                .amlClear(request.isAmlClear())
                .build();

        RuleResult result = ruleEvaluationService.evaluate(input);
        return RuleResultResponse.from(result);
    }
}
