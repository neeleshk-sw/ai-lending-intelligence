package com.ailending.lending.service;

import com.ailending.lending.domain.Loan;
import com.ailending.lending.domain.LoanStatus;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link LoanService}.
 *
 * <p>Loan records are stored in a {@link ConcurrentHashMap} keyed by {@code loanId}.
 * Database persistence is a Phase 2 concern (see TDD §4.3). This implementation is
 * safe for concurrent access from multiple threads.
 *
 * <p>State transitions are validated against the FR-8 workflow graph:
 * <pre>
 *   AI_REVIEW_PENDING → AI_REVIEW_COMPLETED
 *   AI_REVIEW_COMPLETED → UNDERWRITER_REVIEW_PENDING
 *   UNDERWRITER_REVIEW_PENDING → MANUAL_OVERRIDE
 *   UNDERWRITER_REVIEW_PENDING → FINAL_DECISION_COMPLETED
 * </pre>
 */
@Service
public class LoanServiceImpl implements LoanService {

    // -----------------------------------------------------------------------
    // Legal FR-8 transition graph
    // -----------------------------------------------------------------------

    /**
     * Maps each {@link LoanStatus} to the set of statuses it may legally transition into.
     * Terminal states (MANUAL_OVERRIDE, FINAL_DECISION_COMPLETED) map to an empty set.
     */
    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<LoanStatus, Set<LoanStatus>> map = new EnumMap<>(LoanStatus.class);
        map.put(LoanStatus.AI_REVIEW_PENDING,
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LoanStatus.AI_REVIEW_COMPLETED))));
        map.put(LoanStatus.AI_REVIEW_COMPLETED,
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LoanStatus.UNDERWRITER_REVIEW_PENDING))));
        map.put(LoanStatus.UNDERWRITER_REVIEW_PENDING,
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                        LoanStatus.MANUAL_OVERRIDE, LoanStatus.FINAL_DECISION_COMPLETED))));
        map.put(LoanStatus.MANUAL_OVERRIDE,           Collections.emptySet());
        map.put(LoanStatus.FINAL_DECISION_COMPLETED,  Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    // -----------------------------------------------------------------------
    // In-memory store
    // -----------------------------------------------------------------------

    private final ConcurrentHashMap<String, Loan> store = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // LoanService implementation
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>A UUID is generated as the {@code loanId}. The initial status is
     * {@link LoanStatus#AI_REVIEW_PENDING}.
     */
    @Override
    public Loan createLoan(String customerId, BigDecimal amount, String currency) {
        String loanId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Loan loan = Loan.builder()
                .loanId(loanId)
                .customerId(customerId)
                .amount(amount)
                .currency(currency)
                .status(LoanStatus.AI_REVIEW_PENDING)
                .submittedAt(now)
                .updatedAt(now)
                .build();

        store.put(loanId, loan);
        return loan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loan getLoan(String loanId) {
        Loan loan = store.get(loanId);
        if (loan == null) {
            throw new LoanNotFoundException(loanId);
        }
        return loan;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The transition is validated against {@link #ALLOWED_TRANSITIONS} before
     * applying the change.
     */
    @Override
    public Loan updateStatus(String loanId, LoanStatus newStatus) {
        Loan loan = getLoan(loanId);  // throws LoanNotFoundException if absent

        LoanStatus currentStatus = loan.getStatus();
        Set<LoanStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());

        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        loan.setStatus(newStatus);
        loan.setUpdatedAt(Instant.now());
        return loan;
    }
}
