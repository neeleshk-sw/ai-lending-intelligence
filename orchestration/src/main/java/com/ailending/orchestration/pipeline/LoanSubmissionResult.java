package com.ailending.orchestration.pipeline;

import com.ailending.lending.domain.Loan;
import com.ailending.ruleengine.model.RuleResult;

import java.time.Instant;

public final class LoanSubmissionResult {

    private final Loan loan;
    private final RuleResult ruleResult;
    private final String auditId;
    private final Instant processedAt;

    public LoanSubmissionResult(Loan loan, RuleResult ruleResult,
                                 String auditId, Instant processedAt) {
        this.loan        = loan;
        this.ruleResult  = ruleResult;
        this.auditId     = auditId;
        this.processedAt = processedAt;
    }

    public Loan getLoan()             { return loan; }
    public RuleResult getRuleResult() { return ruleResult; }
    public String getAuditId()        { return auditId; }
    public Instant getProcessedAt()   { return processedAt; }
}
