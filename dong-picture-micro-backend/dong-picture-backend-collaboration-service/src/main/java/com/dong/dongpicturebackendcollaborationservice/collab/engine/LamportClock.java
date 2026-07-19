package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lamport 逻辑时钟。
 * 每个客户端和服务端各自维护一个递增的逻辑时钟，
 * 操作消息携带 (clientId, lamportClock) 即可判断因果关系 (happens-before)，
 * 无需依赖 NTP 或物理时间戳。
 */
public class LamportClock {

    private final AtomicLong clock;

    public LamportClock() {
        this.clock = new AtomicLong(0);
    }

    public LamportClock(long initial) {
        this.clock = new AtomicLong(initial);
    }

    /**
     * 本地事件发生前：自增并返回新值
     */
    public long tick() {
        return clock.incrementAndGet();
    }

    /**
     * 收到远程事件后：取 max(local, remote) + 1
     */
    public long update(long receivedClock) {
        clock.updateAndGet(current -> Math.max(current, receivedClock) + 1);
        return clock.get();
    }

    /**
     * 查看当前时钟值（不自增）
     */
    public long current() {
        return clock.get();
    }

    /**
     * 比较两个时钟：返回 true 表示 a 严格发生在 b 之前 (a → b)
     */
    public static boolean happensBefore(long clockA, String clientIdA, long clockB, String clientIdB) {
        if (clockA < clockB) return true;
        if (clockA == clockB) {
            return clientIdA.compareTo(clientIdB) < 0;
        }
        return false;
    }
}
