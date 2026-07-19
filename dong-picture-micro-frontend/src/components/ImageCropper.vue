<template>
<!--缩放，旋转的操作-->
  <a-modal class="image-cropper" v-model:visible="visible"
           title="编辑图片" :footer="false" @cancel="closeModal" >
    <vue-cropper
      ref="cropperRef"
      :img="imageUrl"
      :auto-crop="true"
      :fixed-box="false"
      :center-box="true"
      :can-move-box="true"
      :info="true"
      output-type="png"
    />
    <!-- 多人光标叠加层 -->
    <MultiUserCursors
      v-if="isTeamSpace && showCursors"
      :presences="presenceList"
      :exclude-client-id="myClientId"
      :container-width="600"
      :container-height="400"
    />

    <div style="margin-bottom: 16px" />

    <!-- 协作 Presence 面板（替代旧的独占锁 UI） -->
    <div class="collab-section" v-if="isTeamSpace">
      <div class="collab-header">
        <span class="collab-title">协作编辑</span>
        <a-tag v-if="useNewProtocol" color="green">多人协作</a-tag>
        <a-tag v-else-if="editingUser" color="orange">
          {{ editingUser.userName || '用户' + editingUser.id }} 正在编辑
        </a-tag>
        <a-tag v-else color="default">可编辑</a-tag>
      </div>
      <!-- 在线用户列表 -->
      <PresencePanel
        v-if="useNewProtocol && presenceList.length > 0"
        :presences="presenceList"
        :self-client-id="myClientId"
        :self-user-id="loginUser?.id"
        :compact="true"
      />
      <!-- 旧协议：进入/退出编辑按钮 -->
      <a-space v-if="!useNewProtocol" style="margin-top: 8px">
        <a-button v-if="!wsConnected" type="primary" ghost disabled loading>连接中...</a-button>
        <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
        <a-button v-if="canExitEdit" danger ghost @click="exitEdit">退出编辑</a-button>
      </a-space>
    </div>

    <div style="margin-bottom: 16px" />

    <!--    图片操作（移除独占锁限制，团队空间下所有人都可操作） -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
        <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
        <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
        <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm" :disabled="!canEdit">确认</a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watchEffect } from 'vue'
import 'vue-cropper/next/dist/index.css'
import {VueCropper} from 'vue-cropper/next'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import PictureEditWebSocket from '@/utils/PictureEditWebSocket.ts'
import { CollabWebSocket, type CollabMessage } from '@/utils/CollabWebSocket.ts'
import { WebRTCClient } from '@/utils/WebRTCClient.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
import MultiUserCursors from './MultiUserCursors.vue'
import PresencePanel from './PresencePanel.vue'
import type { UserPresence } from '@/utils/CanvasDocument'
// 定义属性
interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
  /** 其他用户提交编辑后触发，父组件需重新拉取图片数据 */
  onRefreshNeeded?: () => void
}

const loading = ref(false)

const props = defineProps<Props>()
// 是否为团队空间
const isTeamSpace = computed(() => {
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})
// 编辑器组件的使用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}


// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num : number) => {
  cropperRef.value.changeScale(num)
  if (num > 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  }else if (num < 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}


// 嵌套的弹窗组件
const visible = ref(false)

const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  // 关闭弹窗也要去断开websocket连接
  visible.value = false
  if(websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
}

// 暴露函数给父组件
defineExpose({
  openModal,
})

// 编写上传函数，点击确认后将blob数据转换为file对象

const handleConfirm = () => {
  // blob为已裁切的文件
  cropperRef.value.getCropBlob((blob: Blob) => {
    // 获取文件名
    const fileName = (props.picture?.name || 'image') + '.png'
    // 转换为file对象
    const file = new File([blob], fileName, {type: blob.type})
    // 上传图片
    handleUpload({file})
  })
}


const handleUpload = async ({file}: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? {id: props.picture.id} : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 广播 COMMIT 通知其他协作编辑者
      editAction(PICTURE_EDIT_ACTION_ENUM.COMMIT)
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    }else {
      message.error('图片上传失败, ' + res.data.message)
    }
  }catch (error){
    message.error('图片上传失败')
  }finally {
    loading.value = false
  }
}

// -----------------实时编辑（支持新旧协议）----------------
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

// 旧协议：独占编辑用户
const editingUser = ref<API.UserVO>()

// 新协议：Presence 列表
const presenceList = ref<UserPresence[]>([])

// 是否使用新协议（多人协作模式）
const useNewProtocol = ref(true)

