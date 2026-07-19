<template>
  <div id="pictureDetailPage">
    <!--    图片详情页，一行两列，左边展示图片-->
<!--    图片浏览-->
    <a-row :gutter="[16, 16]">
      <!--      当屏幕很小的时候从两列变成一列竖直显示-->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览">
          <a-image :src="picture.url" style="max-height: 600px; object-fit: contain"/>
        </a-card>
      </a-col>
<!--      图片信息-->
      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息">
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.user?.userAvatar" />
                <div>{{picture.user?.userName}}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{picture.name ?? '未命名'}}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{picture.introduction ?? '-'}}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{picture.category ?? '默认'}}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{tag}}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{picture.picFormat ?? '-'}}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{picture.picWidth ?? '-'}}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{picture.picHeight ?? '-'}}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{picture.picScale ?? '-'}}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{formatSize(picture.picSize)}}
            </a-descriptions-item>
            <a-descriptions-item label="主色调">
<!--              展示色块-->
              <a-space>
                {{picture.picColor ?? '-' }}
                <div  v-if="picture.picColor"
                  :style="{
                  width: '16px',
                  height: '16px',
                  backgroundColor: toHexColor(picture.picColor),
                }"/>
              </a-space>
            </a-descriptions-item>
          </a-descriptions>
<!--          补充操作按钮-->
          <a-space wrap>
            <ThumbButton
              :picture-id="picture.id"
              :count="picture.thumbCount || 0"
              :has-thumb="picture.hasThumb || false"
            />
            <a-button type="primary" ghost @click="doShare" >
              分享
              <template #icon >
                <share-alt-outlined />
              </template>
            </a-button>
            <a-button v-if="canEdit" type="default" @click="doEdit">
                    编辑
              <template #icon>
                <EditOutlined />
              </template>
            </a-button>
<!--            增加判断是否具有删除权限-->
            <a-button v-if="canDelete" danger @click="doDelete">
              删除
              <template #icon>
                <DeleteOutlined />
              </template>
            </a-button>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <ShareModal ref="shareModalRef" :link="shareLink"/>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ShareModal from '@/components/ShareModal.vue'
import ThumbButton from '@/components/ThumbButton.vue'
import { EditOutlined, DeleteOutlined, ShareAltOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()

const route = useRoute()
const router = useRouter()
const picture = ref<API.PictureVO>({})

// 定义通用的权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    // 如果后端返回了权限列表，直接使用
    const permList = picture.value.permissionList ?? []
    if (permList.length > 0) {
      return permList.includes(permission)
    }
    // 兜底：仅图片所有者可编辑
    const loginUser = loginUserStore.loginUser
    if (!loginUser) return false
    return loginUser.id === picture.value.userId
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)


// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})


const loginUserStore = useLoginUserStore()
// 是否具有编辑权限
// const canEdit = computed(() => {
//   const loginUser = loginUserStore.loginUser;
// //   未登录不可编辑
//   if (!loginUser) {
//     return false
//   }
// //   仅本人或者管理员可编辑
//   const user = picture.value.user || {}
//   return loginUser.id === user.id || loginUser.userRole === 'admin'
//
// })

// 完成对应事件

// 编辑事件
const doEdit = () => {
  // router.push('/add_picture?id=' + picture.value.id)
  router.push({
    path: "/add_picture",
    query: {
      id:  picture.value.id,
      spaceId: picture.value.spaceId,
    }
  })
}

// 删除
const doDelete = async () => {
//   获取id
  const id = picture.value.id
  if (!id){
    return
  }
//   执行接口
  const res = await deletePictureUsingPost({id})
  if (res.data.code === 0){
    message.success('删除成功')
    // router.push('/')
  }
  else {
    message.error('删除失败' + res.data.message)
  }
}

// 处理下载
const doDownload = async () =>{
    try{
      await downloadImage(picture.value.url);
      message.success('下载成功')
    }catch (error){
      message.error('下载失败')
    }

}

// 分享操作
const shareModalRef = ref()

// 分享连接
const shareLink = ref<string>()

// 分享
const doShare  = () => {
  // console.log("测试打开分享")
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value.id}`
  if (shareModalRef.value){
    // 展示弹窗
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
#pictureDetailPage {
  margin-bottom: 16px
}
</style>
