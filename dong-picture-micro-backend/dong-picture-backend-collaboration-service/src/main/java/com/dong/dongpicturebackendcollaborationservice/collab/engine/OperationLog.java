package com.dong.dongpicturebackendcollaborationservice.collab.engine;

import java.util.List;
import java.util.Map;

/**
 * 操作日志接口。
 * 定义操作日志的持久化契约：追加、范围查询、按客户端+序号查询。
 * 具体实现：Redis Streams (热数据) + PostgreSQL (冷归档)。
 */
public interface OperationLog {

    /**
     * 追加一条操作，返回分配的全局序列号
     */
    Operation append(Operation op);

    /**
     * 按序号范围查询操作
     *
     * @param pictureId 图片 ID
     * @param fromSeq   起始序号（含）
     * @param toSeq     结束序号（含），为 -1 表示到最新
     */
    List<Operation> queryRange(Long pictureId, long fromSeq, long toSeq);

    /**
     * 查询某个客户端在某个序号之后的所有操作
     */
    List<Operation> querySince(Long pictureId, String clientId, long sinceSeq);

    /**
     * 获取某张图片的当前最大序号
     */
    long getMaxSeq(Long pictureId);

    /**
     * 查询多个客户端从各自的 fromSeq 之后的所有操作
     * 用于 SyncStep1 — 一次性拉取所有缺失的操作
     *
     * @param pictureId        图片 ID
     * @param clientStateVector clientId → fromSeq (不含)，即返回 seq > fromSeq 的操作
     * @return 所有缺失的操作列表
     */
    List<Operation> queryMissing(Long pictureId, Map<String, Long> clientStateVector);

    /**
     * 获取某张图片的操作总数
     */
    long count(Long pictureId);
}
