package com.dong.dongpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@EnableAsync
@MapperScan("com.dong.dongpicturebackend.mapper") // 使用mapper的引用路径
@EnableAspectJAutoProxy(exposeProxy = true)
public class DongPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongPictureBackendApplication.class, args);
    }

}
