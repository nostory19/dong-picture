package com.dong.dongpicturebackendcollaborationservice.collab.persistence;

import cn.hutool.json.JSONUtil;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.Operation;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Redis List 的 OperationLog 实现。
 *
 * 每条操作日志序列化为 JSON 存入 Redis List：
 *   Key:  collab:ops:{pictureId}
 *   值:   JSON 序列化的 Operation
 *
 * List 最大长度 100,000，超出后从左侧截断（配合快照机制）。
 */
@Slf4j
public class RedisOperationJournal implements OperationLog {

    private static final String KEY_PREFIX = "collab:ops:";
    private static final long MAX_LIST_LENGTH = 100_000;

    private final StringRedisTemplate redisTemplate;

    public RedisOperationJournal(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Operation append(Operation op) {
        String key = listKey(op.getPictureId());
        String json = JSONUtil.toJsonStr(op);
        redisTemplate.opsForList().rightPush(key, json);
        // 超出最大长度时从左侧截断
        Long len = redisTemplate.opsForList().size(key);
        if (len != null && len > MAX_LIST_LENGTH) {
            redisTemplate.opsForList().trim(key, len - MAX_LIST_LENGTH, -1);
        }
        return op;
    }

    @Override
    public List<Operation> queryRange(Long pictureId, long fromSeq, long toSeq) {
        String key = listKey(pictureId);
        List<String> all = redisTemplate.opsForList().range(key, 0, -1);
        if (all == null) return Collections.emptyList();
        return all.stream()
                .map(json -> JSONUtil.toBean(json, Operation.class))
                .filter(op -> op.getSeq() >= fromSeq && (toSeq <= 0 || op.getSeq() <= toSeq))
                .sorted(Comparator.comparingLong(Operation::getSeq))
                .collect(Collectors.toList());
    }

    @Override
    public List<Operation> querySince(Long pictureId, String clientId, long sinceSeq) {
        return queryRange(pictureId, sinceSeq + 1, -1);
    }

    @Override
    public long getMaxSeq(Long pictureId) {
        String key = listKey(pictureId);
        String last = redisTemplate.opsForList().index(key, -1);
        if (last != null) {
            Operation op = JSONUtil.toBean(last, Operation.class);
            return op != null ? op.getSeq() : 0;
        }
        return 0;
    }

    @Override
    public List<Operation> queryMissing(Long pictureId, Map<String, Long> clientStateVector) {
        String key = listKey(pictureId);
        List<String> all = redisTemplate.opsForList().range(key, 0, -1);
        if (all == null) return Collections.emptyList();
        return all.stream()
                .map(json -> JSONUtil.toBean(json, Operation.class))
                .filter(op -> {
                    long clientSeq = clientStateVector.getOrDefault(op.getClientId(), 0L);
                    return op.getSeq() > clientSeq;
                })
                .sorted(Comparator.comparingLong(Operation::getSeq))
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long pictureId) {
        Long len = redisTemplate.opsForList().size(listKey(pictureId));
        return len != null ? len : 0;
    }

    /**
     * 截断指定序号之前的操作（快照生成后调用）
     */
    public void truncateBefore(Long pictureId, long beforeSeq) {
        String key = listKey(pictureId);
        List<Operation> ops = queryRange(pictureId, 1, beforeSeq);
        if (!ops.isEmpty()) {
            redisTemplate.opsForList().trim(key, ops.size(), -1);
        }
    }

    /**
     * 删除整个日志
     */
    public void delete(Long pictureId) {
        redisTemplate.delete(listKey(pictureId));
    }

    private String listKey(Long pictureId) {
        return KEY_PREFIX + pictureId;
    }
}
