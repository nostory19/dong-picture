package com.dong.dongpicturebackendpictureservice.infrastructure.dco;

public class RedisKeyUtil {

    private static final String APP_NAME = "dongpicture";

    public static String buildRedisKey(String key) {
        return APP_NAME + ":" + key;
    }
}
