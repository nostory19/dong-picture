package com.dong.dongpicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author by hongdou
 * @date 2025/7/31.
 * @DESC: 测试redis的单元测试
 */
@SpringBootTest
public class RedisStringTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;  // 这个bean就理解为可以直接使用redis的客户端

    @Test
    public void restRedisStringOperations() {
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        // key and value
        String key = "testKey";
        String value = "testValue";

        // 测试新增或更新操作
        valueOps.set(key, value);
        String storedValue = valueOps.get(key);
        assertEquals(value, storedValue, "存储的值与预期不一致");

        // 测试修改操作
        String updateValue = "updateValue";
        valueOps.set(key, updateValue);
        storedValue = valueOps.get(key);
        assertEquals(updateValue, storedValue, "更新后的值与预期不一致");

        // 测试查询操作
        storedValue = valueOps.get(key);
        assertNotNull(storedValue, "查询的值为空");
        assertEquals(updateValue, storedValue, "查询的值与预期不一致");

        // 测试删除操作
        stringRedisTemplate.delete(key);
        storedValue = valueOps.get(key);
        assertNull(storedValue, "删除后的值不为空");
    }
}
