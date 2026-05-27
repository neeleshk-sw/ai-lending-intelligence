package com.ailending.lending.service;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoanServiceImpl}.
 *
 * <p>No Spring context, no mocks — the in-memory implementation is tested directly.
 */
class LoanServiceImplTest {

    private LoanServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LoanServiceImpl();
    }

    // -----------------------------------------------------------------------
    // createLoan
    // -----------------------------------------------------------------------

    @Test
    void createLoan_returnsLoanWithGeneratedId() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        assertNotNull(loan.getLoanId(), "loanId must be generated (non-null)");
        assertFalse(loan.getLoanId().isBlank(), "loanId must not be blank");
    }

    @Test
    void createLoan_setsCorrectFields() {
        Loan loan = service.createLoan("cust-42", new BigDecimal("120000"), "USD");

        assertEquals("cust-42",                  loan.getCustomerId());
        assertEquals(new BigDecimal("120000"),   loan.getAmount());
        assertEquals("USD",                      loan.getCurrency());
    }

    @Test
    void createLoan_initialStatusIsAiReviewPending() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("10000"), "INR");

        assertEquals(LoanStatus.AI_REVIEW_PENDING, loan.getStatus(),
                "Initial status must be AI_REVIEW_PENDING");
    }

    @Test
    void createLoan_submittedAtAndUpdatedAtAreNonNull() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("10000"), "INR");

        assertNotNull(loan.getSubmittedAt(), "submittedAt must be set");
        assertNotNull(loan.getUpdatedAt(),   "updatedAt must be set");
    }

    @Test
    void createLoan_eachCallProducesUniqueLoanId() {
        Loan first  = service.createLoan("cust-1", new BigDecimal("50000"), "INR");
        Loan second = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        assertNotEquals(first.getLoanId(), second.getLoanId(),
                "Each createLoan call must produce a unique loanId");
    }

    // -----------------------------------------------------------------------
    // getLoan
    // -----------------------------------------------------------------------

    @Test
    void getLoan_returnsCreatedLoan() {
        Loan created  = service.createLoan("cust-5", new BigDecimal("75000"), "INR");
        Loan retrieved = service.getLoan(created.getLoanId());

        assertSame(created, retrieved, "getLoan must return the same Loan instance");
    }

    @Test
    void getLoan_throwsLoanNotFoundException_whenIdUnknown() {
        assertThrows(LoanNotFoundException.class,
                () -> service.getLoan("does-not-exist"),
                "getLoan must throw LoanNotFoundException for an unknown loanId");
    }

    @Test
    void getLoan_exceptionMessage_containsLoanId() {
        LoanNotFoundException ex = assertThrows(
                LoanNotFoundException.class,
                () -> service.getLoan("missing-loan-123"));

        assertTrue(ex.getMessage().contains("missing-loan-123"),
                "Exception message must reference the unknown loanId");
    }

    // -----------------------------------------------------------------------
    // updateStatus — legal transitions
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_aiReviewPending_toAiReviewCompleted_succeeds() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        Loan updated = service.updateStatus(loan.getLoanId(), LoanStatus.AI_REVIEW_COMPLETED);

        assertEquals(LoanStatus.AI_REVIEW_COMPLETED, updated.getStatus());
    }

    @Test
    void updateStatus_aiReviewCompleted_toUnderwriterReviewPending_succeeds() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");
        service.updateStatus(loan.getLoanId(), LoanStatus.AI_REVIEW_COMPLETED);

        Loan updated = service.updateStatus(loan.getLoanId(), LoanStatus.UNDERWRITER_REVIEW_PENDING);

        assertEquals(LoanStatus.UNDERWRITER_REVIEW_PENDING, updated.getStatus());
    }

    @Test
    void updateStatus_underwriterReviewPending_toManualOverride_succeeds() {
        Loan loan = advanceToUnderwriterReview();

        Loan updated = service.updateStatus(loan.getLoanId(), LoanStatus.MANUAL_OVERRIDE);

        assertEquals(LoanStatus.MANUAL_OVERRIDE, updated.getStatus());
    }

    @Test
    void updateStatus_underwriterReviewPending_toFinalDecisionCompleted_succeeds() {
        Loan loan = advanceToUnderwriterReview();

        Loan updated = service.updateStatus(loan.getLoanId(), LoanStatus.FINAL_DECISION_COMPLETED);

        assertEquals(LoanStatus.FINAL_DECISION_COMPLETED, updated.getStatus());
    }

    @Test
    void updateStatus_setsUpdatedAt() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");
        java.time.Instant before = loan.getUpdatedAt();

        // Tiny sleep so Instant.now() advances
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        service.updateStatus(loan.getLoanId(), LoanStatus.AI_REVIEW_COMPLETED);

        assertFalse(loan.getUpdatedAt().equals(before) && loan.getUpdatedAt().isBefore(before),
                "updatedAt must advance after a status update");
    }

    // -----------------------------------------------------------------------
    // updateStatus — illegal transitions
    // -----------------------------------------------------------------------

    @Test
    void updateStatus_throwsInvalidTransition_aiReviewPendingToUnderwriterReviewPending() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(loan.getLoanId(), LoanStatus.UNDERWRITER_REVIEW_PENDING),
                "Skipping AI_REVIEW_COMPLETED must throw InvalidStatusTransitionException");
    }

    @Test
    void updateStatus_throwsInvalidTransition_aiReviewPendingToFinalDecision() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(loan.getLoanId(), LoanStatus.FINAL_DECISION_COMPLETED));
    }

    @Test
    void updateStatus_throwsInvalidTransition_fromTerminalManualOverride() {
        Loan loan = advanceToUnderwriterReview();
        service.updateStatus(loan.getLoanId(), LoanStatus.MANUAL_OVERRIDE);

        // Terminal state — no further transitions allowed
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(loan.getLoanId(), LoanStatus.FINAL_DECISION_COMPLETED),
                "Terminal state MANUAL_OVERRIDE must reject any further transition");
    }

    @Test
    void updateStatus_throwsInvalidTransition_fromTerminalFinalDecision() {
        Loan loan = advanceToUnderwriterReview();
        service.updateStatus(loan.getLoanId(), LoanStatus.FINAL_DECISION_COMPLETED);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(loan.getLoanId(), LoanStatus.MANUAL_OVERRIDE),
                "Terminal state FINAL_DECISION_COMPLETED must reject any further transition");
    }

    @Test
    void updateStatus_throwsLoanNotFoundException_whenLoanUnknown() {
        assertThrows(LoanNotFoundException.class,
                () -> service.updateStatus("ghost-loan", LoanStatus.AI_REVIEW_COMPLETED));
    }

    @Test
    void updateStatus_exceptionMessage_containsFromAndTo() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");

        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.updateStatus(loan.getLoanId(), LoanStatus.FINAL_DECISION_COMPLETED));

        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("ai_review_pending") || msg.contains("→") || msg.contains("->"),
                "Exception message should describe the invalid transition");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Returns a Loan that has been advanced to UNDERWRITER_REVIEW_PENDING. */
    private Loan advanceToUnderwriterReview() {
        Loan loan = service.createLoan("cust-1", new BigDecimal("50000"), "INR");
        service.updateStatus(loan.getLoanId(), LoanStatus.AI_REVIEW_COMPLETED);
        service.updateStatus(loan.getLoanId(), LoanStatus.UNDERWRITER_REVIEW_PENDING);
        return loan;
    }
}
