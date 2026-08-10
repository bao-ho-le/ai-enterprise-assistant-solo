package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class DocumentException extends BusinessException {

    public DocumentException(
            ErrorCode errorCode
    ) {
        super(
                errorCode
        );
    }

    public DocumentException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(
                errorCode,
                errorCode.getMessage(),
                cause
        );
    }

    public DocumentException(
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
