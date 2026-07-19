<template>
  <div class="collaborative-canvas">
    <!-- 在线用户 Presence 侧栏 -->
    <div v-if="isTeamSpace && onlineUsers.length > 0" class="presence-panel">
      <div class="presence-title">在线协作 ({{ onlineUsers.length }})</div>
      <div
        v-for="user in onlineUsers"
        :key="user.clientId"
        :class="['presence-user', { offline: !user.connected }]"
      >
        <div class="presence-avatar" :style="{ backgroundColor: userColor(user.clientId) }">
          {{ (user.userName || user.clientId).charAt(0).toUpperCase() }}
        </div>
        <div class="presence-info">
          <div class="presence-name">{{ user.userName || '用户' + user.clientId.slice(0, 4) }}</div>
          <div v-if="user.editingField" class="presence-action">
            正在编辑: {{ fieldLabel(user.editingField) }}
          </div>
          <div v-else class="presence-action">正在查看</div>
        </div>
        <div :class="['presence-dot', user.connected ? 'online-dot' : 'offline-dot']" />
      </div>
    </div>

    <!-- 协作编辑控件 -->
    <div v-if="visible" class="edit-panel">
      <div class="edit-section">
        <span class="edit-label">旋转</span>
        <a-slider v-model:value="localRotate" :min="-180" :max="180" :step="1"
          @change="onRotateChange" style="width: 120px" />
        <span class="edit-value">{{ localRotate }}°</span>
      </div>
      <div class="edit-section">
        <span class="edit-label">缩放</span>
        <a-slider v-model:value="localScale" :min="0.1" :max="3" :step="0.1"
          @change="onScaleChange" style="width: 120px" />
        <span class="edit-value">{{ (localScale * 100).toFixed(0) }}%</span>
      </div>
      <div v-if="isTeamSpace" class="edit-section">
        <a-space>
          <a-button size="small" :disabled="!connected" @click="toggleEditing('rotate')">
            {{ myEditingField === 'rotate' ? '停止旋转' : '编辑旋转' }}
          </a-button>
          <a-button size="small" :disabled="!connected" @click="toggleEditing('scale')">
            {{ myEditingField === 'scale' ? '停止缩放' : '编辑缩放' }}
          </a-button>
          <a-button size="small" :disabled="!connected" @click="undo">
            <UndoOutlined /> 撤销
          </a-button>
          <a-button size="small" :disabled="!connected" @click="redo">
            <RedoOutlined /> 重做
          </a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { UndoOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { CanvasDocument, UserPresence } from '@/utils/CanvasDocument'
import { CollabWebSocket } from '@/utils/CollabWebSocket'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const props = defineProps<{
  picture?: API.PictureVO
  visible?: boolean
  isTeamSpace?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:rotate', value: number): void
  (e: 'update:scale', value: number): void
}>()

const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

// 协作引擎
const canvasDoc = ref<CanvasDocument | null>(null)
const connected = ref(false)
const synced = ref(false)

// 本地属性（双向绑定到 slider）
const localRotate = ref(0)
const localScale = ref(1)
const myEditingField = ref<string | null>(null)

// 在线用户
const onlineUsers = ref<UserPresence[]>([])

// 初始化协作
watch(
  () => [props.picture?.id, props.visible],
  async ([pictureId, visible]) => {
    if (pictureId && visible && props.isTeamSpace) {
      await nextTick()
      initCollaboration(pictureId as number)
    } else if (!visible) {
      cleanupCollaboration()
    }
  },
  { immediate: true }
)

async function initCollaboration(pictureId: number) {
  cleanupCollaboration()

  const doc = new CanvasDocument(pictureId)
  canvasDoc.value = doc

  // 监听远程属性变更
  doc.observe('rotate', (val) => {
    localRotate.value = val as number
    emit('update:rotate', val as number)
  })
  doc.observe('scale', (val) => {
    localScale.value = val as number
    emit('update:scale', val as number)
  })

  // 监听 Presence
  doc.observePresence((presences) => {
    onlineUsers.value = Array.from(presences.values())
  })

  await doc.initialize()
  connected.value = true

  // 监听同步完成
  // Note: CollabWebSocket 会在首次同步完成后触发 'synced' 事件
}

function cleanupCollaboration() {
  if (canvasDoc.value) {
    canvasDoc.value.destroy()
    canvasDoc.value = null
  }
  connected.value = false
  synced.value = false
  onlineUsers.value = []
}

// 本地编辑回调
function onRotateChange(value: number) {
  canvasDoc.value?.setProperty('rotate', value)
  myEditingField.value = 'rotate'
}

function onScaleChange(value: number) {
  canvasDoc.value?.setProperty('scale', value)
  myEditingField.value = 'scale'
}

function toggleEditing(field: string) {
  if (myEditingField.value === field) {
    myEditingField.value = null
    canvasDoc.value?.updateMyPresence({ editingField: undefined })
  } else {
    myEditingField.value = field
    canvasDoc.value?.updateMyPresence({ editingField: field })
  }
}

function undo() { canvasDoc.value?.undo() }
function redo() { canvasDoc.value?.redo() }

// 为每个用户分配颜色
const userColors = new Map<string, string>()
const COLORS = ['#1890ff', '#52c41a', '#fa8c16', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#faad14']
let colorIdx = 0
function userColor(clientId: string): string {
  if (!userColors.has(clientId)) {
    userColors.set(clientId, COLORS[colorIdx++ % COLORS.length])
  }
  return userColors.get(clientId)!
}

function fieldLabel(field: string): string {
  const labels: Record<string, string> = {
    rotate: '旋转',
    scale: '缩放',
    cropX: '裁剪',
    brightness: '亮度',
    contrast: '对比度',
    saturation: '饱和度',
    filter: '滤镜',
  }
  return labels[field] || field
}

onUnmounted(() => cleanupCollaboration())
</script>

<style scoped>
.collaborative-canvas {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.presence-panel {
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 8px;
  max-height: 160px;
  overflow-y: auto;
}

.presence-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.presence-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.presence-user.offline {
  opacity: 0.4;
}

.presence-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.presence-info {
  flex: 1;
  min-width: 0;
}

.presence-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.presence-action {
  font-size: 11px;
  color: #999;
}

.presence-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.online-dot { background: #52c41a; }
.offline-dot { background: #d9d9d9; }

.edit-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-label {
  width: 48px;
  font-size: 13px;
  color: #666;
}

.edit-value {
  width: 48px;
  font-size: 13px;
  text-align: right;
  color: #333;
}
</style>
