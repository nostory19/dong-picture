<template>
<!--缩放，旋转的操作-->
  <a-modal class="image-cropper" v-model:visible="visible"
           title="编辑图片" :footer="false" @cancel="closeModal" >
    <vue-cropper
      ref="cropperRef"
      :img="imageUrl"
      :auto-crop="true"
      :fixed-box="false"
      :center-box="true"
      :can-move-box="true"
      :info="true"
      output-type="png"
    />
    <div style="margin-bottom: 16px" />
<!--    图片协同编辑操作-->
    <div class="image-edit-actions" v-if="isTeamSpace">
      <a-space>
        <!--        使用ant design组件的按钮-->
        <a-button v-if="editingUser" disabled>{{editingUser.userName}}正在编辑</a-button>
        <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
        <a-button v-if="canExitEdit" danger ghost @click="exitEdit">退出编辑</a-button>
      </a-space>
    </div>
    <div style="margin-bottom: 16px" />

    <!--    图片操作-->
    <div class="image-cropper-actions">
      <a-space>
        <!--        使用ant design组件的按钮-->
<!--        按钮需要计算是否具有该能力-->
        <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
        <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
        <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
        <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm" :disabled="!canEdit">确认</a-button>
      </a-space>
    </div>
  </a-modal>
<!--  <div class="image-cropper">-->
<!--    -->
<!--  </div>-->
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watchEffect } from 'vue'
import 'vue-cropper/next/dist/index.css'
import {VueCropper} from 'vue-cropper/next'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import PictureEditWebSocket from '@/utils/PictureEditWebSocket.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
// 定义属性
interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const loading = ref(false)

const props = defineProps<Props>()
// 是否为团队空间
const isTeamSpace = computed(() => {
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})
// 编辑器组件的使用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}


// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num : number) => {
  cropperRef.value.changeScale(num)
  if (num > 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  }else if (num < 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}


// 嵌套的弹窗组件
const visible = ref(false)

const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  // 关闭弹窗也要去断开websocket连接
  visible.value = false
  if(websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
}

// 暴露函数给父组件
defineExpose({
  openModal,
})

// 编写上传函数，点击确认后将blob数据转换为file对象

const handleConfirm = () => {
  // blob为已裁切的文件
  cropperRef.value.getCropBlob((blob: Blob) => {
    // 获取文件名
    const fileName = (props.picture?.name || 'image') + '.png'
    // 转换为file对象
    const file = new File([blob], fileName, {type: blob.type})
    // 上传图片
    handleUpload({file})
  })
}


const handleUpload = async ({file}: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? {id: props.picture.id} : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    }else {
      message.error('图片上传失败, ' + res.data.message)
    }
  }catch (error){
    message.error('图片上传失败')
  }finally {
    loading.value = false
  }
}

// -----------------实时编辑----------------
// 获取当前登录用户
const loginUserStore = useLoginUserStore()
let loginUser = loginUserStore.loginUser;
console.log("登录用户", loginUser)
// 正在编辑的用户
const editingUser = ref<API.UserVO>();
console.log("正在编辑的用户", editingUser.value)
// 当前用户是否可进入编辑，通过计算是否有人在编辑
const canEnterEdit = computed(() => {
  return !editingUser.value
})
// 当前用户是否可以退出编辑，满足当前编辑用户是当前登录用户
const canExitEdit = computed(() => {
  return editingUser.value?.id === loginUser?.id
})
// 可以编辑，即可以点击编辑图片的按钮
// 只能是团队空间才能
const canEdit = computed(() => {
  // 非团队空间，任何用户都可以编辑
  if (!isTeamSpace.value) {
    return true;
  }
  // 团队空间，只有编辑用户是当前登录用户才能编辑
  return editingUser.value?.id === loginUser.id
})

// 编写websocket逻辑，团队空间才有
let websocket: PictureEditWebSocket | null

// 初始化websocket连接，绑定监听事件
const initWebsocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) {
    return
  }
  // 防止之前的连接未释放
  if (websocket) {
    websocket.disconnect()
  }
  // 创建websocket实例
  websocket = new PictureEditWebSocket(pictureId)
  // 建立连接
  websocket.connect()
  // open事件
  // websocket.on('open', (msg) => {
  //   console.log('连接成功', msg)
  //   // 同步初始编辑的用户
  //   if (msg.user) {
  //     editingUser.value = msg.user
  //   }
  // })
  // 绑定事件
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    console.log('收到通知：', msg)
    message.info(msg.message)
    console.log(editingUser.value)
  })
  //
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    console.log('错误通知：', msg)
    message.success(msg.message)
  })
  // 绑定进入编辑事件
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    console.log('进入编辑通知：', msg)

    message.info(msg.message)

    editingUser.value = msg.user
    console.log("进入编辑用户", msg.user, editingUser.value)
  })

  // 绑定退出编辑事件
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    console.log('退出编辑通知：', msg)

    message.info(msg.message)
    editingUser.value = undefined
  })

  // 编辑图片操作的处理
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    console.log('编辑操作通知：', msg)
    // message.info(msg.editAction)
    message.info(msg.message)
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        rotateLeft()
        break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        rotateRight()
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        changeScale(1)
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        changeScale(-1)
        break
    }

  })


}

watchEffect(() => {
  // 确保变量发生更改都会重新执行
  if (isTeamSpace.value) {
    initWebsocket()
  }
  // console.log(editingUser)
})

// onMounted是什么操作：是在组件挂载完成后执行的操作
// 组件销毁时，关闭websocket连接
onUnmounted(() => {
  if (websocket) {
    websocket.disconnect()
  }
  // 当前正在编辑的用户重置
  editingUser.value = undefined
})

const enterEdit = () => {
 if (websocket) {
   // 这里就发送进入编辑状态的请求
   websocket.sendMessage({
     type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT
   })
 }
}

const exitEdit = () => {
  if (websocket) {
    // 这里就发送退出编辑状态的请求
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT
    })
  }
}

// 编辑图片操作
const editAction = (action:string) => {
  if (websocket) {
    // 这里就发送编辑操作的请求
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: action,
    })
  }
}
</script>



<style>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}
</style>
