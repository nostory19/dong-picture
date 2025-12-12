<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import {
  addSpaceUsingPost,
  editSpaceUsingPost,
  getSpaceVoByIdUsingGet, listSpaceLevelUsingGet, updateSpaceUsingPost
} from '@/api/spaceController.ts'
import { useRoute, useRouter } from 'vue-router'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_OPTIONS, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space.ts'
import { formatSize } from '../utils'

// 引入路由跳转组件
const router = useRouter()
// 使用route
const route = useRoute()
// 判断空间类别
const spaceType = computed(() => {
  if (route.query?.type){
    return Number(route.query?.type)
  }else {
    // 是私有空间
    return SPACE_TYPE_ENUM.PRIVATE
  }
})
// 接收上传的空间
const space = ref<API.SpaceVO>()
// 指定对象接收表单
const spaceForm = reactive<API.SpaceAddRequest | API.SpaceEditRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
})
// 由于上传空间后，能够返回空间解析的信息，因此可以将这些信息回填到表单当中
// 如何实现呢，即上传成功后，将newSpace赋值给space，然后spaceForm中的值会自动更新

const loading = ref(false)

const spaceLevelList = ref<API.SpaceLevel[]>([])
// 获取spaceLevelList
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别失败，' + res.data.message)
  }
}


// 获取老数据
const getOldSpace = async () => {
  // 获取id
  const id = route.query?.id
  if (id) {
    // 获取实例
    const res = await getSpaceVoByIdUsingGet({
      id: id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      // 填写表单项
      spaceForm.spaceName = data.spaceName
      spaceForm.spaceLevel = data.spaceLevel
    }
  }
}

const handleSubmit = async (values: any) => {
  // 获取到上传空间的id
  // 根据spaceId判断是创建还是修改
  const spaceId = space.value?.id
  loading.value = true // 点击后开始加载
  let res
  if (spaceId) {
    // 更新操作
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...spaceForm
    })
  } else {
    // 创建操作
    res = await addSpaceUsingPost({
      ...spaceForm,
      spaceType: spaceType.value
    })
  }
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    // 跳转到空间详情页面
    router.push({
      path: `/space/${res.data.data}`
    })
  } else {
    message.error('操作失败, ' + res.data.message)
  }

  // 操作完成
  loading.value = false
}


onMounted(() => {
  getOldSpace()
  fetchSpaceLevelList()
})


</script>

<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改' : '创建' }}{{SPACE_TYPE_MAP[spaceType]}}
    </h2>

    <!--    空间信息表单-->
    <!--    一定要注意表单项，需要填写name，才能正确接收到填写的参数到表单里-->
    <!--    只有空间存在的时候再展示表单-->
    <a-form name="spaceForm" layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="名称">
        <a-input v-model:value="spaceForm.spaceName" placeholder="请输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>

      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading" style="width: 100%">
          提交
        </a-button>
      </a-form-item>
    </a-form>
    <!--    空间级别的介绍-->
    <a-card title="空间级别介绍" style="margin-top: 16px">
      <!--    对数组进行遍历展示-->
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系
        <a href="www.baidu.com" target="_blank">dd</a>
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}: 大小 {{ formatSize(spaceLevel.maxSize) }}， 数量
        {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
