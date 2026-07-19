<template>
  <div
    class="mini-map"
    :style="{ width: width + 'px', height: height + 'px' }"
  >
    <div class="mini-map-canvas" ref="canvasContainer">
      <canvas ref="canvasRef" :width="width" :height="height" />
      <!-- 当前视口框 -->
      <div
        class="viewport-rect"
        :style="viewportStyle"
        @mousedown="onDragStart"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

const props = withDefaults(defineProps<{
  imageUrl?: string
  /** 画布总宽度 */
  canvasWidth?: number
  /** 画布总高度 */
  canvasHeight?: number
  /** 当前视口偏移 X */
  viewportX?: number
  /** 当前视口偏移 Y */
  viewportY?: number
  /** 视口宽度 */
  viewportW?: number
  /** 视口高度 */
  viewportH?: number
}>(), {
  canvasWidth: 1200,
  canvasHeight: 800,
  viewportX: 0,
  viewportY: 0,
  viewportW: 300,
  viewportH: 200,
})

const emit = defineEmits<{
  (e: 'navigate', x: number, y: number): void
}>()

const width = 150
const height = 100
const canvasRef = ref<HTMLCanvasElement | null>(null)

let dragging = false

const scaleX = computed(() => width / props.canvasWidth)
const scaleY = computed(() => height / props.canvasHeight)

const viewportStyle = computed(() => ({
  left: (props.viewportX * scaleX.value) + 'px',
  top: (props.viewportY * scaleY.value) + 'px',
  width: (props.viewportW * scaleX.value) + 'px',
  height: (props.viewportH * scaleY.value) + 'px',
}))

onMounted(() => {
  if (props.imageUrl && canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      const img = new Image()
      img.onload = () => {
        ctx.drawImage(img, 0, 0, width, height)
      }
      img.src = props.imageUrl
    }
    // 绘制柔和边框
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      ctx.strokeStyle = '#d9d9d9'
      ctx.lineWidth = 1
      ctx.strokeRect(0, 0, width, height)
    }
  }
})

watch(() => props.imageUrl, (url) => {
  if (url && canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      const img = new Image()
      img.onload = () => ctx.drawImage(img, 0, 0, width, height)
      img.src = url
    }
  }
})

function onDragStart(e: MouseEvent) {
  dragging = true
  const startX = e.clientX
  const startY = e.clientY
  const startVX = props.viewportX
  const startVY = props.viewportY

  const onMove = (ev: MouseEvent) => {
    if (!dragging) return
    const dx = (ev.clientX - startX) / scaleX.value
    const dy = (ev.clientY - startY) / scaleY.value
    emit('navigate', Math.max(0, startVX + dx), Math.max(0, startVY + dy))
  }
  const onUp = () => {
    dragging = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}
</script>

<style scoped>
.mini-map {
  position: relative;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.mini-map-canvas {
  position: relative;
  width: 100%;
  height: 100%;
}

.mini-map-canvas canvas {
  display: block;
}

.viewport-rect {
  position: absolute;
  border: 2px solid #1890ff;
  background: rgba(24, 144, 255, 0.1);
  cursor: move;
  border-radius: 2px;
}
</style>
