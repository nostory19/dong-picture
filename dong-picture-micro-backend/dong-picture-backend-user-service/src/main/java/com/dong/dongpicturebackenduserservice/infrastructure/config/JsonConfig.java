package com.dong.dongpicturebackenduserservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author by hongdou
 * @date 2025/5/31.
 * @DESC: spring mvc json配置
 * 后端通过sprign mvc将返回数据转成json，默认使用jackson将对象转为json，
 * 修改为发现是long类型，转换为string类型
 */
@JsonComponent
public class JsonConfig {
    /**
     * 添加long 转json精度丢失的配置
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder){
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }
}
