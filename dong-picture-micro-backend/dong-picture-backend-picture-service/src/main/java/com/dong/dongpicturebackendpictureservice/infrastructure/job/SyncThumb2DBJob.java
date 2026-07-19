package com.dong.dongpicturebackendpictureservice.infrastructure.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.dongpicturebackendmodel.constant.ThumbConstant;
import com.dong.dongpicturebackendmodel.entity.Thumb;
import com.dong.dongpicturebackendmodel.enums.ThumbTypeEnum;
import com.dong.dongpicturebackendpictureservice.infrastructure.mapper.PictureMapper;
import com.dong.dongpicturebackendpictureservice.infrastructure.mapper.ThumbMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class SyncThumb2DBJob {

    @Resource
    private ThumbMapper thumbMapper;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    @Scheduled(fixedRate = 10000)
    public void run() {
        long epochSecond = Instant.now().getEpochSecond();
        long slice = ((epochSecond / 10) - 1) * 10;
        String date = DateUtil.format(
                new Date(Instant.ofEpochSecond(slice).toEpochMilli()),
                "HH:mm:ss");
        syncThumb2DBByDate(date);
    }

    void syncThumb2DBByDate(String date) {
        String tempKey = ThumbConstant.TEMP_THUMB_KEY_PREFIX + date;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(tempKey);
        if (entries.isEmpty()) {
            return;
        }

        List<Thumb> thumbInsertList = new ArrayList<>();
        List<Long> thumbDeleteIdList = new ArrayList<>();
        Map<Long, Long> pictureThumbCountMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String key = (String) entry.getKey();
            String[] parts = key.split(":");
            Long userId = Long.valueOf(parts[0]);
            Long pictureId = Long.valueOf(parts[1]);
            int thumbType = Integer.parseInt((String) entry.getValue());

            if (thumbType == ThumbTypeEnum.INCR.getValue()) {
                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setPictureId(pictureId);
                thumbInsertList.add(thumb);
                pictureThumbCountMap.merge(pictureId, 1L, Long::sum);
            } else if (thumbType == ThumbTypeEnum.DECR.getValue()) {
                QueryWrapper<Thumb> wrapper = new QueryWrapper<>();
                wrapper.eq("userId", userId).eq("pictureId", pictureId);
                List<Thumb> thumbs = thumbMapper.selectList(wrapper);
                for (Thumb t : thumbs) {
                    thumbDeleteIdList.add(t.getId());
                }
                pictureThumbCountMap.merge(pictureId, -1L, Long::sum);
            }
        }

        for (Thumb thumb : thumbInsertList) {
            thumbMapper.insert(thumb);
        }
        if (!thumbDeleteIdList.isEmpty()) {
            thumbMapper.deleteBatchIds(thumbDeleteIdList);
        }
        if (!pictureThumbCountMap.isEmpty()) {
            pictureMapper.batchUpdateThumbCount(pictureThumbCountMap);
        }

        executor.execute(() -> {
            stringRedisTemplate.delete(tempKey);
        });
    }
}
