/**
 * 协作 WebSocket 客户端 — 实现三阶段同步协议 + 心跳 + 断线重连。
 *
 * 协议阶段：
 *   1. SYNC_STEP1: 客户端携带 stateVector → 服务端返回缺失操作
 *   2. SYNC_STEP2: 客户端提交本地未同步操作 → 服务端 ACK + 广播
 *   3. Live: 实时 OPERATION / PRESENCE / CURSOR 消息收发
 *
 * 使用方式（替换旧的 PictureEditWebSocket）：
 *   const ws = new CollabWebSocket(pictureId, clientId)
 *   ws.on('SYNC_STEP1', handler)
 *   ws.on('OPERATION', handler)
 *   ws.connect()
 */

export interface CollabMessage {
  type: string
  clientId?: string
  pictureId?: number
  stateVector?: Record<string, number>
  serverStateVector?: Record<string, number>
  missingOperations?: Array<Record<string, unknown>>
  pendingOps?: Array<Record<string, unknown>>
  assignedSeqs?: number[]
  field?: string
  value?: string
  newValue?: string
  oldValue?: string
  lamportClock?: number
  editingField?: string
  cursorX?: number
  cursorY?: number
  presenceList?: Array<Record<string, unknown>>
  onlineClients?: Array<Record<string, unknown>>
  message?: string
  acknowledged?: boolean
}

type MessageHandler = (msg: CollabMessage) => void

export class CollabWebSocket {
  private pictureId: number
  private clientId: string
  private socket: WebSocket | null = null
  private handlers: Map<string, MessageHandler[]> = new Map()

  /** 客户端状态向量：{ clientId → lastReceivedSeq } */
  private stateVector: Map<string, number> = new Map()

  /** 未同步到服务端的本地操作队列 */
  private pendingOps: CollabMessage[] = []

  /** 是否已通过三阶段同步完成首次连接 */
  private synced = false

  /** 心跳定时器 */
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null

  /** 重连定时器 */
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null

  /** 最大重连次数 */
  private maxReconnectAttempts = 10
  private reconnectAttempts = 0
  private reconnectDelay = 1000

  /** 连接状态 */
  private _connected = false
  get connected() { return this._connected }
  get isSynced() { return this.synced }

  constructor(pictureId: number, clientId: string) {
    this.pictureId = pictureId
    this.clientId = clientId
  }

  /** 建立 WebSocket 连接 */
  connect(): void {
    const baseUrl = 'ws://localhost:8203'
    const token = localStorage.getItem('authToken')
    const tokenParam = token ? `&token=${token}` : ''
    const url = `${baseUrl}/ws/picture/edit?pictureId=${this.pictureId}&clientId=${this.clientId}${tokenParam}`

    this.socket = new WebSocket(url)
    this.socket.binaryType = 'blob'

    this.socket.onopen = () => {
      console.log('[CollabWS] connected, starting sync...')
      this._connected = true
      this.reconnectAttempts = 0
      this.reconnectDelay = 1000
      this.trigger('open', { message: 'connected' })

      // 连接建立后立即发起 SYNC_STEP1
      this.startSync()

      // 启动心跳
      this.startHeartbeat()
    }

    this.socket.onmessage = (event) => {
      try {
        const msg: CollabMessage = JSON.parse(event.data)
        this.handleMessage(msg)
      } catch (e) {
        console.error('[CollabWS] parse error:', e)
      }
    }

    this.socket.onclose = (event) => {
      console.log('[CollabWS] closed:', event.code, event.reason)
      this._connected = false
      this.synced = false
      this.stopHeartbeat()
      this.trigger('close', { code: event.code } as CollabMessage)

      // 异常关闭时自动重连
      if (event.code !== 1000) {
        this.scheduleReconnect()
      }
    }

    this.socket.onerror = (error) => {
      console.error('[CollabWS] error:', error)
      this.trigger('error', { message: 'WebSocket error' })
    }
  }

  /** 断开连接 */
  disconnect(): void {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts // 禁止重连
    if (this.socket) {
      this.socket.close(1000, 'user disconnect')
      this.socket = null
    }
    this._connected = false
    this.synced = false
  }

