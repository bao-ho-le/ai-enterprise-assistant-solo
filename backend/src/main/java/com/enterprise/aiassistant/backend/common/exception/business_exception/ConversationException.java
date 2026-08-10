package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class ConversationException extends BusinessException {

    public ConversationException(
            ErrorCode errorCode
    ) {
        super(
                errorCode
        );
    }

    public ConversationException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(
                errorCode,
                errorCode.getMessage(),
                cause
        );
    }

    public ConversationException(
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
