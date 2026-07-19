package com.dong.dongpicturebackendcollaborationservice.collab.engine;

/**
 * 一次协作操作。
 */
public class Operation {

    /** 全局唯一序列号，由服务端 LamportClock 分配 */
    private long seq;

    /** 发起操作的客户端 ID */
    private String clientId;

    /** 客户端侧的 Lamport 时钟值 */
    private long lamportClock;

    /** 操作所属的图片 ID */
    private Long pictureId;

    /** 修改的属性字段名 */
    private String field;

    /** 操作前的值 (JSON) */
    private String oldValue;

    /** 操作后的值 (JSON) */
    private String newValue;

    /** 操作时间戳 (Unix ms) */
    private long timestamp;

    /** 客户端签名 (HMAC)，服务端可验证 */
    private String signature;

    public Operation() {}

    public Operation(long seq, String clientId, long lamportClock, Long pictureId,
                     String field, String oldValue, String newValue, long timestamp, String signature) {
        this.seq = seq;
        this.clientId = clientId;
        this.lamportClock = lamportClock;
        this.pictureId = pictureId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public long getLamportClock() { return lamportClock; }
    public void setLamportClock(long lamportClock) { this.lamportClock = lamportClock; }
    public Long getPictureId() { return pictureId; }
    public void setPictureId(Long pictureId) { this.pictureId = pictureId; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long seq;
        private String clientId;
        private long lamportClock;
        private Long pictureId;
        private String field;
        private String oldValue;
        private String newValue;
        private long timestamp;
        private String signature;

        public Builder seq(long seq) { this.seq = seq; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder lamportClock(long lamportClock) { this.lamportClock = lamportClock; return this; }
        public Builder pictureId(Long pictureId) { this.pictureId = pictureId; return this; }
        public Builder field(String field) { this.field = field; return this; }
        public Builder oldValue(String oldValue) { this.oldValue = oldValue; return this; }
        public Builder newValue(String newValue) { this.newValue = newValue; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder signature(String signature) { this.signature = signature; return this; }

        public Operation build() {
            return new Operation(seq, clientId, lamportClock, pictureId,
                    field, oldValue, newValue, timestamp, signature);
        }
    }
}
