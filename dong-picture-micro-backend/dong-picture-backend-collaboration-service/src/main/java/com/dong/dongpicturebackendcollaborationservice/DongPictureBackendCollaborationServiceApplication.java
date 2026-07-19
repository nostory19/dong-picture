package com.dong.dongpicturebackendcollaborationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.dong.dongpicturebackendserviceclient"})
@ComponentScan(basePackages = {"com.dong.dongpicturebackendcollaborationservice", "com.dong.dongpicturebackendcommon"})
@EnableAsync
public class DongPictureBackendCollaborationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongPictureBackendCollaborationServiceApplication.class, args);
    }
}
