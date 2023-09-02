package com.chris.gotravels.frameworks.convention.errorcode;

/**
 * 基础错误码定义
 * */
public enum BaseErrorCode implements IErrorCode{
    // ========== 一级宏观错误码 客户端错误 ==========
    CLIENT_ERROR("A000001", "用户端错误"),

    // ========== 二级宏观错误码 用户注册错误 ==========
    USER_REGISTER_ERROR("A000100", "用户注册错误"),
    USER_NAME_VERIFY_ERROR("A000110", "用户名校验失败"),
    USER_NAME_EXIST_ERROR("A000111", "用户名已存在"),
    USER_NAME_SENSITIVE_ERROR("A000112", "用户名包含敏感词"),
    USER_NAME_SPECIAL_CHARACTER_ERROR("A000113", "用户名包含特殊字符"),
    PASSWORD_VERIFY_ERROR("A000120", "密码校验失败"),
    PASSWORD_SHORT_ERROR("A000121", "密码长度不够"),
    PHONE_VERIFY_ERROR("A000151", "手机格式校验失败"),

    // ========== 二级宏观错误码 系统请求缺少幂等Token ==========
    IDEMPOTENT_TOKEN_NULL_ERROR("A000200", "幂等Token为空"),
    IDEMPOTENT_TOKEN_DELETE_ERROR("A000201", "幂等Token已被使用或失效"),

    // ========== 一级宏观错误码 系统执行出错 ==========
    SERVICE_ERROR("B000001", "系统执行出错"),
    // ========== 二级宏观错误码 系统执行超时 ==========
    SERVICE_TIMEOUT_ERROR("B000100", "系统执行超时"),

    // ========== 一级宏观错误码 调用第三方服务出错 ==========
    REMOTE_ERROR("C000001", "调用第三方服务出错");


    private final String code;

    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}

/*
* 1. 定义异常码抽象和公共异常码
*
* 阿里巴巴开发手册如何规定异常码
*
* 错误码为字符串类型，共 5 位
*   分成两个部分：错误产生来源 + 四位数字编号
* 错误产生来源分为 A / B / C
*    A 表示错误来源于用户，比如参数错误，用户安装版本过低，用户支付超时等问题
*    B 表示错误来源于当前系统，往往是业务逻辑出错，或程序健壮性差等问题
*    C 表示错误来源于第三方服务，比如 CDN 服务出错，消息投递超时等问题
 * 四位数字编号从 0001 到 9999，大类之间的步长间距预留 100
*
* 阿里巴巴开发手册中的核心思想是规定常用异常码，能复用就复用，实在不行就通过异常码平台去创建，先到先得
* */