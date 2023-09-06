package com.chris.gotravels.frameworks.database.handler;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

/**
 *
 * */
public class CustomerIdGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) {
        return null;
    }
}
/*
* 5. 自定义雪花算法
*   定义雪花算法类，并替换MyBatisPlus原有的雪花ID生成器
* */
