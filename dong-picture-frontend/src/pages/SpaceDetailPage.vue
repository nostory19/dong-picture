<template>
  <div id="spaceDetailPage">
    <!--    展示空间信息-->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}({{SPACE_TYPE_MAP[space.spaceType]}})</h2>
      <a-space size="middle">
        <!--        携带当前空间的id-->
        <a-button
          v-if="canUploadPicture"
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
          target="_blank"
        >
          + 创建图片
        </a-button>

        <!--        id代表空间的id-->
<!--        假设是私有空间则不应该有团队成员管理-->
        <a-button v-if="canManageSpaceUser && space.spaceType === SPACE_TYPE_ENUM.TEAM"
          type="primary" ghost :icon="h(TeamOutlined)" :href="`/spaceUserManage/${id}`" target="_blank">
          空间成员管理
        </a-button>
        <a-button v-if="canEditPicture" :icon="h(EditOutlined)" @click="doBatchEdit">批量编辑</a-button>
        <a-button v-if="canManageSpaceUser"
          type="primary" ghost :icon="h(BarChartOutlined)" :href="`/space_analyze?spaceId=${id}`" target="_blank"></a-button>
        <!--      使用进度条展示空间容量情况-->
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            type="circle"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
            :size="42"
          />
        </a-tooltip>
      </a-space>
    </a-flex>

    <div style="margin-bottom: 16px" />
    <!--    引入搜索表单-->
    <PicturteSearchForm :onSearch="onSearch" />
    <a-form-item label="按颜色搜索" name="picColor">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>
    <div style="margin-bottom: 16px" />
    <!--    使用可复用的-->
    <PictureList
      :dataList="dataList"
      :loading="loading"
      :showOp="true"
      :onReload="fetchData"
    :canEdit="canEditPicture"
    :canDelete="canDeletePicture"/>
    <!--    添加分页组件-->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
  <BatchEditPictureModal
  ref="batchEditPictureModalRef"
  :space-id="id"
  :picture-list="dataList"
  :on-success="onBatchEditSuccess"/>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import PicturteSearchForm from '@/components/PicturteSearchForm.vue'

import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { EditOutlined, BarChartOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '../constants/space.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()

const route = useRoute()
const router = useRouter()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
// 使用js闭包的特性
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)


// 获取图片详情
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取图片详情失败' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

// 数据
const dataList = ref<API.PictureVO[]>([])
// const dataList = ref([])
const total = ref(0)
const loading = ref(true)

// 标签和分类列表
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
// 已有选中的分类
const selectedCategory = ref<string>('all')
const selectedTagList = ref<string[]>([])

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据函数
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }
  // 调用接口
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
  loading.value = false
}

const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

// 自动加载
onMounted(() => {
  fetchData()
})

// const doSearch = () =>{
//   // 和其他页面的搜索类似，触发搜索的时候回到第一页
//   searchParams.current = 1
//   fetchData()
//
// }

const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchData()
}

const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data){
    dataList.value = res.data.data ?? []
    total.value = res.data.data.length
  }else {
    message.error("获取数据失败, " + res.data.message);
  }
  loading.value = false
}

// 批量编辑
const batchEditPictureModalRef = ref()
// 批量编辑成功后，刷新数据
const onBatchEditSuccess = () => {
  fetchData()
}

// 打开批量编辑弹窗按钮
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value){
    batchEditPictureModalRef.value.openModal()
  }
}

// 空间id改变时重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  margin-bottom: 16px;
}
</style>
