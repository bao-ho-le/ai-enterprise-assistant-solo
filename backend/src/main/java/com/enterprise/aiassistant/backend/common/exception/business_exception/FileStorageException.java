package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class FileStorageException extends BusinessException {
    public FileStorageException(
            ErrorCode errorCode
    ) {
        super(
                errorCode
        );
    }

    public FileStorageException(
            ErrorCode errorCode,
            Throwable cause
    ) {
        super(
                errorCode,
                errorCode.getMessage(),
                cause
        );
    }

    public FileStorageException(
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
