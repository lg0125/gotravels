package com.chris.gotravels.frameworks.biz.user.config;

import com.chris.gotravels.frameworks.biz.user.core.UserTransmitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import static com.chris.gotravels.frameworks.base.constant.FilterOrderConstant.USER_TRANSMIT_FILTER_ORDER;

/**
 * 用户配置自动装配
 * */
@ConditionalOnWebApplication
public class UserAutoConfiguration {
    /**
     * 用户信息传递过滤器
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter() {
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new UserTransmitFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(USER_TRANSMIT_FILTER_ORDER);

        return registration;
    }
}
