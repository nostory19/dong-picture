import router from '@/router'
import {useLoginUserStore} from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'

// 是否为首次获取登录用户信息
let firstFetchLoginUser = true;

/**
 * 全局权限校验，每次切换页面时都会执行
 * 注意在main.ts中引入access.ts
 * 另外在GlobalHeader中，假设用户未登录也不能让用户看到导航栏
 */
router.beforeEach(async (to, from, next) => {
  // 先拿到当前登录用户的值
  const loginUserStore = useLoginUserStore();
  // 拿到当前登录用户
  let loginUser = loginUserStore.loginUser;
  // 确保页面刷新时，首次加载时，能够等待后端返回用户信息后再校验权限
  if (firstFetchLoginUser){
    // 检查 JWT token 是否存在，有 token 才去获取用户信息
    const token = localStorage.getItem('authToken')
    if (token) {
      // 获取当前登录用户信息
      await loginUserStore.fetchLoginUser();
      // 再赋值
      loginUser = loginUserStore.loginUser;
    }
    firstFetchLoginUser = false; // 这样在vue中再切换页面就不会重复获取用户信息了
  }
  // 拿到路径 fullPath是路由的一个属性，表示完整的URL路径
  const toUrl = to.fullPath
  // 可以自己定义权限校验规则，比如管理员才能访问 /admin页面
  if (toUrl.startsWith("/admin")){
    // 就可以判断登录用户的权限是不是管理员
    if (!loginUser || loginUser.userRole != 'admin'){
      // 报错
      message.error("没有权限")
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  // 反之则不是管理员，放行
  next();
})
