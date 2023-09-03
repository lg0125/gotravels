package com.chris.gotravels.frameworks.pattern.builder;

import java.io.Serializable;

/**
 *  Builder模式抽象接口
 * */
public interface Builder<T> extends Serializable {
    /**
     * 构建方法
     *
     * @return 构建后的对象
     */
    T build();
}
