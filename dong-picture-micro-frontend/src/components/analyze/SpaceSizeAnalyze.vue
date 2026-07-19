<template>
<!--  按图片大小分段统计图片的数量-->
  <div class="space-size-analyze">
    <a-card title="空间图片大小分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import {
  getSpaceSizeAnalyzeUsingPost,
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
const dataList = ref<API.SpaceSizeAnalyzeResponse[]>([])
const loading = ref(true)

/**
 * 加载数据
 */
const fetchData = async () => {
  loading.value = true
  const res = await getSpaceSizeAnalyzeUsingPost({
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
//   包含了图片的大小区间，区间内图片的数量
  const pieData = dataList.value.map((item) => ({
    name: item.sizeRange,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d})%)',
    },
    legend: {
      top: 'bottom',
    },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: '50%',
        data: pieData,
      },
    ],
  }
})
</script>

<style scoped></style>
