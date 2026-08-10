package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class LLMException extends BusinessException {

    public LLMException(
            ErrorCode errorCode
    ) {
        super(
                errorCode
        );
    }

    public LLMException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(
                errorCode,
                errorCode.getMessage(),
                cause
        );
    }

    public LLMException(
            ErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(
                errorCode,
                message,
                cause
        );
    }
}
