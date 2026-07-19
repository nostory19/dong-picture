<template>
  <div v-if="users.length > 0 || !compact" class="presence-panel" :class="{ compact }">
    <div v-if="!compact" class="panel-header">
      <span class="panel-title">协作成员</span>
      <a-badge :count="onlineCount" :number-style="{ backgroundColor: '#52c41a' }" />
    </div>

    <div class="user-list">
      <div
        v-for="user in users"
        :key="user.clientId"
        class="user-row"
        :class="{ 'is-self': user.isSelf, 'is-offline': !user.connected }"
      >
        <!-- 头像 -->
        <a-badge
          :dot="true"
          :color="user.connected ? '#52c41a' : '#d9d9d9'"
          :offset="[-2, 26]"
        >
          <div class="avatar" :style="{ backgroundColor: user.color }">
            {{ user.initial }}
          </div>
        </a-badge>

        <!-- 信息 -->
        <div class="user-info">
          <div class="user-name">
            {{ user.displayName }}
            <a-tag v-if="user.isSelf" color="blue" style="font-size: 10px; line-height: 16px">我</a-tag>
          </div>
          <div class="user-status">
            <template v-if="!user.connected">
              <span class="offline-text">已离线</span>
            </template>
            <template v-else-if="user.editingField">
              <a-tag :color="user.color" style="font-size: 10px; line-height: 16px">
                {{ fieldLabel(user.editingField) }}
              </a-tag>
            </template>
            <template v-else>
              <span class="viewing-text">查看中</span>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!compact && users.length === 0" class="empty-hint">
      暂无其他成员在线
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserPresence } from '@/utils/CanvasDocument'

const props = withDefaults(defineProps<{
  presences?: Map<string, UserPresence> | UserPresence[]
  /** 当前用户的客户端 ID */
  selfClientId?: string
  /** 当前用户 ID（用于头像高亮） */
  selfUserId?: number
  /** 紧凑模式（用于侧栏） */
  compact?: boolean
}>(), {
  presences: () => new Map(),
})

const COLORS = [
  '#1890ff', '#52c41a', '#fa8c16', '#f5222d',
  '#722ed1', '#13c2c2', '#eb2f96', '#faad14',
]
const colorMap = new Map<string, string>()
let cIdx = 0

function getColor(clientId: string): string {
  if (!colorMap.has(clientId)) {
    colorMap.set(clientId, COLORS[cIdx++ % COLORS.length])
  }
  return colorMap.get(clientId)!
}

interface DisplayUser {
  clientId: string
  displayName: string
  initial: string
  editingField?: string
  color: string
  connected: boolean
  isSelf: boolean
}

const users = computed<DisplayUser[]>(() => {
  const arr = props.presences instanceof Map
    ? Array.from(props.presences.values())
    : props.presences

  return arr.map(p => {
    const name = p.userName || `用户${p.clientId.slice(0, 4)}`
    return {
      clientId: p.clientId,
      displayName: name,
      initial: name.charAt(0).toUpperCase(),
      editingField: p.editingField,
      color: getColor(p.clientId),
      connected: p.connected !== false,
      isSelf: p.clientId === props.selfClientId,
    }
  })
})

const onlineCount = computed(() => users.value.filter(u => u.connected).length)

function fieldLabel(field: string): string {
  const labels: Record<string, string> = {
    rotate: '旋转', scale: '缩放', cropX: '裁剪',
    cropY: '裁剪', cropW: '裁剪', cropH: '裁剪',
    brightness: '亮度', contrast: '对比度', saturation: '饱和度',
    filter: '滤镜',
  }
  return labels[field] || field
}
</script>

<style scoped>
.presence-panel {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.presence-panel.compact {
  padding: 8px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-row:hover {
  background: #f0f0f0;
}

.user-row.is-offline {
  opacity: 0.5;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  margin-top: 2px;
}

.viewing-text {
  font-size: 11px;
  color: #999;
}

.offline-text {
  font-size: 11px;
  color: #ccc;
}

.empty-hint {
  text-align: center;
  color: #ccc;
  font-size: 12px;
  padding: 12px 0;
}
</style>
