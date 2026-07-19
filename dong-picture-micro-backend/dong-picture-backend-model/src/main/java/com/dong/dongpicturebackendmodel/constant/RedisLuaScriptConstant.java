package com.dong.dongpicturebackendmodel.constant;

public class RedisLuaScriptConstant {
    public static final String THUMB_SCRIPT =
            "local userThumbKey = KEYS[1] " +
            "local tempThumbKey = KEYS[2] " +
            "local pictureKey = KEYS[3] " +
            "local userId = ARGV[1] " +
            "local pictureId = ARGV[2] " +
            "if redis.call('HEXISTS', userThumbKey, pictureId) == 1 then return -1 end " +
            "local oldNumber = tonumber(redis.call('HGET', tempThumbKey, userId .. ':' .. pictureId)) or 0 " +
            "local oldThumbCount = tonumber(redis.call('GET', pictureKey)) or 0 " +
            "local newNumber = oldNumber + 1 " +
            "local newThumbCount = oldThumbCount + 1 " +
            "redis.call('HSET', tempThumbKey, userId .. ':' .. pictureId, newNumber) " +
            "redis.call('SET', pictureKey, newThumbCount) " +
            "redis.call('HSET', userThumbKey, pictureId, 1) " +
            "return 1";

    public static final String UNTHUMB_SCRIPT =
            "local userThumbKey = KEYS[1] " +
            "local tempThumbKey = KEYS[2] " +
            "local pictureKey = KEYS[3] " +
            "local userId = ARGV[1] " +
            "local pictureId = ARGV[2] " +
            "if redis.call('HEXISTS', userThumbKey, pictureId) ~= 1 then return -1 end " +
            "local oldNumber = tonumber(redis.call('HGET', tempThumbKey, userId .. ':' .. pictureId)) or 0 " +
            "local oldThumbCount = tonumber(redis.call('GET', pictureKey)) or 0 " +
            "local newNumber = oldNumber - 1 " +
            "local newThumbCount = oldThumbCount - 1 " +
            "redis.call('HSET', tempThumbKey, userId .. ':' .. pictureId, newNumber) " +
            "redis.call('SET', pictureKey, newThumbCount) " +
            "redis.call('HDEL', userThumbKey, pictureId) " +
            "return 1";
}
