<template>
  <!--  页面唯一用id，不唯一用class-->
  <div id="basic-layout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader/>
      </a-layout-header>
<!--      Sider和Content应该是同级的-->
      <a-layout>
        <GlobalSider class="sider" />
        <a-layout-content class="content">
          <!--        内容是需要根据导航栏切换的，根据url找到页面文件，把对应页面内容替换-->
          <router-view />
        </a-layout-content>
      </a-layout>

      <a-layout-footer class="footer">
        <!--        写版权信息-->
        <a href="https://www.baidu.com" target="_blank"> 云图库 by dong </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>

<style scoped>
#basic-layout .header {
  padding-inline: 20px;
  background: white;
  color: unset;
  margin-bottom: 1px;

}

#basic-layout .sider {
  background: #fff;
  border-right: 0.5p solid #eee;
  padding-top: 20px;
}

#basic-layout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}
#basic-layout .content {
  padding: 28px;
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 30px;
}
#basic-layout .footer {
  background: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
}


</style>
