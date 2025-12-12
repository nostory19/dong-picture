<template>
  <div id="globalSider">
<!--    全局侧边栏组件-->
    <a-layout-sider v-if="loginUserStore.loginUser.id"  width="200"
    breakpoint="lg" collapsed-width="0">
      <a-menu
        model="inline"
        v-model:selectedKeys="current"
        :items="menuItems"
        @click="doMenuClick"/>

    </a-layout-sider>
  </div>
</template>
<script lang="ts" setup>
  // 菜单列表
  import { useRouter } from 'vue-router'
  import { computed, h, ref, watchEffect } from 'vue'
  import { PictureOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
  import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
  import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
  import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'
  import { message } from 'ant-design-vue'

  const loginUserStore = useLoginUserStore()
  // 固定菜单列表
  const fixedMenuItems = [
    {
      key: '/',
      label: '公共图库',
      icon: () => h(PictureOutlined),
    },
    {
      key: '/my_space',
      label: '我的空间',
      icon: () => h(UserOutlined),
    },
    {
      key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM,
      label: '创建团队',
      icon: () => h(TeamOutlined),
    }

  ]
  // 下面部分实现菜单栏高亮
  const router = useRouter()

  // 当前选中的菜单
  const current = ref<string[]>([])
  // 监听路由变化，更新当前选中菜单，更改当前高亮的菜单值
  router.afterEach((to, from, failure) => {
    current.value = [to.path]
  })

  // 路由跳转
  const doMenuClick = ({key} : {key: string}) => {
    // router.push({
    //   path: key,
    // })
    // 修改为直接传key
    router.push(key)
  }

  // 获取我的团队空间列表
  const teamSpaceList = ref<API.SpaceUserVO[]>([])
  const menuItems = computed(() => {
    // 动态确定团队空间
    // 没有团队空间，只展示固定菜单
    if (teamSpaceList.value.length < 1) {
      return fixedMenuItems;
    }
    // 展示团队空间分组
    const teamSpaceSubMenus = teamSpaceList.value.map((spaceUser) => {
      const space = spaceUser.space
      return {
        key: '/space/' + spaceUser.spaceId,
        label: space?.spaceName,
      }
    })
    const teamSpaceMenuGroup = {
      type: 'group',
      label: '我的团队',
      key: 'teamSpace',
      children: teamSpaceSubMenus,
    }
    return [...fixedMenuItems, teamSpaceMenuGroup]
  })

  // 加载团队空间列表
  const fetchTeamSpaceList = async () => {
    const res = await listMyTeamSpaceUsingPost()
    if (res.data.code === 0 && res.data.data) {
      teamSpaceList.value = res.data.data
    }else {
      message.error('加载我的团队空间失败，' + res.data.message)
    }
  }

  /*
  监听变量，改变时触发数据的重新加载
   */

  watchEffect(() => {
    if (loginUserStore.loginUser.id) {
      fetchTeamSpaceList()
    }
  })

</script>

<style scoped>
#globalSider .ant-layout-sider {
  background: none;
}
</style>
