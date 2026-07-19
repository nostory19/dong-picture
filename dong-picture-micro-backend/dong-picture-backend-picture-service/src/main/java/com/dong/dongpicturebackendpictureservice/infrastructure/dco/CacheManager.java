package com.dong.dongpicturebackendpictureservice.infrastructure.dco;

import cn.hutool.core.util.RandomUtil;
import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.AddResult;
import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.TopK;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheManager {

    @Resource
    private TopK hotKeyDetector;

    @Resource
    private Cache<String, Object> localCache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final Integer redisExpireTime = 60 * 30;

    public Object getValueCache(String key) {
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            log.info("L1 cache hit: {}", key);
            hotKeyDetector.add(key, 1);
            return value;
        }

        Object redisValue = redisTemplate.opsForValue().get(RedisKeyUtil.buildRedisKey(key));
        if (redisValue == null) {
            return null;
        }

        AddResult addResult = hotKeyDetector.add(key, 1);
        if (addResult.isHotKey()) {
            localCache.put(key, redisValue);
        }

        return redisValue;
    }

    public void putValueToCache(String key, Object value) {
        AddResult addResult = hotKeyDetector.add(key, 1);
        if (addResult.isHotKey()) {
            localCache.put(key, value);
        }
        int expireTime = redisExpireTime + RandomUtil.randomInt(0, redisExpireTime);
        redisTemplate.opsForValue().set(RedisKeyUtil.buildRedisKey(key),
                value, expireTime, TimeUnit.SECONDS);
    }

    public void removeValueCache(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(RedisKeyUtil.buildRedisKey(key));
    }

    public Object getThumbCache(String hashKey, String key) {
        String compositeKey = hashKey + ":" + key;
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            hotKeyDetector.add(key, 1);
            return value;
        }
        Object redisValue = redisTemplate.opsForHash().get(RedisKeyUtil.buildRedisKey(hashKey), key);
        if (redisValue != null) {
            hotKeyDetector.add(key, 1);
            AddResult addResult = hotKeyDetector.add(key, 0);
            if (addResult.isHotKey()) {
                localCache.put(compositeKey, redisValue);
            }
        }
        return redisValue;
    }

    public void putThumbCountIfPresent(String key, long delta) {
        Object object = localCache.getIfPresent(key);
        if (object == null) {
            return;
        }
        Long oldValue = ((Number) object).longValue();
        localCache.put(key, oldValue + delta);
    }

    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        hotKeyDetector.fading();
    }
}
