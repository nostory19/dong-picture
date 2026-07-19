<template>
  <button
    :class="['thumb-btn', { thumbed: disHasThumb, disabled: !isLogin }]"
    @click.stop="handleClick"
    :disabled="!isLogin"
  >
    <svg class="thumb-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
      <path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/>
    </svg>
    <transition name="count-fade" mode="out-in">
      <span :key="displayCount" class="thumb-count">{{ formatCount(displayCount) }}</span>
    </transition>
  </button>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { doThumbUsingPost, undoThumbUsingPost } from '@/api/thumbController'
import { message } from 'ant-design-vue'

const props = defineProps<{
  pictureId: number | string
  count: number
  hasThumb: boolean
}>()

const isLogin = ref(!!localStorage.getItem('authToken'))
const displayCount = ref(props.count || 0)
const disHasThumb = ref(props.hasThumb || false)
let debounceTimer: any = null

const formatCount = (count: number) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'w'
  }
  if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return String(count)
}

const handleClick = () => {
  if (debounceTimer) return
  debounceTimer = setTimeout(() => { debounceTimer = null }, 300)
  doThumbAction()
}

const doThumbAction = async () => {
  if (disHasThumb.value) {
    try {
      await undoThumbUsingPost({ pictureId: Number(props.pictureId) })
      displayCount.value = Math.max(0, displayCount.value - 1)
      disHasThumb.value = false
    } catch {
      message.error('取消点赞失败')
    }
  } else {
    try {
      await doThumbUsingPost({ pictureId: Number(props.pictureId) })
      displayCount.value = displayCount.value + 1
      disHasThumb.value = true
    } catch {
      message.error('点赞失败')
    }
  }
}

watch(() => props.count, (val) => { displayCount.value = val || 0 })
watch(() => props.hasThumb, (val) => { disHasThumb.value = val || false })
</script>

<style scoped>
.thumb-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.25s;
  outline: none;
}
.thumb-btn:hover:not(.disabled) {
  border-color: #1677ff;
  color: #1677ff;
}
.thumb-btn.thumbed {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}
.thumb-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.thumb-count {
  min-width: 20px;
  text-align: center;
}
.count-fade-enter-active,
.count-fade-leave-active {
  transition: all 0.2s ease;
}
.count-fade-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.count-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
