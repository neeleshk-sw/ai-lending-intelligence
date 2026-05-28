package com.ailending.integration.exception;

import com.ailending.common.exception.BaseException;

public class IntegrationException extends BaseException {

    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
