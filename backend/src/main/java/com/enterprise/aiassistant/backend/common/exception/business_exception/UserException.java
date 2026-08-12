package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class UserException extends BusinessException {


        public UserException(ErrorCode errorCode) {
            super(errorCode);
        }

        public UserException(ErrorCode errorCode, Throwable cause) {
            super(errorCode, errorCode.getMessage(), cause);
        }


}
