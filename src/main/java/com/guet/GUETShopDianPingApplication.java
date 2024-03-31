package com.guet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.guet.mapper")
@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
public class GUETShopDianPingApplication {
    public static void main (String[] args) {
        SpringApplication.run(GUETShopDianPingApplication.class, args);
    }
}