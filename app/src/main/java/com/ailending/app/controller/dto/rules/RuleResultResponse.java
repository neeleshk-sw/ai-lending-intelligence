package com.ailending.app.controller.dto.rules;

import com.ailending.ruleengine.model.RuleResult;

import java.time.Instant;
import java.util.List;

public class RuleResultResponse {
    private final String loanId;
    private final boolean passed;
    private final List<String> violations;
    private final Instant evaluatedAt;

    private RuleResultResponse(RuleResult r) {
        this.loanId      = r.getLoanId();
        this.passed      = r.isPassed();
        this.violations  = r.getViolations();
        this.evaluatedAt = r.getEvaluatedAt();
    }

    public static RuleResultResponse from(RuleResult r) {
        return new RuleResultResponse(r);
    }

    public String getLoanId()           { return loanId; }
    public boolean isPassed()           { return passed; }
    public List<String> getViolations() { return violations; }
    public Instant getEvaluatedAt()     { return evaluatedAt; }
}
