package com.chris.gotravels.frameworks.convention.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 5679018624309023727L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    private String code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}

/*
* 4. 封装公共响应对象
*
*   如果接口返回的内容格式不规范，会对前端开发人员造成困扰
*   对接口做一些优化，使用统一的返回对象，使得每次请求后返回的结果更加规范
*
* 统一返回 Result 有什么好处呢？
*   1. 返回码
*   code，这是统一返回体的核心字段，用来标记该请求执行返回结果
*   如果返回正常，大部分网站接口返回的往往是 0
*   另外，根据阿里 Java 开发手册（崇山版）错误码列表规定，返回 00000，这里以前者为主
*   返回码不止是正确的，同样包含错误
*
*   2. 返回消息
*   message，当前请求状态码描述，如果接口请求成功，为空。如果接口请求异常，这里是对应的异常错误提示信息
*
*   3. 返回数据
*   data，当前请求返回响应数据
*
* 4. 请求 ID
*   requestId，该 ID 也可以当作链路追踪 TraceID 来看，用来串联当前请求的上下文，以及分布式链路追踪错误排查的依据
*
* 通过统一返回体约束，可以很好与前端形成良好规范，避免多个项目“各自为战”，不方便重构和监控
* */
