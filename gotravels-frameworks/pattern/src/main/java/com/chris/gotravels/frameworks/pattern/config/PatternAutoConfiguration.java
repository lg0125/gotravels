package com.chris.gotravels.frameworks.pattern.config;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainContext;
import com.chris.gotravels.frameworks.pattern.strategy.AbstractStrategyChoose;
import org.springframework.context.annotation.Bean;

/**
 *  设计模式自动装配
 * */
public class PatternAutoConfiguration {
    /**
     * 策略模式选择器
     */
    @Bean
    public AbstractStrategyChoose abstractStrategyChoose() {
        return new AbstractStrategyChoose();
    }

    /**
     * 责任链上下文
     */
    @Bean
    public AbstractChainContext abstractChainContext() {
        return new AbstractChainContext();
    }
}
