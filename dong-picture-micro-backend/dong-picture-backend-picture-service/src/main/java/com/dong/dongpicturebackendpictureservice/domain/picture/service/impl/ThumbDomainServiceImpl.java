package com.dong.dongpicturebackendpictureservice.domain.picture.service.impl;

import cn.hutool.core.date.DateUtil;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendmodel.constant.RedisLuaScriptConstant;
import com.dong.dongpicturebackendmodel.constant.ThumbConstant;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.LuaStatusEnum;
import com.dong.dongpicturebackendmodel.vo.PictureVO;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.ThumbDomainService;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.CacheManager;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.RankManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ThumbDomainServiceImpl implements ThumbDomainService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private RankManager rankManager;

    private static final DefaultRedisScript<Long> THUMB_REDIS_SCRIPT;
    private static final DefaultRedisScript<Long> UNTHUMB_REDIS_SCRIPT;
    private static final Long LONG_UN_THUMB = 0L;

    static {
        THUMB_REDIS_SCRIPT = new DefaultRedisScript<>();
        THUMB_REDIS_SCRIPT.setScriptText(RedisLuaScriptConstant.THUMB_SCRIPT);
        THUMB_REDIS_SCRIPT.setResultType(Long.class);

        UNTHUMB_REDIS_SCRIPT = new DefaultRedisScript<>();
        UNTHUMB_REDIS_SCRIPT.setScriptText(RedisLuaScriptConstant.UNTHUMB_SCRIPT);
        UNTHUMB_REDIS_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean doThumb(Long pictureId, User loginUser) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = loginUser.getId();
        String timeSlice = getTimeSlice();
        String userThumbKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
        String tempThumbKey = ThumbConstant.TEMP_THUMB_KEY_PREFIX + timeSlice;
        String pictureKey = ThumbConstant.THUMB_KEY_PICTURE_PREFIX + pictureId;

        Long result = stringRedisTemplate.execute(THUMB_REDIS_SCRIPT,
                Arrays.asList(userThumbKey, tempThumbKey, pictureKey),
                String.valueOf(userId), String.valueOf(pictureId));

        if (result != null && result == LuaStatusEnum.FAIL.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已点赞过该图片");
        }

        cacheManager.putThumbCountIfPresent(pictureKey, 1);
        rankManager.updateThumbRank(pictureId, getCurrentThumbCount(pictureKey));
        return true;
    }

    @Override
    public boolean undoThumb(Long pictureId, User loginUser) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = loginUser.getId();
        String timeSlice = getTimeSlice();
        String userThumbKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
        String tempThumbKey = ThumbConstant.TEMP_THUMB_KEY_PREFIX + timeSlice;
        String pictureKey = ThumbConstant.THUMB_KEY_PICTURE_PREFIX + pictureId;

        Long result = stringRedisTemplate.execute(UNTHUMB_REDIS_SCRIPT,
                Arrays.asList(userThumbKey, tempThumbKey, pictureKey),
                String.valueOf(userId), String.valueOf(pictureId));

        if (result != null && result == LuaStatusEnum.FAIL.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您还未点赞该图片");
        }

        cacheManager.putThumbCountIfPresent(pictureKey, -1);
        rankManager.updateThumbRank(pictureId, getCurrentThumbCount(pictureKey));
        return true;
    }

    private long getCurrentThumbCount(String pictureKey) {
        String val = stringRedisTemplate.opsForValue().get(pictureKey);
        return val != null ? Long.parseLong(val) : 0;
    }

    @Override
    public boolean hasThumb(Long userId, Long pictureId) {
        String userThumbKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
        Object value = cacheManager.getThumbCache(userThumbKey, String.valueOf(pictureId));
        if (value == null) {
            return false;
        }
        return !LONG_UN_THUMB.equals(value);
    }

    @Override
    public void getPictureThumbState(List<PictureVO> pictureVOList, User loginUser) {
        if (pictureVOList == null || pictureVOList.isEmpty() || loginUser == null) {
            if (pictureVOList != null) {
                pictureVOList.forEach(vo -> vo.setHasThumb(false));
            }
            return;
        }
        String hashKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
        for (PictureVO pictureVO : pictureVOList) {
            String pictureId = String.valueOf(pictureVO.getId());
            Object value = cacheManager.getThumbCache(hashKey, pictureId);
            pictureVO.setHasThumb(value != null && !LONG_UN_THUMB.equals(value));
        }
    }

    private String getTimeSlice() {
        long epochSecond = Instant.now().getEpochSecond();
        long slice = (epochSecond / 10) * 10;
        return DateUtil.format(
                java.util.Date.from(Instant.ofEpochSecond(slice).atZone(ZoneId.of("Asia/Shanghai")).toInstant()),
                "HH:mm:ss");
    }
}
