package com.chris.gotravels.frameworks.base;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 应用上下文处理器
 * */
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * 通过类型获得IOC容器
     * */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * 通过名称获得IOC容器
     * */
    public static Object getBean(String name) {
        return CONTEXT.getBean(name);
    }

    /**
     * 通过类型和名称获得IOC容器
     * */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * 通过类型获得IOC容器的集合
     * */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * 查找Bean是否有注解
     * */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * 获得应用上下文
     * */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}

/*
* 2. 封装应用上下文ApplicationContext
*
* 很多时候需要在非 Spring Bean 中使用到 Spring Bean，比如定义线程池任务实现类时，可能需要获取到 Spring Bean
* 如果说在 Spring Bean 中获取 Spring Bean，只需要通过构造器或者 @Autowired 或 @Resource 就可以了，但非 Spring Bean 却不行
*
* 基于以上诉求，依赖 Spring 提供的 ApplicationContextAware接口，将 Spring IOC 容器的对象放到一个自定义容器中，并持有 Spring IOC 容器
* 这样，就可以通过自定义容器访问 Spring IOC 容器获取 Spring Bean
* */