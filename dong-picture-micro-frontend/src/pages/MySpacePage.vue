<template>
  <div id="mySpacePage">
   加载中
  </div>
</template>
<script setup lang="ts">
// 这里的mySpacePage相当于是一个中间过渡，根据用户的登录状态，跳转到对应页面

// 获取路由信息
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { onMounted } from 'vue'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const router = useRouter();
const loginUserStore = useLoginUserStore();

const checkUserSpace = async () => {
  // 先判断用户是否登录
  const loginUser = loginUserStore.loginUser;
  if (!loginUser?.id){
    // 未登录，跳转到登录页
    router.replace("/user/login");
    return;
  }
  // 登录了，跳转到用户空间页
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    current: 1,
    pageSize: 1,
    spaceType: SPACE_TYPE_ENUM.PRIVATE
  })
  // 判断是否拿到了数据
  if (res.data.code === 0){
    if (res.data.data?.records?.length > 0){
      const space = res.data.data.records[0];
      router.replace(`/space/${space.id}`);
    }else{
      router.replace("/add_space")
      message.warning("请先创建空间")
    }
  }else{
    message.error("加载我的空间失败, " + res.data.message)
  }


}

// 在页面加载时检查用户空间
onMounted(() => {
  checkUserSpace();
})
</script>
<style scoped>

</style>
