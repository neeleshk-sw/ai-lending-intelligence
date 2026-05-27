package com.ailending.lending.domain;

/**
 * Loan workflow states as defined in FR-8.
 *
 * <p>Valid transition graph:
 * <pre>
 *   AI_REVIEW_PENDING
 *        ↓
 *   AI_REVIEW_COMPLETED
 *        ↓
 *   UNDERWRITER_REVIEW_PENDING
 *        ↓              ↓
 *   MANUAL_OVERRIDE   FINAL_DECISION_COMPLETED
 * </pre>
 *
 * <p>AI is advisory only — the system must never autonomously decide a final loan outcome
 * (ADR-003). All transitions after {@code AI_REVIEW_COMPLETED} require a human underwriter.
 */
public enum LoanStatus {

    /** Initial state when a loan is submitted for AI-assisted review. */
    AI_REVIEW_PENDING,

    /** AI analysis is done; awaiting handoff to a human underwriter. */
    AI_REVIEW_COMPLETED,

    /**
     * An underwriter has been assigned and is actively reviewing the application.
     * {@code assignedUnderwriter} must be set on the {@link Loan} before entering this state.
     */
    UNDERWRITER_REVIEW_PENDING,

    /**
     * The underwriter chose to override the AI recommendation.
     * A terminal state — the loan record becomes read-only.
     */
    MANUAL_OVERRIDE,

    /**
     * The final lending decision (approve or reject) has been recorded by the underwriter.
     * A terminal state.
     */
    FINAL_DECISION_COMPLETED
}
