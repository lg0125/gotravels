package com.chris.gotravels.frameworks.common.enums;

/**
 * 删除标记枚举
 * */
public enum DelEnum {
    /**
     * 正常状态
     */
    NORMAL(0),

    /**
     * 删除状态
     */
    DELETE(1);

    private final Integer statusCode;

    DelEnum(Integer statusCode) {
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
