package com.chris.gotravels.frameworks.log.annotation;

import lombok.Data;

/**
 * ILog 日志打印实体
 */
@Data
public class ILogPrintDTO {
    /**
     * 开始时间
     */
    private String beginTime;

    /**
     * 请求入参
     */
    private Object[] inputParams;

    /**
     * 返回参数
     */
    private Object outputParams;
}
/*
* 定义日志打印规约参数：开始时间、请求入参、以及返回参数等
* */

