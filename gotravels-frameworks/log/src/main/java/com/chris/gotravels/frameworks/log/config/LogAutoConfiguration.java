package com.chris.gotravels.frameworks.log.config;

import com.chris.gotravels.frameworks.log.annotation.ILog;
import com.chris.gotravels.frameworks.log.core.ILogPrintAspect;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动装配
 */
public class LogAutoConfiguration {
    /**
     * {@link ILog} 日志打印 AOP 切面
     */
    @Bean
    public ILogPrintAspect iLogPrintAspect() {
        return new ILogPrintAspect();
    }
}
