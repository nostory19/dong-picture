package com.dong.dongpicturebackendpictureservice.infrastructure.job;

import com.dong.dongpicturebackendmodel.constant.ThumbConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class SyncThumb2DBCompensatoryJob {

    @Resource
    private SyncThumb2DBJob syncThumb2DBJob;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        Set<String> keys = stringRedisTemplate.keys(ThumbConstant.TEMP_THUMB_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String date = key.replace(ThumbConstant.TEMP_THUMB_KEY_PREFIX, "");
            syncThumb2DBJob.syncThumb2DBByDate(date);
        }
    }
}
