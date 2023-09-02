package com.chris.gotravels.frameworks.convention.exception;

import com.chris.gotravels.frameworks.convention.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象项目中三类异常体系，客户端异常、服务端异常以及远程服务调用异常
 * */
@Getter
public abstract class AbstractException extends RuntimeException{
    public final String errorCode;

    public final String errorMessage;

    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);

        this.errorCode = errorCode.code();

        this.errorMessage = Optional
                .ofNullable(StringUtils.hasLength(message) ? message : null)
                .orElse(errorCode.message());
    }
}

/*
* 2. 定义抽象异常和客户端、服务端以及远程调用异常
*
* 什么是异常
*   异常机制是指当程序出现错误后，程序如何处理
*   具体来说，异常机制提供了程序退出的安全通道
*   当出现错误后，程序执行的流程发生改变，程序的控制权转移到异常处理器
*
* 在 Java 中，所有的异常都有一个共同的祖先 Throwable（可抛出）
*   Throwable 指定代码中可用异常传播机制通过 Java 应用程序传输的任何问题的共性
*   Throwable： 有两个重要的子类：Exception（异常）和 Error（错误），二者都是 Java 异常处理的重要子类，各自都包含大量子类
*
*   异常和错误的区别是：异常能被程序本身可以处理，错误是无法处理
*   Error（错误）:是程序无法处理的错误，表示运行应用程序中较严重问题
*       大多数错误与代码编写者执行的操作无关，而表示代码运行时 JVM（Java 虚拟机）出现的问题
*   Exception（异常）:是程序本身可以处理的异常。Exception 类有一个重要的子类 RuntimeException
*       RuntimeException 类及其子类表示“JVM 常用操作”引发的错误
* */
