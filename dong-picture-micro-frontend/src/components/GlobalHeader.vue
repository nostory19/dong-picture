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
import { computed, h, ref, watchEffect } from 'vue'
import {
  HomeOutlined,
  PlusCircleOutlined,
  AppstoreOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogOutUsingPost } from '@/api/userController.ts'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const loginUserStore = useLoginUserStore()

// 团队空间列表
const teamSpaceList = ref<API.SpaceUserVO[]>([])

const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  } else {
    message.error('加载我的团队空间失败，' + res.data.message)
  }
}

watchEffect(() => {
  if (loginUserStore.loginUser.id) {
    fetchTeamSpaceList()
  }
})

/**
 * 没有权限控制的时候传入固定的菜单栏
 * 有权限控制后需要根据用户权限对菜单栏进行过滤
 */
// 下面是未经过滤的原始菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: 'space',
    icon: () => h(AppstoreOutlined),
    label: '空间',
    title: '空间',
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
  const filtered = filterMenus(originItems)
  // 动态注入空间子菜单
  const spaceItem = filtered.find(item => item?.key === 'space')
  if (spaceItem) {
    const staticChildren = [
      { key: '/my_space', label: '我的空间' },
      { key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM, label: '创建团队' },
    ]
    if (teamSpaceList.value.length > 0) {
      const teamSpaceChildren = teamSpaceList.value.map((spaceUser) => ({
        key: '/space/' + spaceUser.spaceId,
        label: spaceUser.space?.spaceName,
      }))
      spaceItem.children = [
        ...staticChildren,
        { type: 'group', label: '团队空间', key: 'teamSpaceGroup', children: teamSpaceChildren },
      ]
    } else {
      spaceItem.children = staticChildren
    }
  }
  return filtered
})

// 路由跳转事件
const router = useRouter()

const current = ref<string[]>([])
// 设计一个钩子函数
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 这里的key和上面的key路由对应
const doMenuClick = ({ key }) => {
  router.push(key)
}



// 退出登录
const doLogout = () => {
  const token = localStorage.getItem('authToken')
  if (token) {
    localStorage.removeItem('authToken')
    message.success('退出登录成功')
    location.reload()
  } else {
    message.error('未登录')
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
