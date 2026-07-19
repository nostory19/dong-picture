/**
 * WebRTC P2P 客户端 — 通过信令服务器建立 P2P DataChannel，用于高频消息（光标/画笔）。
 *
 * 架构：
 *   Client A ←→ Signaling WebSocket ←→ Client B   (信令：SDP/ICE)
 *   Client A ←→ P2P DataChannel ←→ Client B       (数据：CURSOR/PRESENCE)
 *
 * NAT 穿透失败时自动降级：所有数据回退到 WebSocket 通道。
 *
 * 使用方式：
 *   const rtc = new WebRTCClient(pictureId, myClientId)
 *   rtc.onPeerJoin = (clientId) => { ... }
 *   rtc.onData = (clientId, msg) => { ... }
 *   rtc.connect()
 *   rtc.sendTo(clientId, { type: 'CURSOR', cursorX: 100, cursorY: 200 })
 */

export interface RTCSignalMessage {
  type: string           // OFFER, ANSWER, ICE_CANDIDATE, PEER_JOIN, PEER_LEAVE
  fromClientId?: string
  targetClientId?: string
  sdp?: string
  candidate?: string
  sdpMid?: string
  sdpMLineIndex?: number
  payload?: string
}

/** ICE 服务器配置（STUN + TURN） */
const ICE_SERVERS: RTCConfiguration = {
  iceServers: [
    // Google 公共 STUN（开发环境）
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    // TURN 服务器（生产环境兜底，部署 coturn）
    // {
    //   urls: 'turn:turn.example.com:3478',
    //   username: 'collab',
    //   credential: 'your-turn-password',
    // },
  ],
  iceCandidatePoolSize: 2,
}

interface PeerConnection {
  pc: RTCPeerConnection
  dataChannel: RTCDataChannel | null
  clientId: string
  connected: boolean
}

type DataHandler = (clientId: string, data: unknown) => void
type PeerEventHandler = (clientId: string) => void

export class WebRTCClient {
  private pictureId: number
  private myClientId: string
  private signalingWs: WebSocket | null = null

  /** clientId → PeerConnection */
  private peers: Map<string, PeerConnection> = new Map()

  /** 未能建立 P2P 的客户端（降级到 WebSocket） */
  private fallbackPeers: Set<string> = new Set()

  /** 重连定时器 */
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // 事件回调
  onPeerJoin: PeerEventHandler | null = null
  onPeerLeave: PeerEventHandler | null = null
  onData: DataHandler | null = null
  onConnected: ((clientId: string) => void) | null = null
  onDisconnected: ((clientId: string) => void) | null = null
  /** 当 P2P 连接失败时应降级的消息发送器（外部注入 WebSocket 发送） */
  fallbackSender: ((clientId: string, msg: unknown) => void) | null = null

  constructor(pictureId: number, myClientId: string) {
    this.pictureId = pictureId
    this.myClientId = myClientId
  }

  /** 连接信令服务器 */
  connect(): void {
    // 直连 collaboration-service 的 WebRTC 信令端点
    const baseUrl = 'ws://localhost:8204'
    const url = `${baseUrl}/ws/collab/signaling?pictureId=${this.pictureId}&clientId=${this.myClientId}`

    this.signalingWs = new WebSocket(url)

    this.signalingWs.onopen = () => {
      console.log('[WebRTC] signaling connected')
    }

    this.signalingWs.onmessage = (event) => {
      try {
        const msg: RTCSignalMessage = JSON.parse(event.data)
        this.handleSignal(msg)
      } catch (e) {
        console.error('[WebRTC] signal parse error:', e)
      }
    }

    this.signalingWs.onclose = () => {
      console.log('[WebRTC] signaling closed (P2P unavailable, using WS fallback)')
      this.cleanupAllPeers()
      // 不重连：WebRTC 为可选增强，协作通过 WebSocket 继续工作
    }

    this.signalingWs.onerror = (e) => {
      console.warn('[WebRTC] signaling unavailable, using WebSocket fallback for all messages')
    }
  }

