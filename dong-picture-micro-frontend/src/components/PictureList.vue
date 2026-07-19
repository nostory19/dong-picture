<template>
  <div class="picture-list">
<!--    图片列表-->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <!--      获取到列表中的每个元素，使用item:picture，赋值给picture-->
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <a-card hoverable @click="doClickPicture(picture)">
            <template #cover>
              <img
                :alt="picture.name"
                :src="picture.thumbnailUrl ?? picture.url"
                style="height: 180px; object-fit: cover"
              />
              <!--            使用object-fit让图片组件自适应宽高-->
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <!--                  将分类和标签统一标识-->
                  <a-tag color="green">
                    {{ picture.category ?? '模板' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:8px">
              <ThumbButton
                :picture-id="picture.id"
                :count="picture.thumbCount || 0"
                :has-thumb="picture.hasThumb || false"
              />
            </div>
            <template v-if="showOp" #actions>
<!--              <search-outlined @click="(e) => doSearch(picture, e)" />-->
              <!--              增加出发分享的入口-->
              <share-alt-outlined @click="(e) => doShare(picture, e)" />
<!--              注意传递一个点击的事件对象e-->
              <search-outlined @click="(e) => doSearch(picture, e)" />
              <edit-outlined v-if="canEdit" @click="(e) => doEdit(picture, e)" />
              <delete-outlined v-if="canDelete" @click="(e) => doDelete(picture, e)" />
<!--              <a-space @click="(e) => doEdit(picture, e)">-->
<!--                <edit-outlined/>-->
<!--                编辑-->
<!--              </a-space>-->
<!--              <a-space @click="e => doDelete(picture, e)">-->
<!--                <delete-outlined/>-->
<!--                删除-->
<!--              </a-space>-->
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink"/>
  </div>
</template>

<script setup lang="ts">
import {ref } from 'vue'
import ThumbButton from '@/components/ThumbButton.vue'
import { useRouter } from 'vue-router'
import { EditOutlined, DeleteOutlined, ShareAltOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
// showOp是否展示操作
interface Props {
  dataList?: API.PictureVO[],
  loading?: boolean,
  showOp?: boolean,
  onReload?: () => void,
  canEdit?: boolean,
  canDelete?: boolean,
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})


const router = useRouter()

// 点击图片跳转到图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 编辑事件，接收两个参数，
const doEdit = (picture, e) => {
  // 防止事件冒泡
  e.stopPropagation()
  router.push({
    path: "/add_picture",
    query: {
      id:  picture.id,
      spaceId: picture.spaceId,
    }
  })

}

// 删除
const doDelete = async (picture, e) => {
  e.stopPropagation()
//   获取id
  const id = picture.id
  if (!id){
    return
  }
//   执行接口
  const res = await deletePictureUsingPost({id})
  if (res.data.code === 0){
    message.success('删除成功')
    props.onReload?.()
    // router.push('/')
  }
  else {
    message.error('删除失败' + res.data.message)
  }
}


// 搜索
const doSearch = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}
const shareModalRef = ref()

// 分享连接
const shareLink = ref<string>()

// 分享
const doShare  = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  // console.log("测试打开分享")
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value){
    // 展示弹窗
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>

</style>
