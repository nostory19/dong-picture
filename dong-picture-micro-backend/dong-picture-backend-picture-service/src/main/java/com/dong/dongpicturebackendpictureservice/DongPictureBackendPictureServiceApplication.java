package com.dong.dongpicturebackendpictureservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.dong.dongpicturebackendserviceclient"})
@ComponentScan(basePackages = {"com.dong.dongpicturebackendpictureservice", "com.dong.dongpicturebackendcommon"})
@MapperScan("com.dong.dongpicturebackendpictureservice.infrastructure.mapper")
@EnableAsync
@EnableScheduling
public class DongPictureBackendPictureServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongPictureBackendPictureServiceApplication.class, args);
    }
}
