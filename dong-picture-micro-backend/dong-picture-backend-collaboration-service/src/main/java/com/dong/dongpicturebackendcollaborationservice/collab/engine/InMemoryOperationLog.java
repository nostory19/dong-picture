package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 基于内存的 OperationLog 实现，用于开发/测试环境。
 * 生产环境替换为 Redis Streams + PostgreSQL 实现。
 */
public class InMemoryOperationLog implements OperationLog {

    /** pictureId → 有序操作列表 */
    private final ConcurrentHashMap<Long, List<Operation>> store = new ConcurrentHashMap<>();

    /** pictureId → 当前最大序号 */
    private final ConcurrentHashMap<Long, AtomicLong> maxSeqMap = new ConcurrentHashMap<>();

    @Override
    public Operation append(Operation op) {
        store.computeIfAbsent(op.getPictureId(), k -> new CopyOnWriteArrayList<>())
                .add(op);
        maxSeqMap.computeIfAbsent(op.getPictureId(), k -> new AtomicLong(0))
                .set(op.getSeq());
        return op;
    }

    @Override
    public List<Operation> queryRange(Long pictureId, long fromSeq, long toSeq) {
        List<Operation> ops = store.get(pictureId);
        if (ops == null) return Collections.emptyList();
        long maxSeq = toSeq <= 0 ? Long.MAX_VALUE : toSeq;
        return ops.stream()
                .filter(op -> op.getSeq() >= fromSeq && op.getSeq() <= maxSeq)
                .sorted(Comparator.comparingLong(Operation::getSeq))
                .collect(Collectors.toList());
    }

    @Override
    public List<Operation> querySince(Long pictureId, String clientId, long sinceSeq) {
        List<Operation> ops = store.get(pictureId);
        if (ops == null) return Collections.emptyList();
        return ops.stream()
                .filter(op -> op.getSeq() > sinceSeq)
                .sorted(Comparator.comparingLong(Operation::getSeq))
                .collect(Collectors.toList());
    }

    @Override
    public long getMaxSeq(Long pictureId) {
        AtomicLong max = maxSeqMap.get(pictureId);
        return max != null ? max.get() : 0;
    }

    @Override
    public List<Operation> queryMissing(Long pictureId, Map<String, Long> clientStateVector) {
        List<Operation> ops = store.get(pictureId);
        if (ops == null) return Collections.emptyList();

        // 对于每个 clientId，找到客户端缺失的操作范围
        // clientStateVector[clientId] = 客户端已收到的该 client 最后一个 seq
        // 缺失：seq 大于 clientStateVector 中对应 clientId 值的操作
        return ops.stream()
                .filter(op -> {
                    long clientSeq = clientStateVector.getOrDefault(op.getClientId(), 0L);
                    return op.getSeq() > clientSeq;
                })
                .sorted(Comparator.comparingLong(Operation::getSeq))
                .collect(Collectors.toList());
    }

    @Override
    public long count(Long pictureId) {
        List<Operation> ops = store.get(pictureId);
        return ops != null ? ops.size() : 0;
    }

    /** 清空所有数据（仅用于测试） */
    public void clear() {
        store.clear();
        maxSeqMap.clear();
    }
}
