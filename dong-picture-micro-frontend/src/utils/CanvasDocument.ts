/**
 * CanvasDocument — Yjs 协作文档封装。
 *
 * 绑定 Y.Map 到图片的可编辑属性，每次本地修改自动同步到其他客户端。
 * 提供 undo/redo、presence、离线队列管理。
 *
 * 使用方式：
 *   const doc = new CanvasDocument(pictureId)
 *   await doc.initialize()
 *   doc.setProperty('rotate', 90)       // → 自动同步
 *   doc.observe('rotate', (val) => ...) // 监听远程变更
 *   doc.undo()                          // Ctrl+Z
 */

import * as Y from 'yjs'
import { CollabWebSocket, CollabMessage } from './CollabWebSocket'
import { OfflineStore } from './OfflineStore'

/** 可编辑的图片属性 */
export interface CanvasProperties {
  rotate: number
  scale: number
  cropX: number
  cropY: number
  cropW: number
  cropH: number
  brightness: number
  contrast: number
  saturation: number
  filter: string
}

/** 在线用户的 Presence 信息 */
export interface UserPresence {
  clientId: string
  userId?: number
  userName?: string
  userAvatar?: string
  editingField?: string
  cursorX: number
  cursorY: number
  connected: boolean
}

type PropertyObserver = (value: unknown, source: 'local' | 'remote') => void
type PresenceObserver = (presences: Map<string, UserPresence>) => void

export class CanvasDocument {
  readonly pictureId: number
  private clientId: string

  /** Yjs 根文档 */
  private ydoc: Y.Doc

  /** 图片属性 Map（每个属性是一个 CRDT register） */
  private props: Y.Map<unknown>

  /** 在线用户 Presence */
  private presence: Y.Map<unknown>

  /** WebSocket 连接 */
  private ws: CollabWebSocket | null = null

  /** 属性观察者 */
  private propObservers: Map<string, PropertyObserver[]> = new Map()

  /** Presence 观察者 */
  private presenceObservers: PresenceObserver[] = []

  /** 离线存储 */
  private offlineStore: OfflineStore

  /** Undo 管理器 */
  private undoManager: Y.UndoManager | null = null

  /** 是否已初始化 */
  private initialized = false

  constructor(pictureId: number, clientId?: string) {
    this.pictureId = pictureId
    this.clientId = clientId || this.loadOrCreateClientId()
    this.ydoc = new Y.Doc()
    this.props = this.ydoc.getMap('properties')
    this.presence = this.ydoc.getMap('presence')
    this.offlineStore = new OfflineStore()
  }

  /** 初始化协作连接 */
  async initialize(): Promise<void> {
    if (this.initialized) return

    // 连接 WebSocket
    this.ws = new CollabWebSocket(this.pictureId, this.clientId)

    // 监听实时操作
    this.ws.on('OPERATION', (msg) => {
      if (msg.field && msg.newValue !== undefined) {
        this.applyRemoteOperation(msg.field, msg.newValue)
      }
    })

    // 监听 Presence 更新
    this.ws.on('PRESENCE', (msg) => {
      this.updateRemotePresence(msg)
    })

    // 监听用户进出
    this.ws.on('CLIENT_JOIN', (msg) => {
      if (msg.clientId) {
        this.setPresence(msg.clientId, { connected: true } as UserPresence)
        this.notifyPresenceObservers()
      }
    })

    this.ws.on('CLIENT_LEAVE', (msg) => {
      if (msg.clientId) {
        this.setPresence(msg.clientId, { connected: false } as UserPresence)
        this.notifyPresenceObservers()
      }
    })

    this.ws.connect()

    // 初始化 UndoManager（仅追踪本地操作）
    this.undoManager = new Y.UndoManager(this.props, {
      trackedOrigins: new Set([this.clientId]),
      captureTimeout: 200,
    })

    this.initialized = true
  }

