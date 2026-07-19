package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 三阶段同步协议实现。
 *
 * <pre>
 * 阶段1: Client → Server: 携带 stateVector，请求同步
 *        Server → Client: 返回缺失的 Operation 列表
 *
 * 阶段2: Client → Server: 确认收到 + 发送本地未同步的操作
 *        Server → Client: 确认同步完成
 *
 * 阶段3: 进入实时广播模式
 *        Server 将收到的操作广播给所有订阅客户端（排除发送者）
 * </pre>
 */
public class SyncProtocol {

    private final OperationLog operationLog;
    private final LamportClock serverClock;
    private final StateVector globalStateVector;

    public SyncProtocol(OperationLog operationLog) {
        this.operationLog = operationLog;
        this.serverClock = new LamportClock();
        this.globalStateVector = new StateVector();
    }

    /**
     * Step1: 客户端发起同步请求。
     *
     * @param pictureId        图片 ID
     * @param clientId         客户端 ID
     * @param clientStateVector 客户端本地的状态向量
     * @return 服务端响应：缺失的操作列表 + 服务端当前状态向量
     */
    public SyncStep1Result handleSyncStep1(Long pictureId, String clientId,
                                            Map<String, Long> clientStateVector) {
        // 更新客户端在 global state vector 中的记录
        long maxClientSeq = clientStateVector.values().stream()
                .mapToLong(Long::longValue).max().orElse(0);
        globalStateVector.update(clientId, maxClientSeq);

        // 计算客户端缺失的操作
        Map<String, Long> missing = globalStateVector.computeMissing(clientStateVector);
        List<Operation> missingOps = new ArrayList<>();
        if (!missing.isEmpty()) {
            missingOps = operationLog.queryMissing(pictureId, clientStateVector);
        }

        return SyncStep1Result.builder()
                .serverStateVector(globalStateVector.snapshot())
                .missingOperations(missingOps)
                .build();
    }

    /**
     * Step2: 客户端发送本地未同步的操作到服务端。
     *
     * @param clientOps 客户端本地未同步的操作列表
     * @return 确认结果，包含服务端分配的全局序号
     */
    public SyncStep2Result handleSyncStep2(List<Operation> clientOps) {
        List<Long> assignedSeqs = new ArrayList<>();
        for (Operation op : clientOps) {
            long seq = serverClock.tick();
            op.setSeq(seq);
            op.setTimestamp(System.currentTimeMillis());
            Operation stored = operationLog.append(op);
            assignedSeqs.add(stored.getSeq());
            globalStateVector.update(op.getClientId(), seq);
        }
        return SyncStep2Result.builder()
                .acknowledged(true)
                .serverStateVector(globalStateVector.snapshot())
                .assignedSeqs(assignedSeqs)
                .build();
    }

    /**
     * 处理实时操作（Live Sync 阶段）。
     * 服务端为操作分配序号、持久化、返回以便广播。
     */
    public Operation processLiveOperation(Operation clientOp) {
        long seq = serverClock.tick();
        clientOp.setSeq(seq);
        clientOp.setTimestamp(System.currentTimeMillis());
        Operation stored = operationLog.append(clientOp);
        globalStateVector.update(clientOp.getClientId(), seq);
        return stored;
    }

    /**
     * 获取全局状态向量快照
     */
    public Map<String, Long> getGlobalStateVector() {
        return globalStateVector.snapshot();
    }

    /**
     * 获取服务端时钟值
     */
    public long getServerClock() {
        return serverClock.current();
    }

    // === 协议消息载体 ===

    public static class SyncStep1Result {
        private Map<String, Long> serverStateVector;
        private List<Operation> missingOperations;

        public SyncStep1Result() {}
        public SyncStep1Result(Map<String, Long> serverStateVector, List<Operation> missingOperations) {
            this.serverStateVector = serverStateVector;
            this.missingOperations = missingOperations;
        }
        public Map<String, Long> getServerStateVector() { return serverStateVector; }
        public void setServerStateVector(Map<String, Long> v) { this.serverStateVector = v; }
        public List<Operation> getMissingOperations() { return missingOperations; }
        public void setMissingOperations(List<Operation> ops) { this.missingOperations = ops; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Map<String, Long> serverStateVector;
            private List<Operation> missingOperations;
            public Builder serverStateVector(Map<String, Long> v) { this.serverStateVector = v; return this; }
            public Builder missingOperations(List<Operation> ops) { this.missingOperations = ops; return this; }
            public SyncStep1Result build() { return new SyncStep1Result(serverStateVector, missingOperations); }
        }
    }

    public static class SyncStep2Result {
        private boolean acknowledged;
        private Map<String, Long> serverStateVector;
        private List<Long> assignedSeqs;

        public SyncStep2Result() {}
        public SyncStep2Result(boolean acknowledged, Map<String, Long> serverStateVector, List<Long> assignedSeqs) {
            this.acknowledged = acknowledged;
            this.serverStateVector = serverStateVector;
            this.assignedSeqs = assignedSeqs;
        }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean v) { this.acknowledged = v; }
        public Map<String, Long> getServerStateVector() { return serverStateVector; }
        public void setServerStateVector(Map<String, Long> v) { this.serverStateVector = v; }
        public List<Long> getAssignedSeqs() { return assignedSeqs; }
        public void setAssignedSeqs(List<Long> seqs) { this.assignedSeqs = seqs; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private boolean acknowledged;
            private Map<String, Long> serverStateVector;
            private List<Long> assignedSeqs;
            public Builder acknowledged(boolean v) { this.acknowledged = v; return this; }
            public Builder serverStateVector(Map<String, Long> v) { this.serverStateVector = v; return this; }
            public Builder assignedSeqs(List<Long> seqs) { this.assignedSeqs = seqs; return this; }
            public SyncStep2Result build() { return new SyncStep2Result(acknowledged, serverStateVector, assignedSeqs); }
        }
    }
}
