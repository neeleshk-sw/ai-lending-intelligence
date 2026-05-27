package com.ailending.app.controller;

import com.ailending.app.controller.dto.loan.CreateLoanRequest;
import com.ailending.app.controller.dto.loan.LoanResponse;
import com.ailending.app.controller.dto.loan.UpdateLoanStatusRequest;
import com.ailending.lending.domain.Loan;
import com.ailending.lending.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for loan lifecycle management (FR-8).
 *
 * <p>All final lending decisions are made by human underwriters — this controller
 * only manages the workflow state machine and never approves or rejects loans
 * autonomously (ADR-003).
 */
@RestController
@RequestMapping("/api/lending/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * Create a new loan application.
     *
     * <p>The loan is created with status {@code AI_REVIEW_PENDING}.
     * A UUID {@code loanId} is generated and returned in the response.
     *
     * @param request borrower ID, loan amount, and currency
     * @return the newly created loan (HTTP 201)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse createLoan(@RequestBody CreateLoanRequest request) {
        Loan loan = loanService.createLoan(
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency()
        );
        return LoanResponse.from(loan);
    }

    /**
     * Retrieve a loan by its ID.
     *
     * @param loanId UUID of the loan
     * @return the loan (HTTP 200), or HTTP 404 if not found
     */
    @GetMapping("/{loanId}")
    public LoanResponse getLoan(@PathVariable String loanId) {
        return LoanResponse.from(loanService.getLoan(loanId));
    }

    /**
     * Advance a loan to a new workflow status.
     *
     * <p>Only transitions that are legal per the FR-8 graph are accepted;
     * invalid transitions return HTTP 400.
     *
     * @param loanId  UUID of the loan to update
     * @param request the target status
     * @return the updated loan (HTTP 200)
     */
    @PatchMapping("/{loanId}/status")
    public LoanResponse updateStatus(
            @PathVariable String loanId,
            @RequestBody UpdateLoanStatusRequest request) {
        Loan updated = loanService.updateStatus(loanId, request.getStatus());
        return LoanResponse.from(updated);
    }
}