  /** 设置属性并自动同步 */
  setProperty<K extends keyof CanvasProperties>(field: K, value: CanvasProperties[K]): void {
    // 本地立即应用
    this.props.set(field as string, value)

    // 如果已连接，发送到服务端
    if (this.ws?.connected) {
      this.ws.sendMessage({
        type: 'OPERATION',
        field: field as string,
        newValue: String(value),
        lamportClock: Date.now(), // 简化版：使用时间戳作为 lamport clock
      })
    } else {
      // 离线：缓存到 IndexedDB
      this.offlineStore.enqueue(this.pictureId, {
        type: 'OPERATION',
        field: field as string,
        newValue: String(value),
      })
    }
  }

  /** 获取属性值 */
  getProperty(field: string): unknown {
    return this.props.get(field)
  }

  /** 监听属性变更 */
  observe(field: string, callback: PropertyObserver): void {
    if (!this.propObservers.has(field)) {
      this.propObservers.set(field, [])
    }
    this.propObservers.get(field)!.push(callback)
  }

  /** 取消监听 */
  unobserve(field: string, callback: PropertyObserver): void {
    const observers = this.propObservers.get(field)
    if (observers) {
      const idx = observers.indexOf(callback)
      if (idx >= 0) observers.splice(idx, 1)
    }
  }

  /** 监听 Presence 变更 */
  observePresence(callback: PresenceObserver): void {
    this.presenceObservers.push(callback)
  }

  /** 获取所有在线用户的 Presence */
  getPresenceMap(): Map<string, UserPresence> {
    const result = new Map<string, UserPresence>()
    this.presence.forEach((value, key) => {
      result.set(key, value as UserPresence)
    })
    return result
  }

  /** 更新本地用户的 Presence */
  updateMyPresence(presence: Partial<UserPresence>): void {
    const current = (this.presence.get(this.clientId) || {}) as UserPresence
    const updated = { ...current, ...presence, clientId: this.clientId }
    this.presence.set(this.clientId, updated as unknown as Parameters<Y.Map<unknown>['set']>[1])

    if (this.ws?.connected) {
      this.ws.sendMessage({
        type: 'PRESENCE',
        editingField: updated.editingField,
        cursorX: updated.cursorX,
        cursorY: updated.cursorY,
      })
    }
  }

  /** Undo */
  undo(): void {
    this.undoManager?.undo()
  }

  /** Redo */
  redo(): void {
    this.undoManager?.redo()
  }

  /** 断开连接并销毁 */
  destroy(): void {
    this.ws?.disconnect()
    this.ydoc.destroy()
    this.initialized = false
  }

  // ======== 内部方法 ========

  private applyRemoteOperation(field: string, value: string): void {
    // 转换值类型
    let parsed: unknown = value
    if (!isNaN(Number(value))) parsed = Number(value)
    else if (value === 'true') parsed = true
    else if (value === 'false') parsed = false

    // 使用不同的 origin 避免 undoManager 追踪远程操作
    this.ydoc.transact(() => {
      this.props.set(field, parsed)
    }, 'remote')

    // 通知观察者
    const observers = this.propObservers.get(field)
    if (observers) {
      observers.forEach(cb => {
        try { cb(parsed, 'remote') } catch (e) { console.error('[CanvasDoc] observer error:', e) }
      })
    }
  }

  private updateRemotePresence(msg: CollabMessage): void {
    if (!msg.clientId) return
    const presence: UserPresence = {
      clientId: msg.clientId,
      editingField: msg.editingField,
      cursorX: msg.cursorX || 0,
      cursorY: msg.cursorY || 0,
      connected: true,
    }
    this.setPresence(msg.clientId, presence)
    this.notifyPresenceObservers()
  }

  private setPresence(clientId: string, presence: Partial<UserPresence>): void {
    const current = (this.presence.get(clientId) || {}) as UserPresence
    this.presence.set(clientId, { ...current, ...presence } as unknown as Parameters<Y.Map<unknown>['set']>[1])
  }

  private notifyPresenceObservers(): void {
    const map = this.getPresenceMap()
    this.presenceObservers.forEach(cb => {
      try { cb(map) } catch (e) { console.error('[CanvasDoc] presence observer error:', e) }
    })
  }

  private loadOrCreateClientId(): string {
    const key = 'collabClientId'
    let id = localStorage.getItem(key)
    if (!id) {
      id = crypto.randomUUID()
      localStorage.setItem(key, id)
    }
    return id
  }
}
