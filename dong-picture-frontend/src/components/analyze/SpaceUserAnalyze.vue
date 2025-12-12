<template>
  <div class="space-user-analyze">
<!--    <a-card title="空间图片用户分析">-->
<!--      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />-->
<!--    </a-card>-->
<!--    修改为支持用户选择统计的时间范围（日，周，月）-->
    <a-card title="用户上传分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
<!--      使用卡片组件的插槽语法-->
      <template #extra>
        <a-space>
          <a-segmented v-model:value="timeDimension" :options="timeDimensionOptions"/>
<!--          不需要实时更新值而是绑定事件，这样输入userId时候就不会实时刷新了，点击搜索才会进行搜索-->
          <a-input-search placeholder="请输入用户id" enter-button="搜索用户" @search="doSearch" />
        </a-space>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import {
  getSpaceCategoryAnalyzeUsingPost,
  getSpaceUsageAnalyzeUsingPost, getSpaceUserAnalyzeUsingPost
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
// 用户行为分析，增加一些变量
const userId = ref<string>()
const timeDimension = ref<string>('day') // 默认为天，
const timeDimensionOptions = [
  {
    label: '日',
    value: 'day',
  },
  {
    label: '周',
    value: 'week',
  },
  {
    label: '月',
    value: 'month',
  },

]
/**
 * 加载数据
 */
const fetchData = async () => {
  loading.value = true
  const res = await getSpaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  //   搜索的时间维度
    timeDimension: timeDimension.value,
    userId: userId.value,
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
  const periods = dataList.value.map((item) => item.period) // 时间区间
  const counts = dataList.value.map((item) => item.count) // 上传数量

  return {
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      data: periods,
      name: '时间区间',
    },
    yAxis: {
      type: 'value',
      name: '上传数量',
    },
    series: [
      {
        data: counts,
        type: 'line',
        name: '上传数量',
        smooth: true, // 平滑折线
        emphasis: {
          focus: 'series',
        }
      },
    ],
  }
})

// 编写提交表达的函数，点击搜索时更改userId的值
const doSearch = (value : string) => {
  // 当点击搜索才会更改userId的值，此时就会出发重新搜索
  userId.value = value
}
</script>

<style scoped></style>
