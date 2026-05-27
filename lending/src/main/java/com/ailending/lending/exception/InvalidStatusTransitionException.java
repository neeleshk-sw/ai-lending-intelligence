package com.ailending.lending.exception;

import com.ailending.common.exception.BaseException;
import com.ailending.lending.domain.LoanStatus;

/**
 * Thrown when a requested {@link LoanStatus} transition violates the FR-8 workflow rules.
 */
public class InvalidStatusTransitionException extends BaseException {

    public InvalidStatusTransitionException(LoanStatus from, LoanStatus to) {
        super("Invalid loan status transition: " + from + " → " + to);
    }
}
