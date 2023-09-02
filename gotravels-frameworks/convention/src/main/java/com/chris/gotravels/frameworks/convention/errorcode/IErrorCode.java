package com.chris.gotravels.frameworks.convention.errorcode;

/**
 * 平台错误码
 * */
public interface IErrorCode {
    /**
     * 错误码
     */
    String code();

    /**
     * 错误信息
     */
    String message();
}

/*
* 1. 定义异常码抽象和公共异常码
*
* 为什么要做异常码规范和抽象异常码？
*   异常码的不统一，直接后果就是返回结果异常码混乱，没有规律，也不方便排查问题
* */