  /** 断开连接 */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.cleanupAllPeers()
    if (this.signalingWs) {
      this.signalingWs.close(1000, 'user disconnect')
      this.signalingWs = null
    }
  }

  /** 发送消息到指定对端（优先 P2P，失败时降级 WebSocket） */
  sendTo(clientId: string, data: unknown): void {
    if (this.fallbackPeers.has(clientId)) {
      // 降级到 WebSocket
      this.fallbackSender?.(clientId, data)
      return
    }

    const peer = this.peers.get(clientId)
    if (peer?.dataChannel && peer.dataChannel.readyState === 'open') {
      peer.dataChannel.send(JSON.stringify(data))
    } else {
      // DataChannel 未就绪，降级
      this.fallbackPeers.add(clientId)
      this.fallbackSender?.(clientId, data)
    }
  }

  /** 广播消息到所有已连接的对端 */
  broadcast(data: unknown, excludeClientId?: string): void {
    for (const [clientId] of this.peers) {
      if (clientId !== excludeClientId) {
        this.sendTo(clientId, data)
      }
    }
  }

  /** 检查是否与指定对端有 P2P 连接 */
  isP2PConnected(clientId: string): boolean {
    const peer = this.peers.get(clientId)
    return peer?.connected === true &&
           peer.dataChannel?.readyState === 'open'
  }

  // ======== 信令处理 ========

  private handleSignal(msg: RTCSignalMessage): void {
    const fromId = msg.fromClientId
    if (!fromId || fromId === this.myClientId) return

    switch (msg.type) {
      case 'PEER_JOIN':
        // 有新 peer 加入 → 主动发起连接
        this.createPeerConnection(fromId)
        this.sendOffer(fromId)
        this.onPeerJoin?.(fromId)
        break

      case 'OFFER':
        this.handleOffer(fromId, msg.sdp!)
        break

      case 'ANSWER':
        this.handleAnswer(fromId, msg.sdp!)
        break

      case 'ICE_CANDIDATE':
        this.handleIceCandidate(fromId, msg)
        break

      case 'PEER_LEAVE':
        this.removePeer(fromId)
        this.onPeerLeave?.(fromId)
        break
    }
  }

  // ======== P2P 连接管理 ========

  private createPeerConnection(clientId: string): RTCPeerConnection {
    const pc = new RTCPeerConnection(ICE_SERVERS)

    // 创建 DataChannel（主动方）
    const dataChannel = pc.createDataChannel('collab', {
      ordered: false,      // 允许乱序（高频光标消息不需要严格顺序）
      maxRetransmits: 0,   // 不重传（允许丢帧）
    })

    this.setupDataChannel(clientId, dataChannel)

    // ICE candidate 事件 → 通过信令服务器转发
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignal({
          type: 'ICE_CANDIDATE',
          targetClientId: clientId,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid ?? undefined,
          sdpMLineIndex: event.candidate.sdpMLineIndex ?? undefined,
        })
      }
    }

    // 连接状态变化
    pc.onconnectionstatechange = () => {
      const state = pc.connectionState
      console.log(`[WebRTC] connection to ${clientId}: ${state}`)
      if (state === 'failed' || state === 'disconnected') {
        this.fallbackPeers.add(clientId)
        this.onDisconnected?.(clientId)
      } else if (state === 'connected') {
        this.fallbackPeers.delete(clientId)
        this.onConnected?.(clientId)
      }
    }

    // 被动方接收 DataChannel
    pc.ondatachannel = (event) => {
      this.setupDataChannel(clientId, event.channel)
    }

    const peer: PeerConnection = { pc, dataChannel, clientId, connected: false }
    this.peers.set(clientId, peer)
    return pc
  }

  private setupDataChannel(clientId: string, channel: RTCDataChannel): void {
    channel.onopen = () => {
      console.log(`[WebRTC] DataChannel opened with ${clientId}`)
      const peer = this.peers.get(clientId)
      if (peer) {
        peer.dataChannel = channel
        peer.connected = true
      }
      this.fallbackPeers.delete(clientId)
    }

    channel.onclose = () => {
      console.log(`[WebRTC] DataChannel closed with ${clientId}`)
      this.fallbackPeers.add(clientId)
    }

    channel.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.onData?.(clientId, data)
      } catch (e) {
        console.error('[WebRTC] DataChannel parse error:', e)
      }
    }

    channel.onerror = (e) => {
      console.error(`[WebRTC] DataChannel error with ${clientId}:`, e)
      this.fallbackPeers.add(clientId)
    }
  }

  // ======== SDP 交换 ========

  private async sendOffer(clientId: string): Promise<void> {
    const peer = this.peers.get(clientId)
    if (!peer) return

    try {
      const offer = await peer.pc.createOffer()
      await peer.pc.setLocalDescription(offer)
      this.sendSignal({
        type: 'OFFER',
        targetClientId: clientId,
        sdp: peer.pc.localDescription?.sdp,
      })
    } catch (e) {
      console.error('[WebRTC] createOffer failed:', e)
      this.fallbackPeers.add(clientId)
    }
  }

  private async handleOffer(fromId: string, sdp: string): Promise<void> {
    let peer = this.peers.get(fromId)
    if (!peer) {
      this.createPeerConnection(fromId)
      peer = this.peers.get(fromId)!
    }

    try {
      await peer.pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'offer', sdp })
      )
      const answer = await peer.pc.createAnswer()
      await peer.pc.setLocalDescription(answer)
      this.sendSignal({
        type: 'ANSWER',
        targetClientId: fromId,
        sdp: peer.pc.localDescription?.sdp,
      })
    } catch (e) {
      console.error('[WebRTC] handleOffer failed:', e)
      this.fallbackPeers.add(fromId)
    }
  }

  private async handleAnswer(fromId: string, sdp: string): Promise<void> {
    const peer = this.peers.get(fromId)
    if (!peer) return

    try {
      await peer.pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp })
      )
    } catch (e) {
      console.error('[WebRTC] handleAnswer failed:', e)
      this.fallbackPeers.add(fromId)
    }
  }

  private async handleIceCandidate(fromId: string, msg: RTCSignalMessage): Promise<void> {
    const peer = this.peers.get(fromId)
    if (!peer || !msg.candidate) return

    try {
      await peer.pc.addIceCandidate(
        new RTCIceCandidate({
          candidate: msg.candidate,
          sdpMid: msg.sdpMid ?? undefined,
          sdpMLineIndex: msg.sdpMLineIndex ?? undefined,
        })
      )
    } catch (e) {
      console.error('[WebRTC] addIceCandidate failed:', e)
    }
  }

  // ======== 辅助方法 ========

  private sendSignal(msg: RTCSignalMessage): void {
    if (this.signalingWs?.readyState === WebSocket.OPEN) {
      this.signalingWs.send(JSON.stringify(msg))
    }
  }

  private removePeer(clientId: string): void {
    const peer = this.peers.get(clientId)
    if (peer) {
      peer.dataChannel?.close()
      peer.pc.close()
      this.peers.delete(clientId)
    }
    this.fallbackPeers.delete(clientId)
  }

  private cleanupAllPeers(): void {
    for (const [clientId] of this.peers) {
      this.removePeer(clientId)
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => this.connect(), 3000)
  }
}
