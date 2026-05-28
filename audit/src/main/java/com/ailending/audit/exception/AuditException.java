package com.ailending.audit.exception;

import com.ailending.common.exception.BaseException;

public class AuditException extends BaseException {

    public AuditException(String message) {
        super(message);
    }

    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
