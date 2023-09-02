package com.chris.gotravels.frameworks.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例对象容器
 * */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Singleton {
    private static final ConcurrentHashMap<String, Object> SINGLE_OBJECT_POOL = new ConcurrentHashMap<>();


    /**
     * 根据 key 获取单例对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        return result == null ? null : (T) result;
    }

    /**
     * 根据 key 获取单例对象
     * result为空时，通过 supplier 构建单例对象并放入容器
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Supplier<T> supplier) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        if (result == null && (result = supplier.get()) != null) {
            SINGLE_OBJECT_POOL.put(key, result);
        }
        return result == null ? null : (T) result;
    }

    /**
     * 对象放入容器
     */
    public static void put(Object value) {
        put(value.getClass().getName(), value);
    }

    /**
     * 对象放入容器
     */
    public static void put(String key, Object value) {
        SINGLE_OBJECT_POOL.put(key, value);
    }
}

/*
*  5. 单例对象容器
*  提供一种单例的访问模式，单例的优点如下：
*       (1) 全局访问：单例对象可以在应用程序的任何地方被访问，而不需要传递对象的引用
*               这样,可以方便地共享对象的状态和功能，简化了对象之间的通信和协作
*       (2) 节省资源：由于只有一个对象实例存在，可以减少重复创建对象的开销
*               在需要频繁创建和销毁对象的情况下，单例对象可以显著节省系统资源，提高性能
*  比如说，发送邮件时需要加载邮件的模板文件，重复加载就比较浪费性能
*       完全可以加载一次后，将该对象放到单例对象容器中缓存，下次使用时直接获取就好
* */