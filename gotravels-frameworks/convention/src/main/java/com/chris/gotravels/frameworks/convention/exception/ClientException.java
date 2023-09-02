package com.chris.gotravels.frameworks.convention.exception;

import com.chris.gotravels.frameworks.convention.errorcode.BaseErrorCode;
import com.chris.gotravels.frameworks.convention.errorcode.IErrorCode;

/**
 * 客户端异常
 * */
public class ClientException extends AbstractException{
    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException" +
                '{' +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
