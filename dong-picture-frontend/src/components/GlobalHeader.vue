<template>
  <div id="global-header">
    <a-row :wrap="false">
      <a-col flex="150px">
        <!--   将导航栏分为左边的logo和标题，中间的导航条，右边的登录按钮那些-->
        <router-link to="/">
          <div class="title-bar">
            <img src="../assets/logo.jpg" alt="logo" class="logo" />
            <div class="title">云图库</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <!--        绑定一个点击事件-->
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="120px">
        <!--    登录按钮-->
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <!--           并添加头像-->
                <a-avatar :src="loginUserStore.loginUser.userAvatar" style="margin-right: 8px" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>

              <!--            添加插槽-->
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined/>
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登陆
                  </a-menu-item>

                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" ghost href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import {
  HomeOutlined,
  PlusCircleOutlined,
  MailOutlined,
  AppstoreOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogOutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

/**
 * 没有权限控制的时候传入固定的菜单栏
 * 有权限控制后需要根据用户权限对菜单栏进行过滤
 */
// const items = ref<MenuProps['items']>()
// 下面是未经过滤的原始菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片'
  },
  {
    key: '/admin/userManage',
    icon: () => h(PlusCircleOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    icon: () => h(PlusCircleOutlined),
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    icon: () => h(PlusCircleOutlined),
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.baidu.com', target: '_blank' }, 'Others'),
    title: '编程导航',
  },
]
// 过滤菜单项
const filterMenus = (menus = [] as  MenuProps['items']) => {
  return menus?.filter(((menu) => {
    // 如果是管理员才能访问的页面
    // 即key是/admin开头的就是管理员看到的
    if (menu?.key.startsWith("/admin")){
      // 拿到当前登录用户
      const loginUser=  loginUserStore.loginUser;
      if (!loginUser || loginUser.userRole !== 'admin'){
        return false;
      }
    }
    return true
  }))
}
// 展示在菜单的路由数组
const items = computed(() => {
  return filterMenus(originItems);
})

// 路由跳转事件
const router = useRouter()

const current = ref<string[]>([])
// 设计一个钩子函数
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 这里的key和上面的key路由对应
const doMenuClick = ({ item, key, keyPath }) => {
  router.push({ path: key })
}




// 退出登录
const doLogout = async () => {
  const res = await userLogOutUsingPost()
  if (res.data.code == 0) {
    loginUserStore.setLoginUser({
      userName: '',
    }) // 登录态重置
    message.success('退出登录成功')
    await router.push({
      path: '/user/login',
    })
  } else {
    message.error('退出登录失败' + (res.data.message || ''))
  }
}
</script>

<style scoped>
#global-header .title-bar {
  display: flex;
  align-items: center;
}

.logo {
  height: 48px;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}
</style>
