package com.chris.gotravels.frameworks.base.constant;

/**
 * 用户常量
 * */
public final class UserConstant {
    /**
     * 用户ID的key
     * */
    public static final String USER_ID_KEY = "userId";

    /**
     * 用户名的key
     * */
    public static final String USER_NAME_KEY = "username";

    /**
     * 用户真实名称的key
     * */
    public static final String REAL_NAME_KEY = "realName";

    /**
     * 用户Token的key
     * */
    public static final String USER_TOKEN_KEY = "token";
}

/*
* 1. 定义全局配置常量
*
* 为什么要定义这个全局变量定义常量类？
*   如果没有定义在顶级组件库中，
*   那么，这些常量都是定义在各个组件库中的，比如用户的常量定义在用户的组件库中
*
*   这个常量肯定不是只在用户组件库中使用
*   因为在网关中，将用户 Token 进行解析，并放到 HTTP Header 中，最终放到用户请求上下文，也需要用到这些用户常量
*
*   类似于这种多处使用的变量，分别定义肯定是不优雅的，
*   那么，我们只能找个地方将这个定义，并被用户和网关同时使用
*   基础组件库因为要被每个组件库依赖，所以是最好的选择
* */
