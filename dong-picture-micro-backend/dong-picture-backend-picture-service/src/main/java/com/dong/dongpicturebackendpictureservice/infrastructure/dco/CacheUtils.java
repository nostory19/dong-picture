package com.dong.dongpicturebackendpictureservice.infrastructure.dco;

import cn.hutool.json.JSONUtil;
import org.springframework.util.DigestUtils;

public class CacheUtils {

    public static final String PICTURE_CACHE = "pic";
    public static final String PICTURE_QUERY_CACHE = "pic:query";
    public static final String PICTURE_QUERY_DETAIL_CACHE = "pic:single";

    public static String getPictureCacheKey(String key) {
        return PICTURE_CACHE + ":" + key;
    }

    public static String getPictureQueryCacheKey(Object queryCondition) {
        String queryConditionString = JSONUtil.toJsonStr(queryCondition);
        String hashKey = DigestUtils.md5DigestAsHex(queryConditionString.getBytes());
        return PICTURE_QUERY_CACHE + ":" + hashKey;
    }

    public static String getSinglePictureQueryCacheKey(long id) {
        return PICTURE_QUERY_DETAIL_CACHE + ":" + id;
    }
}
