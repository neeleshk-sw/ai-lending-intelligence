package com.ailending.app.controller;

import com.ailending.app.controller.dto.loan.LoanResponse;
import com.ailending.app.controller.dto.workflow.AssignUnderwriterRequest;
import com.ailending.app.controller.dto.workflow.CompleteAiReviewRequest;
import com.ailending.app.controller.dto.workflow.RecordDecisionRequest;
import com.ailending.app.controller.dto.workflow.SubmitLoanRequest;
import com.ailending.app.controller.dto.workflow.WorkflowEventResponse;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.workflow.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lending/workflow/loans")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse submitLoan(@RequestBody SubmitLoanRequest request) {
        return LoanResponse.from(
                workflowService.submitLoan(
                        request.getCustomerId(), request.getAmount(), request.getCurrency()));
    }

    @PatchMapping("/{loanId}/ai-review")
    public LoanResponse completeAiReview(
            @PathVariable String loanId,
            @RequestBody CompleteAiReviewRequest request) {
        return LoanResponse.from(
                workflowService.completeAiReview(loanId, request.getConfidenceScore(), request.getModelUsed()));
    }

    @PatchMapping("/{loanId}/underwriter")
    public LoanResponse assignUnderwriter(
            @PathVariable String loanId,
            @RequestBody AssignUnderwriterRequest request) {
        return LoanResponse.from(
                workflowService.assignUnderwriter(loanId, request.getUnderwriterId()));
    }

    @PatchMapping("/{loanId}/decision")
    public LoanResponse recordDecision(
            @PathVariable String loanId,
            @RequestBody RecordDecisionRequest request) {
        return LoanResponse.from(
                workflowService.recordDecision(
                        loanId, LoanStatus.valueOf(request.getFinalStatus()), request.getReason()));
    }

    @GetMapping("/{loanId}/history")
    public List<WorkflowEventResponse> getHistory(@PathVariable String loanId) {
        return workflowService.getHistory(loanId).stream()
                .map(WorkflowEventResponse::from)
                .collect(Collectors.toList());
    }
}
