package com.chris.gotravels.frameworks.base.safe;

import org.springframework.beans.factory.InitializingBean;

/**
 * FastJson安全模式，在开启之后，关闭类型隐式传递
 * */
public class FastJsonSafeMode implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}

/*
* 3. FastJSON 安全模式
*
*
* Fastjson 的 "autoType" 特性是指
*   在反序列化过程中，允许将 JSON 字符串自动转换为指定的 Java 类型
*   它提供了一种方便的方式，使得开发人员可以直接将 JSON 数据转换为相应的 Java 对象，而无需手动指定目标类
*
* FastJSON 安全模式会关闭该 autoType 特性
* */