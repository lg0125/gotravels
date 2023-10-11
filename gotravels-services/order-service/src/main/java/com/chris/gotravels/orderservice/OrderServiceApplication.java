package com.chris.gotravels.orderservice;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务应用启动器
 */
@SpringBootApplication
@MapperScan("com.chris.gotravels.orderservice.dao.mapper")
@EnableFeignClients("com.chris.gotravels.orderservice.remote")
@EnableCrane4j(enumPackages = "com.chris.gotravels.orderservice.common.enums")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
