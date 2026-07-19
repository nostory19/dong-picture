package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态向量。
 * 记录每个 clientId 已收到的最后一个操作序号。
 * 通过比较两个状态向量，可以精确知道一方缺少哪些操作，
 * 实现增量同步而非全量拉取。
 *
 * <pre>
 * {
 *   "clientA": 5,
 *   "clientB": 3,
 *   "server": 12
 * }
 * </pre>
 */
public class StateVector {

    private final ConcurrentHashMap<String, Long> vector;

    public StateVector() {
        this.vector = new ConcurrentHashMap<>();
    }

    public StateVector(Map<String, Long> initial) {
        this.vector = new ConcurrentHashMap<>(initial);
    }

    /**
     * 更新某个客户端的序号（仅在严格递增时才更新）。
     * seq=0 表示无操作，不移入向量。
     */
    public void update(String clientId, long seq) {
        if (seq <= 0) return;
        vector.merge(clientId, seq, Math::max);
    }

    /**
     * 获取某个客户端的序号
     */
    public long get(String clientId) {
        return vector.getOrDefault(clientId, 0L);
    }

    /**
     * 删除某个客户端（客户端离线后可选清理）
     */
    public void remove(String clientId) {
        vector.remove(clientId);
    }

    /**
     * 计算当前向量的缺失操作。
     * 给定另一个状态向量（通常是客户端发来的），返回该客户端缺少的操作列表。
     *
     * @param clientVector 客户端的当前状态向量
     * @return 缺失的 (clientId, fromSeq) 列表，fromSeq 是该 client 的第一个缺失序号
     */
    public Map<String, Long> computeMissing(Map<String, Long> clientVector) {
        Map<String, Long> missing = new HashMap<>();
        for (Map.Entry<String, Long> entry : vector.entrySet()) {
            String clientId = entry.getKey();
            long serverSeq = entry.getValue();
            long clientSeq = clientVector.getOrDefault(clientId, 0L);
            if (clientSeq < serverSeq) {
                missing.put(clientId, clientSeq + 1);
            }
        }
        return missing;
    }

    /**
     * 判断 remote 向量是否已经完全被当前向量覆盖（即 remote 没有任何当前没有的操作）
     */
    public boolean covers(Map<String, Long> remote) {
        for (Map.Entry<String, Long> entry : remote.entrySet()) {
            long localSeq = vector.getOrDefault(entry.getKey(), 0L);
            if (localSeq < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取所有已知的客户端 ID
     */
    public Set<String> clientIds() {
        return Collections.unmodifiableSet(vector.keySet());
    }

    /**
     * 返回不可变快照
     */
    public Map<String, Long> snapshot() {
        return new HashMap<>(vector);
    }

    /**
     * 合并另一个状态向量（取每个 client 的最大值）
     */
    public void merge(StateVector other) {
        for (Map.Entry<String, Long> entry : other.vector.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    public boolean isEmpty() {
        return vector.isEmpty();
    }

    public int size() {
        return vector.size();
    }

    @Override
    public String toString() {
        return vector.toString();
    }
}
