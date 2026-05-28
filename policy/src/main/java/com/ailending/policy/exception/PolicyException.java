package com.ailending.policy.exception;

import com.ailending.common.exception.BaseException;

public class PolicyException extends BaseException {

    public PolicyException(String message) {
        super(message);
    }

    public PolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
