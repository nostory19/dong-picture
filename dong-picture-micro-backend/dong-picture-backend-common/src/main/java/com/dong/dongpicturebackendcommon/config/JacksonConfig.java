package com.dong.dongpicturebackendcommon.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            // 序列化：Long → String（防止前端精度丢失）
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
            // 反序列化：String → Long（前端发回的字符串 ID 转回 Long）
            builder.deserializerByType(Long.class, new StringToLongDeserializer());
            builder.deserializerByType(long.class, new StringToLongDeserializer());
        };
    }

    /**
     * 自定义反序列化器：将 JSON 字符串或数字统一转为 Java Long。
     * 解决前端将雪花 ID 作为字符串发送时 Jackson 无法自动转换的问题。
     * 同时兼容前端直接发送数字的旧行为。
     */
    static class StringToLongDeserializer extends StdDeserializer<Long> {
        StringToLongDeserializer() {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            switch (p.currentToken()) {
                case VALUE_STRING:
                    // 前端发送字符串 "2077767896272724000"
                    String text = p.getText();
                    if (text == null || text.isEmpty()) {
                        return null;
                    }
                    return Long.valueOf(text);
                case VALUE_NUMBER_INT:
                    // 前端直接发送数字 1
                    return p.getLongValue();
                case VALUE_NULL:
                    return null;
                default:
                    return (Long) ctxt.handleUnexpectedToken(Long.class, p);
            }
        }
    }
}
