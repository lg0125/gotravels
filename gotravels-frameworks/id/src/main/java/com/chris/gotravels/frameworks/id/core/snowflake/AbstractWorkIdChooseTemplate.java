package com.chris.gotravels.frameworks.id.core.snowflake;

import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.chris.gotravels.frameworks.id.toolkit.SnowflakeIdUtil;

/**
 * 雪花算法模板生成
 */
@Slf4j
public abstract class AbstractWorkIdChooseTemplate {
    /**
     * 是否使用 {@link SystemClock} 获取当前时间戳
     */
    @Value("${framework.distributed.id.snowflake.is-use-system-clock:false}")
    private boolean isUseSystemClock;

    /**
     * 根据自定义策略获取 WorkId 生成器
     */
    protected abstract WorkIdWrapper chooseWorkId();

    /**
     * 选择 WorkId 并初始化雪花
     */
    public void chooseAndInit() {
        // 模板方法模式: 通过抽象方法获取 WorkId 包装器创建雪花算法
        WorkIdWrapper workIdWrapper = chooseWorkId();
        long workId = workIdWrapper.getWorkId();
        long dataCenterId = workIdWrapper.getDataCenterId();
        Snowflake snowflake = new Snowflake(workId, dataCenterId, isUseSystemClock);
        log.info("Snowflake type: {}, workId: {}, dataCenterId: {}", this.getClass().getSimpleName(), workId, dataCenterId);
        SnowflakeIdUtil.initSnowflake(snowflake);
    }
}
/*
* 1. 雪花算法生成器
* 首先，雪花算法生成器是为了解决工作机器 ID 重复问题，底层创建雪花算法依然采用推特那一套逻辑
* 通过分配抢占的方式获取机器 ID，存储机器 ID 的位置就是个比较难选择的事情
* 默认是通过 Redis 缓存存储，但是当项目中没有 Redis 时，采用随机数方式获取机器 ID
*
* 1.1 定义雪花算法获取机器ID模板抽象类
* 采用模板方法模式，获取Redis或者随机数提供的机器ID
* */