  /** 发送消息 */
  sendMessage(msg: Partial<CollabMessage>): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      // 离线时入队
      this.pendingOps.push(msg as CollabMessage)
      return
    }
    const payload: CollabMessage = {
      ...msg,
      clientId: msg.clientId || this.clientId,
      pictureId: this.pictureId,
    }
    this.socket.send(JSON.stringify(payload))
  }

  /** 注册事件处理器 */
  on(type: string, handler: MessageHandler): void {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, [])
    }
    this.handlers.get(type)!.push(handler)
  }

  /** 移除事件处理器 */
  off(type: string, handler: MessageHandler): void {
    const handlers = this.handlers.get(type)
    if (handlers) {
      const idx = handlers.indexOf(handler)
      if (idx >= 0) handlers.splice(idx, 1)
    }
  }

  /** 获取当前状态向量快照 */
  getStateVector(): Record<string, number> {
    const sv: Record<string, number> = {}
    this.stateVector.forEach((v, k) => { sv[k] = v })
    return sv
  }

  // ======== 内部方法 ========

  private handleMessage(msg: CollabMessage): void {
    const type = msg.type
    if (!type) return

    switch (type) {
      case 'SYNC_STEP1':
        this.handleSyncStep1Response(msg)
        break
      case 'SYNC_STEP2':
        this.handleSyncStep2Response(msg)
        break
      case 'OPERATION':
        this.updateStateVector(msg)
        break
    }

    // 触发注册的处理器
    this.trigger(type, msg)
  }

  /** 发起三阶段同步 */
  private startSync(): void {
    // Step1: 发送客户端状态向量
    this.sendMessage({
      type: 'SYNC_STEP1',
      stateVector: this.getStateVector(),
    })
  }

  /** 处理 SYNC_STEP1 响应 */
  private handleSyncStep1Response(msg: CollabMessage): void {
    // 更新服务端状态向量
    if (msg.serverStateVector) {
      for (const [k, v] of Object.entries(msg.serverStateVector)) {
        const current = this.stateVector.get(k) || 0
        if (v > current) this.stateVector.set(k, v as number)
      }
    }

    // Step2: 提交本地未同步操作
    if (this.pendingOps.length > 0) {
      this.sendMessage({
        type: 'SYNC_STEP2',
        pendingOps: this.pendingOps,
      })
    } else {
      // 没有待同步操作，直接标记为已同步
      this.completeSync()
    }

    console.log(`[CollabWS] sync step1: received ${msg.missingOperations?.length || 0} missing ops`)
  }

  /** 处理 SYNC_STEP2 响应 */
  private handleSyncStep2Response(msg: CollabMessage): void {
    if (msg.acknowledged) {
      // 清除已确认的本地操作
      const ackCount = msg.assignedSeqs?.length || 0
      this.pendingOps.splice(0, ackCount)
      this.completeSync()
      console.log(`[CollabWS] sync step2: ${ackCount} ops acknowledged`)
    }
  }

  /** 标记同步完成 */
  private completeSync(): void {
    if (!this.synced) {
      this.synced = true
      this.trigger('synced', { message: 'sync complete' })
    }
  }

  /** 更新状态向量 */
  private updateStateVector(msg: CollabMessage): void {
    if (msg.clientId && msg.lamportClock) {
      const current = this.stateVector.get(msg.clientId) || 0
      if (msg.lamportClock > current) {
        this.stateVector.set(msg.clientId, msg.lamportClock)
      }
    }
  }

  /** 心跳 */
  private startHeartbeat(): void {
    this.heartbeatTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.sendMessage({ type: 'HEARTBEAT' })
      }
    }, 15000) // 每 15 秒
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /** 断线重连 */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 30000)
      console.log(`[CollabWS] reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
      this.connect()
    }, this.reconnectDelay)
  }

  /** 触发事件 */
  private trigger(type: string, msg: CollabMessage): void {
    const handlers = this.handlers.get(type)
    if (handlers) {
      handlers.forEach(h => {
        try { h(msg) } catch (e) { console.error('[CollabWS] handler error:', e) }
      })
    }
  }

  // ======== WebRTC 集成 ========

  /** 向指定对端发消息（优先 WebRTC，不可用时回退 WebSocket） */
  sendTo(clientId: string, msg: Partial<CollabMessage>, rtcClient?: unknown): void {
    // rtcClient 实际类型是 WebRTCClient，但为避免循环依赖，使用 unknown + duck typing
    const rtc = rtcClient as { isP2PConnected?: (id: string) => boolean; sendTo?: (id: string, data: unknown) => void } | null
    if (rtc?.isP2PConnected?.(clientId)) {
      rtc.sendTo!(clientId, msg)
    } else {
      // WebSocket 广播（服务端会转发给对应客户端）
      this.sendMessage({ ...msg, clientId })
    }
  }

  /** 获取原始的 WebSocket 发送方法（供 WebRTCClient 降级使用） */
  getRawSender(): (clientId: string, msg: unknown) => void {
    return (clientId: string, msg: unknown) => {
      this.sendMessage({
        type: 'CURSOR',
        clientId: clientId,
        ...(msg as Record<string, unknown>),
      } as Partial<CollabMessage>)
    }
  }
}
