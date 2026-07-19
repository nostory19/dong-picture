<template>
  <div v-if="annotations.length > 0 || !readonly" class="annotation-layer">
    <!-- 已有批注 -->
    <div
      v-for="(ann, idx) in annotations"
      :key="idx"
      class="annotation-marker"
      :style="{ left: ann.x + 'px', top: ann.y + 'px' }"
      @click="onClick(idx)"
    >
      <div class="marker-dot" :style="{ backgroundColor: markerColor(idx) }" />
      <div v-if="activeIndex === idx" class="marker-popup">
        <div class="marker-author">{{ ann.author }}</div>
        <div class="marker-text">{{ ann.text }}</div>
        <div class="marker-time">{{ formatTime(ann.timestamp) }}</div>
        <a-button
          v-if="canDelete(ann)"
          size="small"
          danger
          type="link"
          @click.stop="onDelete(idx)"
        >删除</a-button>
      </div>
    </div>

    <!-- 新增批注 -->
    <div
      v-if="!readonly"
      class="annotation-add-hint"
      @click="onCanvasClick"
    >
      点击图片添加批注
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface Annotation {
  x: number
  y: number
  text: string
  author: string
  authorId?: number
  timestamp: number
}

const props = withDefaults(defineProps<{
  annotations?: Annotation[]
  /** 当前用户 ID（用于判断是否可删除） */
  currentUserId?: number
  /** 只读模式 */
  readonly?: boolean
}>(), {
  annotations: () => [],
})

const emit = defineEmits<{
  (e: 'add', annotation: Omit<Annotation, 'timestamp'>): void
  (e: 'delete', index: number): void
}>()

const activeIndex = ref<number | null>(null)

const COLORS = ['#1890ff', '#52c41a', '#fa8c16', '#f5222d', '#722ed1']

function markerColor(idx: number): string {
  return COLORS[idx % COLORS.length]
}

function canDelete(ann: Annotation): boolean {
  return ann.authorId === props.currentUserId
}

function onClick(idx: number) {
  activeIndex.value = activeIndex.value === idx ? null : idx
}

function onDelete(idx: number) {
  emit('delete', idx)
  activeIndex.value = null
}

function onCanvasClick(e: MouseEvent) {
  const target = e.currentTarget as HTMLElement
  const parent = target.parentElement
  if (!parent) return

  const rect = parent.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  const text = prompt('输入批注内容:')
  if (text) {
    emit('add', { x, y, text, author: '我', authorId: props.currentUserId })
  }
}

function formatTime(ts: number): string {
  return new Date(ts).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.annotation-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.annotation-marker {
  position: absolute;
  pointer-events: auto;
  cursor: pointer;
}

.marker-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

.marker-popup {
  position: absolute;
  top: 24px;
  left: -4px;
  width: 200px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  padding: 10px;
  z-index: 100;
}

.marker-author {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.marker-text {
  font-size: 13px;
  color: #333;
  margin-bottom: 4px;
}

.marker-time {
  font-size: 11px;
  color: #bbb;
}

.annotation-add-hint {
  position: absolute;
  bottom: 8px;
  right: 8px;
  font-size: 11px;
  color: #999;
  pointer-events: auto;
  cursor: pointer;
  padding: 4px 8px;
  background: rgba(255,255,255,0.8);
  border-radius: 4px;
}

.annotation-add-hint:hover {
  color: #1890ff;
}
</style>
