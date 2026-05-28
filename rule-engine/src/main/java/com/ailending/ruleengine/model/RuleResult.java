package com.ailending.ruleengine.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public final class RuleResult {

    private final String loanId;
    private final boolean passed;
    private final List<String> violations;
    private final Instant evaluatedAt;

    public RuleResult(String loanId, boolean passed, List<String> violations, Instant evaluatedAt) {
        this.loanId      = loanId;
        this.passed      = passed;
        this.violations  = Collections.unmodifiableList(violations);
        this.evaluatedAt = evaluatedAt;
    }

    public String getLoanId()           { return loanId; }
    public boolean isPassed()           { return passed; }
    public List<String> getViolations() { return violations; }
    public Instant getEvaluatedAt()     { return evaluatedAt; }
}
