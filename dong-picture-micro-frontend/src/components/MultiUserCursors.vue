<template>
  <svg
    v-if="visibleCursors.length > 0"
    class="multi-user-cursors"
    :width="containerWidth"
    :height="containerHeight"
    :viewBox="`0 0 ${containerWidth} ${containerHeight}`"
    style="position: absolute; top: 0; left: 0; pointer-events: none; z-index: 10"
  >
    <g v-for="cursor in visibleCursors" :key="cursor.clientId">
      <!-- 光标图标 -->
      <polygon
        :points="cursorPoints(cursor.cursorX, cursor.cursorY)"
        :fill="cursor.color"
        :opacity="cursor.connected ? 0.9 : 0.3"
      />
      <!-- 用户名称气泡 -->
      <rect
        :x="cursor.cursorX + 12"
        :y="cursor.cursorY + 8"
        :width="cursor.nameWidth"
        height="20"
        rx="4"
        :fill="cursor.color"
        :opacity="cursor.connected ? 0.9 : 0.3"
      />
      <!-- 用户名 -->
      <text
        :x="cursor.cursorX + 16"
        :y="cursor.cursorY + 22"
        fill="#fff"
        font-size="11"
        font-weight="600"
      >
        {{ cursor.displayName }}
      </text>
      <!-- 编辑动作气泡 -->
      <template v-if="cursor.editingField">
        <rect
          :x="cursor.cursorX + 12"
          :y="cursor.cursorY + 32"
          width="80"
          height="18"
          rx="4"
          :fill="cursor.color"
          opacity="0.8"
        />
        <text
          :x="cursor.cursorX + 16"
          :y="cursor.cursorY + 45"
          fill="#fff"
          font-size="10"
        >
          {{ fieldLabel(cursor.editingField) }}
        </text>
      </template>
    </g>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserPresence } from '@/utils/CanvasDocument'

interface CursorData {
  clientId: string
  cursorX: number
  cursorY: number
  displayName: string
  editingField?: string
  color: string
  connected: boolean
  nameWidth: number
}

const props = withDefaults(defineProps<{
  presences?: Map<string, UserPresence> | UserPresence[]
  /** 排除的客户端 ID（自己的光标不画） */
  excludeClientId?: string
  /** 容器宽度 */
  containerWidth?: number
  /** 容器高度 */
  containerHeight?: number
}>(), {
  presences: () => new Map(),
  containerWidth: 600,
  containerHeight: 400,
})

const COLORS = [
  '#1890ff', '#52c41a', '#fa8c16', '#f5222d',
  '#722ed1', '#13c2c2', '#eb2f96', '#faad14',
]

const userColorMap = new Map<string, string>()
let colorIdx = 0

function getUserColor(clientId: string): string {
  if (!userColorMap.has(clientId)) {
    userColorMap.set(clientId, COLORS[colorIdx++ % COLORS.length])
  }
  return userColorMap.get(clientId)!
}

const visibleCursors = computed<CursorData[]>(() => {
  const arr = props.presences instanceof Map
    ? Array.from(props.presences.values())
    : props.presences

  return arr
    .filter(p => p.clientId !== props.excludeClientId)
    .map(p => ({
      clientId: p.clientId,
      cursorX: p.cursorX || 100,
      cursorY: p.cursorY || 100,
      displayName: p.userName || p.clientId.slice(0, 8),
      editingField: p.editingField,
      color: getUserColor(p.clientId),
      connected: p.connected !== false,
      nameWidth: Math.max(60, (p.userName || p.clientId.slice(0, 8)).length * 8),
    }))
})

function cursorPoints(x: number, y: number): string {
  return `${x},${y} ${x + 12},${y + 10} ${x + 4},${y + 10} ${x + 4},${y + 16}`
}

function fieldLabel(field: string): string {
  const labels: Record<string, string> = {
    rotate: '旋转中', scale: '缩放中', cropX: '裁剪中',
    brightness: '亮度', contrast: '对比度', saturation: '饱和度',
  }
  return labels[field] || field
}
</script>

<style scoped>
.multi-user-cursors {
  user-select: none;
}
</style>
