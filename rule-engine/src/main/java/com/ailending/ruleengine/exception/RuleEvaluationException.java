package com.ailending.ruleengine.exception;

import com.ailending.common.exception.BaseException;

public class RuleEvaluationException extends BaseException {

    public RuleEvaluationException(String message) {
        super(message);
    }

    public RuleEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
