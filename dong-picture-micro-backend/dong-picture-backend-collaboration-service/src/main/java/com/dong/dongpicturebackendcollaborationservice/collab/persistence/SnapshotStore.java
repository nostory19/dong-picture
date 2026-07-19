package com.dong.dongpicturebackendcollaborationservice.collab.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 快照存储 — 管理 CRDT 文档快照的持久化和加载。
 *
 * 策略：
 * - 每 1000 次操作或每 5 分钟生成一次增量快照
 * - 快照内容 = (序列化的文档状态 + 状态向量)
 * - 当前实现为内存存储，生产环境可扩展到 PostgreSQL + COS
 */
public class SnapshotStore {

    /**
     * 内存中缓存的最近快照：pictureId → (snapshotData, stateVector)
     */
    private final ConcurrentHashMap<Long, SnapshotEntry> cache = new ConcurrentHashMap<>();

    /**
     * 保存快照
     *
     * @param pictureId       图片 ID
     * @param snapshotData    序列化的文档状态
     * @param stateVector     快照时的状态向量
     * @param operationSeq    快照覆盖的操作序号
     */
    public void save(Long pictureId, byte[] snapshotData, Map<String, Long> stateVector, long operationSeq) {
        cache.put(pictureId, new SnapshotEntry(snapshotData, stateVector, operationSeq));
    }

    /**
     * 加载最近一次快照
     *
     * @return null 表示无快照
     */
    public SnapshotEntry load(Long pictureId) {
        return cache.get(pictureId);
    }

    /**
     * 删除快照
     */
    public void delete(Long pictureId) {
        cache.remove(pictureId);
    }

    /**
     * 判断是否需要生成新快照
     *
     * @param pictureId      图片 ID
     * @param currentSeq     当前操作序号
     * @param snapshotInterval 快照间隔（操作数）
     * @return true 表示需要生成新快照
     */
    public boolean shouldSnapshot(Long pictureId, long currentSeq, long snapshotInterval) {
        SnapshotEntry last = cache.get(pictureId);
        return last == null || (currentSeq - last.operationSeq) >= snapshotInterval;
    }

    /**
     * 快照条目
     */
    public static class SnapshotEntry {
        /** 序列化的 CRDT 文档状态 */
        public final byte[] snapshotData;
        /** 快照时的状态向量 */
        public final Map<String, Long> stateVector;
        /** 快照覆盖的最大操作序号 */
        public final long operationSeq;

        public SnapshotEntry(byte[] snapshotData, Map<String, Long> stateVector, long operationSeq) {
            this.snapshotData = snapshotData;
            this.stateVector = stateVector;
            this.operationSeq = operationSeq;
        }
    }
}
