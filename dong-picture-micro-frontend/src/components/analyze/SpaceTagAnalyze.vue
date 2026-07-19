<template>
  <div class="space-tag-analyze">
    <a-card title="图库标签词云">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import {
 getSpaceTagAnalyzeUsingPost,
} from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'
import VChart from 'vue-echarts'
import 'echarts'
import 'echarts-wordcloud'
// 后端公共请求属性
// 接收父页面传递来的参数
interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})
// 先编写获取数据逻辑

// 图表数据
// 返回结果是一个列表
const dataList = ref<API.SpaceTagAnalyzeResponse[]>([])
const loading = ref(true)

/**
 * 加载数据
 */
const fetchData = async () => {
  loading.value = true
  const res = await getSpaceTagAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })

  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }

  loading.value = false
}

/**
 * 监听变量，改变时出发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})
// 不能只在页面加载时获取，需要监听

// 图表选项
// 动态计算options
const options = computed(() => {
  // 包含了标签名称，标签数量
  const tagData = dataList.value.map((item) => ({
    name: item.tag,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name}:${params.value}次`,
    },
    series: [
      {
        type: 'wordCloud',
        gridSize: 10,
        sizeRange: [12, 50], // 字体大小范围
        rotationRange: [-90, 90],
        shape: 'circle',
        testStyle: {
          color: () =>
            `rgb(${Math.round(Math.random() * 255)}, ${Math.round(Math.random() * 255)}, ${Math.round(Math.random() * 255)}`
        },
        // 随机颜色
        data: tagData,
      }
    ],
  }
})
</script>

<style scoped></style>
