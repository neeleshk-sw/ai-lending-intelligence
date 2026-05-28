package com.ailending.ruleengine.service;

import com.ailending.ruleengine.exception.RuleEvaluationException;
import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.rule.Rule;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RuleEvaluationServiceImpl implements RuleEvaluationService {

    private final List<Rule> rules;

    public RuleEvaluationServiceImpl(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public RuleResult evaluate(RuleInput input) {
        if (input == null) {
            throw new RuleEvaluationException("RuleInput must not be null");
        }

        List<String> violations = rules.stream()
                .map(rule -> rule.evaluate(input))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return new RuleResult(input.getLoanId(), violations.isEmpty(), violations, Instant.now());
    }
}
