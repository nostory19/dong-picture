package com.dong.dongpicturebackendspaceservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.dong.dongpicturebackendspaceservice.infrastructure.mapper")
@EnableFeignClients(basePackages = "com.dong.dongpicturebackendserviceclient.application.service")
@ComponentScan(basePackages = {"com.dong.dongpicturebackendspaceservice", "com.dong.dongpicturebackendcommon"})
public class DongPictureBackendSpaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongPictureBackendSpaceServiceApplication.class, args);
    }
}
