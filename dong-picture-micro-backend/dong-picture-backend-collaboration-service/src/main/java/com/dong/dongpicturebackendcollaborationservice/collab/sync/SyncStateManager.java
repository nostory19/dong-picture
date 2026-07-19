package com.dong.dongpicturebackendcollaborationservice.collab.sync;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.CRDTDocument;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.OperationLog;
import com.dong.dongpicturebackendcollaborationservice.collab.engine.SyncProtocol;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 同步状态管理器 — 管理每张图片的 SyncProtocol 实例。
 *
 * 每个活跃的协作会话（图片）对应一个 SyncProtocol 实例，
 * 负责该图片的操作序列管理、状态向量追踪、三阶段同步。
 */
public class SyncStateManager {

    private final OperationLog operationLog;

    /** pictureId → SyncProtocol 实例 */
    private final ConcurrentHashMap<Long, SyncProtocol> syncProtocols = new ConcurrentHashMap<>();

    /** pictureId → CRDTDocument */
    private final ConcurrentHashMap<Long, CRDTDocument> documents = new ConcurrentHashMap<>();

    public SyncStateManager(OperationLog operationLog) {
        this.operationLog = operationLog;
    }

    /**
     * 获取或创建指定图片的 SyncProtocol
     */
    public SyncProtocol getOrCreateSyncProtocol(Long pictureId) {
        return syncProtocols.computeIfAbsent(pictureId, id -> new SyncProtocol(operationLog));
    }

    /**
     * 获取或创建指定图片的文档
     */
    public CRDTDocument getOrCreateDocument(Long pictureId) {
        return documents.computeIfAbsent(pictureId, CRDTDocument::new);
    }

    /**
     * 获取 SyncProtocol（可能为 null）
     */
    public SyncProtocol getSyncProtocol(Long pictureId) {
        return syncProtocols.get(pictureId);
    }

    /**
     * 获取文档（可能为 null）
     */
    public CRDTDocument getDocument(Long pictureId) {
        return documents.get(pictureId);
    }

    /**
     * 当图片不再有任何活跃会话时，清理内存中的状态
     */
    public void evictIfIdle(Long pictureId, CollaborationSession sessionManager) {
        if (sessionManager.getSessionCount(pictureId) == 0) {
            syncProtocols.remove(pictureId);
            documents.remove(pictureId);
        }
    }

    /**
     * 获取活跃的文档数量
     */
    public int getActiveDocumentCount() {
        return documents.size();
    }
}
