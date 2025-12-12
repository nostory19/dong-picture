<template>
  <!-- 遵循开发规范，最外层指定一个唯一id-->
  <div id="app">
    <!--    引入布局-->
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import HelloWorld from './components/HelloWorld.vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
// import { healthUsingGet } from '@/api/mainController.ts'
// import {useLoginUserStore} from '@/stores/useLoginUserStore.ts'

// 下面获取用户信息部分，转换到了access.ts中权限校验中获取
// healthUsingGet().then((res) => {
//   console.log(res)
// })
//
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()

</script>

<style scoped>

</style>
