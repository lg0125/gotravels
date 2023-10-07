package com.chris.gotravels.userservice.common.constant;

/**
 * Redis Key 定义常量类
 */
public final class RedisKeyConstant {
    /**
     * 用户注册锁，Key Prefix + 用户名
     */
    public static final String LOCK_USER_REGISTER = "gotravels-user-service:lock:user-register:";

    /**
     * 用户注销锁，Key Prefix + 用户名
     */
    public static final String USER_DELETION = "gotravels-user-service:user-deletion:";

    /**
     * 用户注册可复用用户名分片，Key Prefix + Idx
     */
    public static final String USER_REGISTER_REUSE_SHARDING = "gotravels-user-service:user-reuse:";

    /**
     * 用户乘车人列表，Key Prefix + 用户名
     */
    public static final String USER_PASSENGER_LIST = "gotravels-user-service:user-passenger-list:";
}
