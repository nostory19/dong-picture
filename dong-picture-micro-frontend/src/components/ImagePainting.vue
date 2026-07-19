<template>
<!--缩放，旋转的操作-->
  <a-modal class="image-out-painting" v-model:visible="visible"
           title="AI扩图" :footer="false" @cancel="closeModal" >
<!--    开发弹窗的内容-->
    <a-row gutter="16">
<!--      栅格为24列-->
      <a-col span="12">
        <h4>原始图片</h4>
        <img
          :src="picture?.url"
          :alt="picture?.name"
          style="max-width: 100%" />
      </a-col>
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
        v-if="resultImageUrl"
        :src="resultImageUrl"
        :alt="picture?.name"
        style="max-width: 100%">
      </a-col>

    </a-row>
<!--    设置创建扩图，保存图片的两个按钮-->
    <div style="margin-bottom: 16px"/>
    <a-flex gap="16" justify="center">
      <a-button type="primary" goast  :loading="!!taskId" @click="createTask">生成图片</a-button>
      <a-button type="primary" v-if="resultImageUrl" :loading="uploadLoading" @click="handleUpload">应用结果</a-button>
    </a-flex>
  </a-modal>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import 'vue-cropper/next/dist/index.css'
import {VueCropper} from 'vue-cropper/next'
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskUsingGet, uploadPictureByUrlUsingPost,
  uploadPictureUsingPost
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
// 定义属性
interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}
const props = defineProps<Props>()

// 编辑器组件的使用
// const cropperRef = ref()



// 嵌套的弹窗组件
const visible = ref(false)

const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})


// 存储图片结果
const resultImageUrl = ref<string>()

// 创建任务函数
let taskId = ref<string>()

const loading = ref(false)
/**
 * 创建任务
 */
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  loading.value  = true
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    // 根据需要设置扩图参数
    parameters: {
      xScale: 2,
      yScale: 2,
    }
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    console.log("任务id: " + res.data.data.output.taskId)
    // 要在前端保存taskId，下一步根据taskId获取结果用的到
    taskId.value = res.data.data.output.taskId
    // 开启轮询
    startPolling()
  }else{
    message.error('创建任务失败, '+res.data.message)
  }
}

// 编写轮询，设置定时器，
let pollingTimer: NodeJS.Timeout = null

// 清理轮询
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    // 清空taskId
    taskId.value = null
  }
}

// 开启轮询
const startPolling = () => {
  // 判断是否为空
  if (!taskId.value) return
  // 捕获异常
  pollingTimer = setInterval(async () => {
    try {
      // 请求
      const res = await getPictureOutPaintingTaskUsingGet({
        taskId: taskId.value,
      })
      // 判断请求结果
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          // 查询成功
          message.success('扩图任务成功')
          // 返回扩图结果
          resultImageUrl.value = taskResult.outputImageUrl
        //   成功后也要清楚轮询
          clearPolling()
        }else if (taskResult.taskStatus === 'FAILED') {
          message.error('扩图任务失败')
          clearPolling()
        }
      }
    }catch (error){
      console.log('轮训任务状态查询失败', error)
      message.error('监测任务状态失败，请稍候重试')
      clearPolling()
    }
  }, 3000) // 事件，轮循时间3s一次
}

// 清理定时器，避免内存泄露
onUnmounted(() => {
  clearPolling()
})

 // 上传扩图图片
const uploadLoading = ref<boolean>(false)

const handleUpload = async() => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
      closeModal()
    }else {
      message.error('图片上传失败，' + res.data.message)
    }
  }catch (error) {
    message.error('图片上传失败')
  }finally {
    uploadLoading.value = false
  }
}
</script>



<style>
.image-out-painting {
  text-align: center;
}

</style>
