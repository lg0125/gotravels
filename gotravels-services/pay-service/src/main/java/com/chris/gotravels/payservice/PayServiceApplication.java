package com.chris.gotravels.payservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 支付服务应用启动器
 */
@SpringBootApplication
@MapperScan("com.chris.gotravels.payservice.dao.mapper")
@EnableFeignClients("com.chris.gotravels.payservice.remote")
@EnableRetry
public class PayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }
}
