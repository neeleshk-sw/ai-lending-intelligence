package com.ailending.workflow.exception;

import com.ailending.common.exception.BaseException;

public class WorkflowException extends BaseException {

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
