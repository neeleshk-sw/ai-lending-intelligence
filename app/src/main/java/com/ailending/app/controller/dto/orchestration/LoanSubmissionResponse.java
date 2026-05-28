package com.ailending.app.controller.dto.orchestration;

import com.ailending.orchestration.pipeline.LoanSubmissionResult;

import java.time.Instant;
import java.util.List;

public class LoanSubmissionResponse {
    private final String loanId;
    private final String status;
    private final boolean rulesPassed;
    private final List<String> violations;
    private final String auditId;
    private final Instant processedAt;

    private LoanSubmissionResponse(LoanSubmissionResult r) {
        this.loanId      = r.getLoan().getLoanId();
        this.status      = r.getLoan().getStatus().name();
        this.rulesPassed = r.getRuleResult().isPassed();
        this.violations  = r.getRuleResult().getViolations();
        this.auditId     = r.getAuditId();
        this.processedAt = r.getProcessedAt();
    }

    public static LoanSubmissionResponse from(LoanSubmissionResult r) {
        return new LoanSubmissionResponse(r);
    }

    public String getLoanId()           { return loanId; }
    public String getStatus()           { return status; }
    public boolean isRulesPassed()      { return rulesPassed; }
    public List<String> getViolations() { return violations; }
    public String getAuditId()          { return auditId; }
    public Instant getProcessedAt()     { return processedAt; }
}