// 生成客户端 ID（持久化到 localStorage）
const myClientId = ref(loadOrCreateClientId())

// 是否显示光标叠加层
const showCursors = ref(true)

// WebSocket 是否已连接
const wsConnected = ref(false)

// 当前用户是否可点击"进入编辑" — 需要等 WebSocket 连接就绪
const canEnterEdit = computed(() => {
  if (!wsConnected.value) return false
  // 新协议：允许多人同时进入编辑
  if (useNewProtocol.value) return true
  // 旧协议：独占编辑，只有没人编辑时才能进入
  return !editingUser.value
})

// 当前用户是否可以退出编辑
const canExitEdit = computed(() => {
  if (useNewProtocol.value) return true // 新协议：随时可退出
  return editingUser.value?.id === loginUser?.id
})

// 是否可以操作编辑按钮（旋转/缩放）
const canEdit = computed(() => {
  if (!isTeamSpace.value) return true
  // 新协议：始终可编辑（多人协作不互斥）
  if (useNewProtocol.value) return true
  // 旧协议：只有独占编辑者可编辑
  return editingUser.value?.id === loginUser?.id
})

// WebSocket 连接
let websocket: PictureEditWebSocket | CollabWebSocket | null = null

// WebRTC P2P 客户端
let rtcClient: WebRTCClient | null = null

// 初始化 WebSocket
const initWebsocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) return

  // 防止之前的连接未释放
  if (websocket) {
    websocket.disconnect()
  }

  if (useNewProtocol.value) {
    initNewProtocolWebSocket(pictureId)
  } else {
    initOldProtocolWebSocket(pictureId)
  }
}

// 旧协议 WebSocket（向后兼容）
function initOldProtocolWebSocket(pictureId: number) {
  const ws = new PictureEditWebSocket(pictureId)
  websocket = ws
  wsConnected.value = false
  ws.connect()
  ws.on('open', () => {
    wsConnected.value = true
    console.log('[ImageCropper] WebSocket connected')
  })
  ws.on('close', () => {
    wsConnected.value = false
  })
  ws.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg: any) => {
    message.info(msg.message)
  })
  ws.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg: any) => {
    message.error(msg.message)
  })
  ws.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg: any) => {
    message.info(msg.message)
    editingUser.value = msg.user
  })
  ws.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg: any) => {
    message.info(msg.message)
    editingUser.value = undefined
  })
  ws.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg: any) => {
    message.info(msg.message)
    applyRemoteAction(msg.editAction)
  })
}

// 新协议 WebSocket（CRDT 协作 + WebRTC P2P）
function initNewProtocolWebSocket(pictureId: number) {
  const ws = new CollabWebSocket(pictureId, myClientId.value)
  websocket = ws
  wsConnected.value = false

  // 初始化 WebRTC P2P 客户端
  initWebRTC(pictureId, ws)

  // 新协议消息处理
  ws.on('OPERATION', (msg: CollabMessage) => {
    if (msg.message) message.info(msg.message)
    if (msg.editAction) {
      applyRemoteAction(msg.editAction)
    }
  })

  ws.on('PRESENCE', (msg: CollabMessage) => {
    updatePresenceFromMsg(msg)
  })

  // CURSOR 消息（WebSocket 降级通道，WebRTC 可用时走 P2P）
  ws.on('CURSOR', (msg: CollabMessage) => {
    if (msg.clientId && rtcClient?.isP2PConnected(msg.clientId)) return // P2P 已处理
    updatePresenceFromMsg(msg)
  })

  ws.on('CLIENT_JOIN', (msg: CollabMessage) => {
    if (msg.clientId) {
      addPresence(msg.clientId, msg.message || '加入了协作')
      message.info(msg.message || '有人加入了协作')
    }
  })

  ws.on('CLIENT_LEAVE', (msg: CollabMessage) => {
    if (msg.clientId) {
      removePresence(msg.clientId)
      message.info(msg.message || '有人离开了协作')
    }
  })

  ws.on('INFO', (msg: CollabMessage) => {
    if (msg.onlineClients) {
      presenceList.value = msg.onlineClients.map((c: any) => ({
        clientId: c.clientId || '',
        userName: c.userName,
        userAvatar: c.userAvatar,
        editingField: c.editingField,
        cursorX: c.cursorX || 0,
        cursorY: c.cursorY || 0,
        connected: true,
      } as UserPresence))
    }
    message.info(msg.message || '')
  })

  ws.on('ERROR', (msg: CollabMessage) => {
    message.error(msg.message || '协作错误')
  })

  ws.on('open', () => {
    wsConnected.value = true
    console.log('[ImageCropper] CollabWS connected')
  })

  ws.on('close', () => {
    wsConnected.value = false
  })

  ws.connect()
}

