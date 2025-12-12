<script setup lang="ts">
import 'vue-cropper/next/dist/index.css'
import { VueCropper } from 'vue-cropper/next'
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, h, onMounted, reactive, ref, watchEffect } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImagePainting from '@/components/ImagePainting.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

const uploadType = ref<'file' | 'url'>('file')
// 引入路由跳转组件
const router = useRouter()
// 使用route
const route = useRoute()
// 接收上传的图片
const picture = ref<API.PictureVO>()
// 指定对象接收表单
const pictureForm = reactive<API.PictureEditRequest>({})
// 由于上传图片后，能够返回图片解析的信息，因此可以将这些信息回填到表单当中
// 如何实现呢，即上传成功后，将newPicture赋值给picture，然后pictureForm中的值会自动更新

// 空间id，使用computed，当页面发生变化，值也会发生变化
const spaceId = computed(() => {
  return route.query?.spaceId
})

/*
图片上传成功
 */
const onsuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  // 将newPicture赋值给picture，然后pictureForm中的值会自动更新
  pictureForm.name = newPicture.name
}

const handleSubmit = async (values: any) => {
  // 获取到上传图片的id
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('图片创建成功')
    // 跳转到图片详情页面
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('图片创建失败, ' + res.data.message)
  }
}

// 定义分类选项、标签选项
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  // 获取成功，
  if (res.data.code === 0 && res.data.data) {
    // 展示的时候是有格式要求的
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      //   每个元素再作为一个新的对象返回
      return {
        value: data,
        label: data,
      }
    })
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表, ' + res.data.message)
  }
}

// 首次进入页面进行加载
onMounted(() => {
  getTagCategoryOptions()
})

// 获取老数据
const getOldPicture = async () => {
  // 获取id
  const id = route.query?.id
  if (id) {
    // 获取实例
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      // 填写表单项
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}
onMounted(() => {
  getOldPicture()
})

// 图片编辑弹窗引用
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = () => {
  if (imageCropperRef.value) {
    imageCropperRef.value.openModal()
  }
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// AI扩图功能
const imageOutPaintingRef = ref()

// 执行ai扩图，调用组件
const doImageOutPainting = () => {
  console.log("执行扩图")
  if (imageOutPaintingRef.value) {
    console.log('执行doImageOutPainting')
    imageOutPaintingRef.value.openModal()
  }
}

// 扩图成功事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// 获取空间信息
const space = ref<API.SpaceVO>()
const fetchSpace = async () => {
  // 获取数据
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({
      id: spaceId.value
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data

    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<template>
  <div id="addPicturePage">
    <!--    <ImageCropper imageUrl="https://th.bing.com/th/id/R.987f582c510be58755c4933cda68d525?rik=C0D21hJDYvXosw&riu=http%3a%2f%2fimg.pconline.com.cn%2fimages%2fupload%2fupc%2ftx%2fwallpaper%2f1305%2f16%2fc4%2f20990657_1368686545122.jpg&ehk=netN2qzcCVS4ALUQfDOwxAwFcy41oxC%2b0xTFvOYy5ds%3d&risl=&pid=ImgRaw&r=0" />-->

    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间： <a :href="`/space/${spaceId}`" target="_blank"> {{ spaceId }} </a>
    </a-typography-paragraph>
    <!--    图片上传组件-->
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <PictureUpload :picture="picture" :spaceId="spaceId" :onsuccess="onsuccess" />
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传" force-render>
        <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onsuccess="onsuccess" />
      </a-tab-pane>
    </a-tabs>
    <!--    补充一个编辑按钮，有图片才能编辑-->
    <div v-if="picture" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button type="primary" :icon="h(FullscreenOutlined)" ghost @click="doImageOutPainting"
          >AI扩图</a-button
        >
      </a-space>

      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="space"
        :onSuccess="onCropSuccess"
      />
      <ImagePainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />
    </div>

    <!--    图片信息表单-->
    <!--    一定要注意表单项，需要填写name，才能正确接收到填写的参数到表单里-->
    <!--    只有图片存在的时候再展示表单-->
    <a-form
      v-if="picture"
      name="pictureForm"
      layout="vertical"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" placeholder="请输入图片名称" allow-clear />
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <!--       使用多行文本框编辑-->
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入图片简介"
          allow-clear
          :auto-size="{ minRows: 2, maxRows: 5 }"
        />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <!--        使用既能提供选项，又能输入的组件-->
        <a-auto-complete
          v-model:value="pictureForm.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%"> 创建 </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}

#addPicturePage .edit-bar {
  text-align: center;
  margin: 16px 0;
}
</style>
