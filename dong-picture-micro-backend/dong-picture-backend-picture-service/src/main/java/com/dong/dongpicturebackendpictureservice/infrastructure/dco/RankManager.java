package com.dong.dongpicturebackendpictureservice.infrastructure.dco;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RankManager {

    private static final String THUMB_RANK_KEY = "dongpicture:rank:thumb";
    private static final String TIME_RANK_KEY = "dongpicture:rank:time";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void updateThumbRank(Long pictureId, long thumbCount) {
        redisTemplate.opsForZSet().add(THUMB_RANK_KEY, pictureId.toString(), thumbCount);
    }

    public void addToTimeRank(Long pictureId, long createTimeMs) {
        redisTemplate.opsForZSet().add(TIME_RANK_KEY, pictureId.toString(), createTimeMs);
    }

    public void removeFromRank(Long pictureId) {
        redisTemplate.opsForZSet().remove(THUMB_RANK_KEY, pictureId.toString());
        redisTemplate.opsForZSet().remove(TIME_RANK_KEY, pictureId.toString());
    }

    public List<Long> getHotRankedIds(long page, long size) {
        return getRankedIds(THUMB_RANK_KEY, page, size, true);
    }

    public List<Long> getLatestRankedIds(long page, long size) {
        return getRankedIds(TIME_RANK_KEY, page, size, true);
    }

    private List<Long> getRankedIds(String key, long page, long size, boolean desc) {
        long start = (page - 1) * size;
        long end = page * size - 1;
        Set<Object> members;
        if (desc) {
            members = redisTemplate.opsForZSet().reverseRange(key, start, end);
        } else {
            members = redisTemplate.opsForZSet().range(key, start, end);
        }
        List<Long> ids = new ArrayList<>();
        if (members != null) {
            for (Object member : members) {
                ids.add(Long.valueOf(member.toString()));
            }
        }
        return ids;
    }

    public long getThumbRankCount() {
        Long count = redisTemplate.opsForZSet().zCard(THUMB_RANK_KEY);
        return count != null ? count : 0;
    }

    public long getTimeRankCount() {
        Long count = redisTemplate.opsForZSet().zCard(TIME_RANK_KEY);
        return count != null ? count : 0;
    }

    public void setExpire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }
}
