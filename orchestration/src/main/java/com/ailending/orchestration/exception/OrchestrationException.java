package com.ailending.orchestration.exception;

import com.ailending.common.exception.BaseException;

public class OrchestrationException extends BaseException {

    public OrchestrationException(String message) {
        super(message);
    }

    public OrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
