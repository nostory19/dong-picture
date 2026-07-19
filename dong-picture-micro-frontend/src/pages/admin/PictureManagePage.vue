<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between">
      <h2 style="margin-bottom: 16px">图片管理</h2>
      <a-space>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost>+ 批量创建图片</a-button>
      </a-space>
    </a-flex>
<!--    搜索菜单-->
    <div style="margin-bottom: 16px"/>
    <a-form
      layout="inline"
      :model="searchParams"
      @finish="doSearch">
      <a-form-item
        label="关键词">
          <a-input
            v-model:value="searchParams.searchText"
            placeholder="从名称和简介搜索"
            allow-clear/>
      </a-form-item>
      <a-form-item
        label="类型">
        <a-input
          v-model:value="searchParams.category"
          placeholder="请输入类型"
          allow-clear/>
      </a-form-item>
<!--      搜索标签-->
      <a-form-item
      label="标签">
        <a-select
          v-model:value="searchParams.tags"
          mode="tags"
          placeholder="请输入标签"
          style="min-width: 180px"
          allow-clear/>
      </a-form-item>
      <a-form-item name="reviewStatus" label="审核状态">
        <a-select
          style="min-width: 180px"
          v-model:value="searchParams.reviewStatus"
          placeholder="请选择审核状态"
          :options="PIC_REVIEW_STATUS_OPTIONS"
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
      :pagination="false"
      :scroll="{ x: 'max-content' }">

      <template #bodyCell="{column, record}">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" :width="120"/>
        </template>
<!--        标签，循环遍历展示标签-->
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">
              {{tag}}
            </a-tag>
          </a-space>
        </template>
<!--        图片信息展示-->
        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式：{{record.picFormat}}</div>
          <div>宽度：{{record.picWidth}}</div>
          <div>高度：{{record.picHeight}}</div>
          <div>宽高比：{{record.picScale}}</div>
          <div>大小：{{(record.picSize / 1024).toFixed(2)}}KB</div>

        </template>
        <template v-if="column.dataIndex === 'reviewMessage'">
          <div>审核状态：{{PIC_REVIEW_STATUS_MAP[record.reviewStatus]}}</div>
          <div>审核信息：{{record.reviewMessage}}</div>
          <div>审核人：{{record.reviewerId}}</div>
          <div v-if="record.reviewTime">
            审核时间：{{dayjs(record.reviewTime).format('YYYY-MM-DD HH:mm:ss')}}
          </div>

        </template>
<!--        <template v-else-if="column.dataIndex === 'pictureRole'">-->
<!--          <div v-if="record.pictureRole === 'admin'">-->
<!--&lt;!&ndash;            使用标签代替角色&ndash;&gt;-->
<!--            <a-tag color="green">管理员</a-tag>-->
<!--          </div>-->
<!--          <div v-else>-->
<!--            <a-tag color="blue">普通图片</a-tag>-->
<!--          </div>-->
<!--        </template>-->

        <template v-if="column.dataIndex === 'createTime'">
          {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss')}}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button v-if="record.reviewStatus != PIC_REVIEW_STATUS_ENUM.PASS"
                      type="link"
                      @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)">
              通过
            </a-button>
            <a-button v-if="record.reviewStatus != PIC_REVIEW_STATUS_ENUM.REJECT"
                      type="link"
                      danger
                      @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)">
              拒绝
            </a-button>
            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">
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
import {
  deletePictureUsingPost, doPictureReviewUsingPost,
  listPictureByPageUsingPost
} from '@/api/pictureController.ts'
import dayjs from 'dayjs'
import { PIC_REVIEW_STATUS_ENUM, PIC_REVIEW_STATUS_MAP, PIC_REVIEW_STATUS_OPTIONS } from '../../constants/picture.ts'


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
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户id',
    dataIndex: 'useId',
    width: 80,
  },
  {
    title: '空间ID',
    dataIndex: 'spaceId',
    width: 80,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewMessage',
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

// 删除方法
const doDelete = async (id : number) => {
  const res = await deletePictureUsingPost({id})
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
const dataList = ref<API.Picture[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据函数
const fetchData = async () => {
  // 调用接口，只查公共图库的请求
  const res = await listPictureByPageUsingPost({
    ...searchParams,
    // nullSpaceId: true,
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

const handleReview = async (record: API.Picture, reviewStatus: number) => {
  // 补充审核信息
  const reviewMessage = reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员操作通过' : '管理员操作拒绝'

  // 调用接口
  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  // 成功，刷新数据
  if (res.data.code === 0){
    message.success('审核操作成功')
    // 重新获取
    fetchData()

  }else{
    message.error('审核操作失败, ' + res.data.message)
  }
}

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
