package com.dong.dongpicturebackenduserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.dong.dongpicturebackenduserservice.infrastructure.mapper")
@EnableFeignClients(basePackages = "com.dong.dongpicturebackendserviceclient")
public class DongPictureBackendUserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DongPictureBackendUserServiceApplication.class, args);
    }
}
