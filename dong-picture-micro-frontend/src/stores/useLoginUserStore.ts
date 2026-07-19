import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUserUsingGet } from '@/api/userController.ts'

/**
 * 存储登录用户信息的状态store
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  async function fetchLoginUser() {
    // 检查是否有 JWT token，没有则无需发请求
    const token = localStorage.getItem('authToken')
    if (!token) {
      // 用户未登录，无需调用后端接口
      return
    }
    // 后端还没有接口，后面完善获取登录用户
    // 现在后端接口已经写好，直接调用后端接口
    const res = await getLoginUserUsingGet()
    // 判断正常响应并且状态码为0
    if (res.data.code == 0 && res.data.data) {
      loginUser.value = res.data.data
    }
    // 测试3s后用户登录
    // setTimeout(() =>
    // {
    //   loginUser.value = {
    //     userName: "测试用户",
    //     id: 10,
    //   }
    // }, 3000)
  }
  function setLoginUser(user: any) {
    loginUser.value = user
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
