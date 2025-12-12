<template>
  <div class="space-category-analyze">
    <a-card title="空间使用排行分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import {
  getSpaceCategoryAnalyzeUsingPost, getSpaceRankAnalyzeUsingPost,
  getSpaceUsageAnalyzeUsingPost
} from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'
import VChart from 'vue-echarts'
import 'echarts'
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
const dataList = ref<API.SpaceCategoryAnalyzeResponse[]>([])
const loading = ref(true)

/**
 * 加载数据
 * 空间使用排行分析
 */
const fetchData = async () => {
  loading.value = true
  const res = await getSpaceRankAnalyzeUsingPost({
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
  // 包含前N个的空间使用排行
  const spaceName = dataList.value.map((item) => item.spaceName)
  const usageData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2)) // 转为MB

  return {
    tooltip: {trigger: 'axis'},
    xAxis: {
      type: 'category',
      data: spaceName,
    },
    yAxis: {
      type: 'value',
      name: '空间使用量(MB)'
    },
    series: [
      {
        name: '空间使用量(MB)',
        type: 'bar',
        data: usageData,
        itemStyle: {
          color: '#5470C6', // 自定义柱状颜色
        },
      },
    ],
  }
})
</script>

<style scoped></style>
