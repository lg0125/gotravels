package com.chris.gotravels.frameworks.common.enums;

/**
 * 状态枚举
 * */
public enum StatusEnum {
    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * 失败
     */
    FAIL(1);

    private final Integer statusCode;

    StatusEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer code() {
        return this.statusCode;
    }

    public String strCode() {
        return String.valueOf(this.statusCode);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
/*
 * 抽象常用枚举码值，比如：删除标记枚举、标识枚举、操作类型以及状态枚举等
 * */