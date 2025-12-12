<template>
  <div class="space-category-analyze">
    <a-card title="空间图片分类分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import {
  getSpaceCategoryAnalyzeUsingPost,
  getSpaceUsageAnalyzeUsingPost,
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
 */
const fetchData = async () => {
  loading.value = true
  const res = await getSpaceCategoryAnalyzeUsingPost({
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
  // 包含了category, count, totalSize
  const categories = dataList.value.map((item) => item.category)
  const countData = dataList.value.map((item) => item.count)
  const sizeData = dataList.value.map((item) => (item.totalSize / (1 * 1024 * 1024)).toFixed(2))

  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['图片数量', '图片总大小'],
      top: 'bottom',
    },
    xAxis: {
      type: 'category',
      data: categories,
    },
    yAxis: [
      {
        type: 'value',
        name: '图片数量',
        axisLine: {
          show: true,
          linStyle: {
            color: '#5470C6',
          },
        },
        // 左轴颜色
      },
      {
        type: 'value',
        name: '图片总大小(MB)',
        position: 'right',
        axisLine: { show: true, linStyle: { color: '#91CC75' } },
        // 右轴颜色
        splitLine: {
          lineStyle: {
            color: '#91CC75', // 调整网格线颜色
            type: 'dashed', // }
          },
        },
      },
    ],
    series: [
      {
        name: '图片数量',
        type: 'bar',
        data: countData,
        yAxisIndex: 0,
      },
      {
        name: '图片总大小',
        type: 'bar',
        data: sizeData,
        yAxisIndex: 1,
      },
    ],
  }
})
</script>

<style scoped></style>
