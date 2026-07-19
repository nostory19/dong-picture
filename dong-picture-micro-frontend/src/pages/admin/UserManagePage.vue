<template>
  <div id="userManagePage">
<!--    搜索菜单-->
    <a-form
      layout="inline"
      :model="searchParams"
      @finish="doSearch">
      <a-form-item
        label="账号">
          <a-input
            v-model:value="searchParams.userAccount"
            placeholder="输入账号"
            allow-clear/>
      </a-form-item>
      <a-form-item
        label="用户名">
        <a-input
          v-model:value="searchParams.userName"
          placeholder="输入用户名"
          allow-clear/>
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
      :pagination="false">
      <template #bodyCell="{column, record}">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="60"/>
        </template>

        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
<!--            使用标签代替角色-->
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>

        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-button danger @click="doDelete(record.id)">
            删除
          </a-button>
        </template>
      </template>
    </a-table>
    <div style="margin-top: 16px; text-align: right">
      <a-pagination
        :current="searchParams.current"
        :page-size="searchParams.pageSize"
        :total="total"
        show-size-changer
        :show-total="(total) => `共 ${total} 条`"
        @change="onPageChange"
      />
    </div>
  </div>


</template>


<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {message} from 'ant-design-vue'
import {deleteUserUsingPost, listUserVoByPageUsingPost} from '@/api/userController.ts'
import dayjs from 'dayjs'


// 设置列
// title为前端展示的名称，dataIndex为后端数据库中字段名
const columns  = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
  },
  {
    title: '操作',
    key: 'action',
  }
]

// 删除方法
const doDelete = async (id : number) => {
  const res = await deleteUserUsingPost({id})
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

// 数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取数据函数
const fetchData = async () => {
  // 调用接口
  const res = await listUserVoByPageUsingPost({
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

// 分页变化后重新获取数据
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 一定注意搜索的时候从第一页开始搜
  searchParams.current = 1
  fetchData()

}

// onMounted可以在页面加载时执行函数
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
</style>