// WebRTC P2P 初始化
function initWebRTC(pictureId: number, ws: CollabWebSocket) {
  if (rtcClient) {
    rtcClient.disconnect()
  }
  rtcClient = new WebRTCClient(pictureId, myClientId.value)

  // 当新 peer 加入时，不需要额外操作（信令自动处理）

  // 当收到 P2P 数据时（光标消息）
  rtcClient.onData = (clientId: string, data: any) => {
    if (data.type === 'CURSOR') {
      updatePresenceFromMsg({
        clientId,
        editingField: data.editingField,
        cursorX: data.cursorX,
        cursorY: data.cursorY,
      })
    }
  }

  // 当 P2P 连接成功时
  rtcClient.onConnected = (clientId: string) => {
    console.log(`[ImageCropper] P2P connected with ${clientId}`)
    // 更新 presence 中该用户的 connected 状态
    updatePresenceFromMsg({ clientId, editingField: undefined, cursorX: 0, cursorY: 0 } as CollabMessage)
  }

  // P2P 断开时
  rtcClient.onDisconnected = (clientId: string) => {
    console.log(`[ImageCropper] P2P disconnected with ${clientId}, falling back to WS`)
  }

  // 注入降级发送器（P2P 不可用时走 WebSocket）
  rtcClient.fallbackSender = ws.getRawSender()

  rtcClient.connect()
}

// Presence 管理
function updatePresenceFromMsg(msg: CollabMessage) {
  if (!msg.clientId) return
  const idx = presenceList.value.findIndex(p => p.clientId === msg.clientId)
  const p: UserPresence = {
    clientId: msg.clientId,
    editingField: msg.editingField,
    cursorX: msg.cursorX || 0,
    cursorY: msg.cursorY || 0,
    connected: true,
  }
  if (idx >= 0) {
    presenceList.value[idx] = { ...presenceList.value[idx], ...p }
  } else {
    presenceList.value.push(p)
  }
}

function addPresence(clientId: string, userName?: string) {
  if (!presenceList.value.find(p => p.clientId === clientId)) {
    presenceList.value.push({
      clientId,
      userName,
      cursorX: 0,
      cursorY: 0,
      connected: true,
    })
  }
}

function removePresence(clientId: string) {
  const idx = presenceList.value.findIndex(p => p.clientId === clientId)
  if (idx >= 0) {
    presenceList.value[idx] = { ...presenceList.value[idx], connected: false }
  }
}

// 应用远程编辑动作（新旧协议通用）
function applyRemoteAction(action: string) {
  switch (action) {
    case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
      cropperRef.value?.rotateLeft()
      break
    case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
      cropperRef.value?.rotateRight()
      break
    case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
      cropperRef.value?.changeScale(1)
      break
    case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
      cropperRef.value?.changeScale(-1)
      break
    case PICTURE_EDIT_ACTION_ENUM.COMMIT:
      // 其他用户已提交编辑，关闭弹窗，通知父组件刷新
      message.info('其他用户已提交编辑，页面即将刷新')
      closeModal()
      props.onRefreshNeeded?.()
      break
  }
}

// 生命周期
watchEffect(() => {
  if (isTeamSpace.value) initWebsocket()
})

onUnmounted(() => {
  if (rtcClient) {
    rtcClient.disconnect()
    rtcClient = null
  }
  if (websocket) {
    websocket.disconnect()
  }
  wsConnected.value = false
  editingUser.value = undefined
  presenceList.value = []
})

// 旧协议按钮
function enterEdit() {
  if (websocket instanceof PictureEditWebSocket) {
    websocket.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT })
  }
}

function exitEdit() {
  if (websocket instanceof PictureEditWebSocket) {
    websocket.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT })
  }
}

// 编辑操作 — 同时支持新旧协议
function editAction(action: string) {
  if (!websocket) return
  if (useNewProtocol.value && websocket instanceof CollabWebSocket) {
    // 新协议：通过 editAction 传递操作类型（与旧协议兼容同一个字段）
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.OPERATION,
      editAction: action,
    })
  } else if (websocket instanceof PictureEditWebSocket) {
    // 旧协议：发送 EDIT_ACTION
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: action,
    })
  }
}

function loadOrCreateClientId(): string {
  const key = 'collabClientId'
  let id = localStorage.getItem(key)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(key, id)
  }
  return id
}
</script>



<style>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}
</style>
