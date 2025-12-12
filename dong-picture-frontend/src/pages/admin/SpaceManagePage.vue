<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between">
      <h2 style="margin-bottom: 16px">空间管理</h2>
      <a-space>
        <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
<!--      空间管理页面：  新增公共图库分析按钮，全空间分析按钮，并且可以直接跳转到某个特定的空间分析页-->

        <a-button type="primary" ghost href="/space_analyze?queryPublic=1" target="_blank">
          分析公共图库
        </a-button>
        <a-button type="primary" ghost href="/space_analyze?queryAll=1" target="_blank">
          分析全空间
        </a-button>
      </a-space>
    </a-flex>
<!--    搜索菜单-->
    <div style="margin-bottom: 16px"/>
    <a-form
      layout="inline"
      :model="searchParams"
      @finish="doSearch">
      <a-form-item
        label="空间名称"
        name="spaceName">
          <a-input
            v-model:value="searchParams.spaceName"
            placeholder="请输入空间名称"
            allow-clear/>
      </a-form-item>
      <a-form-item
        label="空间级别"
        name="spaceLevel">
       <a-select
       v-model:value="searchParams.spaceLevel"
       :options="SPACE_LEVEL_OPTIONS"
       placeholder="请输入空间级别"
       style="min-width: 180px"
       allow-clear/>
      </a-form-item>
      <a-form-item
        label="空间类别"
        name="spaceType">
        <a-select
          v-model:value="searchParams.spaceType"
          :options="SPACE_TYPE_OPTIONS"
          placeholder="请输入空间类别"
          style="min-width: 180px"
          allow-clear/>
      </a-form-item>
<!--      搜索标签-->
      <a-form-item
      label="用户 id"
      name="userId">
        <a-input
        v-model:value="searchParams.userId"
        allow-clear
        placeholder="请输入用户id"/>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">
          搜索
        </a-button>

      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px"/>
<!--    表格-->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
      :scroll="{ x: 'max-content' }">

      <template #bodyCell="{column, record}">
<!--        空间级别，显示文字映射-->
        <template v-if="column.dataIndex === 'spaceLevel'">
          <a-tag>{{SPACE_LEVEL_MAP[record.spaceLevel]}}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'spaceType'">
          <a-tag>{{SPACE_TYPE_MAP[record.spaceType]}}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'spaceUseInfo'">
          <div>大小：{{formatSize(record.totalSize)}} / {{formatSize(record.maxSize)}}</div>
          <div>数量：{{record.totalCount}} / {{record.maxCount}}</div>
        </template>

        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" :href="`/space_analyze?spaceId=${record.id}`" target="_blank">分析</a-button>
            <a-button type="link" :href="`/add_space?id=${record.id}`" target="_blank">
              编辑
            </a-button>
            <a-popconfirm
            title="确定删除吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="confirm"
            @cancel="cancel">
<!--              <a-button danger @click="doDelete(record.id)">-->
<!--                删除-->
<!--              </a-button>-->
<!--              使用了弹出框就需要进行事件修改-->
              <a-button danger @click="handleDelete(record.id)">
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>


</template>


<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import {message} from 'ant-design-vue'
import {
  deleteSpaceUsingPost,
  listSpaceByPageUsingPost
} from '@/api/spaceController.ts'
import dayjs from 'dayjs'
import { formatSize } from '../../utils'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS, SPACE_TYPE_MAP, SPACE_TYPE_OPTIONS } from '../../constants/space.ts'
import { BarChartOutlined } from '@ant-design/icons-vue'



// 设置列
// title为前端展示的名称，dataIndex为后端数据库中字段名
// 将所有解析出来的图片信息放在一个字段里，用picInfo代替
const columns  = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
  },
  {
    title: '空间类别',
    dataIndex: 'spaceType',
  },
  {
    title: '使用情况',
    dataIndex: 'spaceUseInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  }
]



// 数据
const dataList = ref<API.Space[]>([])
const total = ref(0)

// 删除方法
const doDelete = async (id : number) => {
  const res = await deleteSpaceUsingPost({id})
  console.log("删除结果res", res)
  if (res.data.code === 0){
    message.success("删除成功")
    // 刷新数据
    await fetchData()
  }
  else{
    // 删除失败
    message.error('删除失败')
  }
}

// 搜索条件
const searchParams = reactive<API.SpaceQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据函数
const fetchData = async () => {
  // 调用接口，给管理员用的接口
  const res = await listSpaceByPageUsingPost({
    ...searchParams,
  })
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  }
  else{
    message.error("获取数据失败, " + res.data.message)
  }
}

// 表格变化后重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 一定注意搜索的时候从第一页开始搜
  searchParams.current = 1
  fetchData()

}

// 分页参数
// 利用计算属性computed，接收一个渲染函数，pagination就会动态变化
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`
  }
})

// onMounted可以在页面加载时执行函数
onMounted(() => {
  fetchData()
})

const deleteId = ref(null)

const handleDelete = (id: number) => {
  deleteId.value = id
}
// 删除的确认
const confirm = (e: MouseEvent) => {
  console.log(e);
  message.success('点击确认');
  doDelete(deleteId.value)
};

const cancel = (e: MouseEvent) => {
  console.log(e);
  message.error('点击取消');
};
</script>

<style scoped>
</style>
