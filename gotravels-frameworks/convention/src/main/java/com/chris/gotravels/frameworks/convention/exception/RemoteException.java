package com.chris.gotravels.frameworks.convention.exception;

import com.chris.gotravels.frameworks.convention.errorcode.BaseErrorCode;
import com.chris.gotravels.frameworks.convention.errorcode.IErrorCode;

/**
 * 远程服务调用异常
 * */
public class RemoteException extends AbstractException{
    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }
}
