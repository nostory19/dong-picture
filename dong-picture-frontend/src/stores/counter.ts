import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

// 一个计数类的示例

export const useCounterStore = defineStore('counter', () => {
  // 定义状态
  const count = ref(0)
  // 计算逻辑
  const doubleCount = computed(() => count.value * 2)
  // 方法
  function increment() {
    count.value++
  }

  return { count, doubleCount, increment }
})
