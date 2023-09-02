package com.chris.gotravels.frameworks.base.init;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用初始化后置处理器，防止Spring事件被多次执行
 * */
@RequiredArgsConstructor
public class ApplicationContentPostProcessor implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext applicationContext;

    /**
     * 执行标识，确保Spring事件 {@link ApplicationReadyEvent} 有且执行一次
     */
    private final AtomicBoolean executeOnlyOnce = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if(!executeOnlyOnce.compareAndSet(false, true))
            return;

        applicationContext.publishEvent(new ApplicationInitializingEvent(this));
    }
}

/*
* 4.2 定义应用初始化后置处理器，防止 Spring 事件被多次执行
*       通过锁来保证同一时间只有一个事件进行初始化
*       初始化后设置一个标识，下次如果再触发，就不再执行，类似于幂等处理的逻辑
* */
