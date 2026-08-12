package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class DepartmentException extends BusinessException {

    public DepartmentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DepartmentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DepartmentException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
