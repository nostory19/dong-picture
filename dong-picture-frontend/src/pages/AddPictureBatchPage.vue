<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { onMounted, reactive, ref } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet, uploadPictureByBatchUsingPost
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'

// 批量抓取上传任务时间比较长，
const loading = ref(false)
const uploadType = ref<'file' | 'url'>('file')
// 引入路由跳转组件
const router = useRouter()

// 指定对象接收表单
const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})
// 由于上传图片后，能够返回图片解析的信息，因此可以将这些信息回填到表单当中
// 如何实现呢，即上传成功后，将newPicture赋值给picture，然后formData中的值会自动更新


const handleSubmit = async (values: any) => {
  loading.value = true

  try {
    const res = await uploadPictureByBatchUsingPost({
      ...formData,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success(`创建成功，共${res.data.data}条`)
      // 跳转到主页
      router.push({
        path: `/`,
      })
    } else {
      message.error('创建失败, ' + res.data.message)
    }
  }catch(e){
    message.error('创建失败, ' + e)
  }
  loading.value = false

}

</script>

<template>
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px">批量创建图片</h2>

    <!--    图片信息表单-->
    <!--    一定要注意表单项，需要填写name，才能正确接收到填写的参数到表单里-->
    <!--    只有图片存在的时候再展示表单-->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="searchText" label="关键词">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear />
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <!--       使用多行文本框编辑-->
        <a-input-number
          v-model:value="formData.count"
          placeholder="请输入批量抓取数量"
          allow-clear
          :min="1"
          :max="30"
          style="min-width: 180px"
        />
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <!--        使用既能提供选项，又能输入的组件-->
        <a-input
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀(自动补充序号)"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%"
        :loading="loading"> 执行任务 </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
