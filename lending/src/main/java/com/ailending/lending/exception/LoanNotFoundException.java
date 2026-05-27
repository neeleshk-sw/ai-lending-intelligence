package com.ailending.lending.exception;

import com.ailending.common.exception.BaseException;

/**
 * Thrown when a loan with the requested ID cannot be found.
 */
public class LoanNotFoundException extends BaseException {

    public LoanNotFoundException(String loanId) {
        super("Loan not found: " + loanId);
    }
}
