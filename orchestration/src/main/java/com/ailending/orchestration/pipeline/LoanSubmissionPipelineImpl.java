package com.ailending.orchestration.pipeline;

import com.ailending.audit.model.AuditEventType;
import com.ailending.audit.model.AuditRecord;
import com.ailending.audit.service.AuditService;
import com.ailending.lending.domain.Loan;
import com.ailending.orchestration.exception.OrchestrationException;
import com.ailending.ruleengine.model.RuleInput;
import com.ailending.ruleengine.model.RuleResult;
import com.ailending.ruleengine.service.RuleEvaluationService;
import com.ailending.workflow.service.WorkflowService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class LoanSubmissionPipelineImpl implements LoanSubmissionPipeline {

    private final WorkflowService workflowService;
    private final RuleEvaluationService ruleEvaluationService;
    private final AuditService auditService;

    public LoanSubmissionPipelineImpl(WorkflowService workflowService,
                                       RuleEvaluationService ruleEvaluationService,
                                       AuditService auditService) {
        this.workflowService       = workflowService;
        this.ruleEvaluationService = ruleEvaluationService;
        this.auditService          = auditService;
    }

    @Override
    public LoanSubmissionResult process(String customerId, BigDecimal amount, String currency,
                                         RuleInput ruleInput) {
        try {
            // Step 1: create loan via workflow
            Loan loan = workflowService.submitLoan(customerId, amount, currency);

            // Step 2: evaluate deterministic rules
            RuleResult ruleResult = ruleEvaluationService.evaluate(ruleInput);

            // Step 3: record audit trail
            AuditRecord auditRecord = AuditRecord.builder()
                    .loanId(loan.getLoanId())
                    .customerId(loan.getCustomerId())
                    .eventType(AuditEventType.AI_REVIEW_COMPLETED)
                    .performedBy("system")
                    .decision(ruleResult.isPassed() ? "RULES_PASSED" : "RULES_FAILED")
                    .notes(ruleResult.getViolations().isEmpty()
                            ? null
                            : String.join("; ", ruleResult.getViolations()))
                    .build();
            auditService.record(auditRecord);

            return new LoanSubmissionResult(loan, ruleResult, auditRecord.getAuditId(), Instant.now());

        } catch (OrchestrationException e) {
            throw e;
        } catch (Exception e) {
            throw new OrchestrationException(
                    "Loan submission pipeline failed for customer " + customerId + ": " + e.getMessage(), e);
        }
    }
}
