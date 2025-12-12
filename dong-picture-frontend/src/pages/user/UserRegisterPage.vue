<template>
  <div id="userRegisterPage">
    <!--    用户注册页面就是使用一个表单  -->
    <h2 class="title">云图库-用户注册</h2>
    <div class="desc">企业级智能系统云图库</div>
    <!--  添加表单-->
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item
        label="用户账号"
        name="userAccount"
        :rule="[{ required: true, message: '请输入账号' }]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
        <!--        指定输入框给哪个字段设置值-->
      </a-form-item>

      <a-form-item
        label="用户密码"
        name="userPassword"
        :rule="[
          { required: true, message: '请输入密码!' },
          { min: 8, message: '密码长度不能小于8' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <a-form-item
        label="确认密码"
        name="checkPassword"
        :rule="[
          { required: true, message: '请输入确认密码!' },
          { min: 8, message: '确认密码长度不能小于8' },
        //   再次校验密码是否一致
          { validator: (rule: any, value: string) => {
            if (!value || value === formState.userPassword) {
              return Promise.resolve()
            }
            return Promise.reject(new Error('两次输入的密码不一致'))
          } }
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>

      <!--      <a-form-item-->
      <!--        name="remember"-->
      <!--        :wrapper-col="{offset: 8, span: 16}"-->
      <!--       >-->
      <!--      <a-checkbox v-model:checked="formState.rember">Remember me</a-checkbox>-->
      <!--      </a-form-item>-->

      <!--      跳转到注册页-->
      <div class="tips">
        已有账号
        <router-link to="/user/login">立即登录</router-link>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { userLoginUsingPost, userRegisterUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import router from '@/router'

// 用于接收表单输入的值
const formState = reactive<API.UserRegistRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// 引入全局状态管理器
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: any) => {
  try {
    // 调用后端接口
    const res = await userRegisterUsingPost(values)
    // 判断是否注册成功
    // 注册成功不用保存状态，而是跳转到登录页
    if (res.data.code === 0 && res.data.data) {
      // 注册成功
      // 使用await 等待异步操作完成
      message.success('注册成功')
      // 跳转到首页
      router.push({
        path: '/user/login',
        replace: true, // 覆盖当前的注册页
      })
    } else {
      // 注册失败
      message.error('注册失败' + (res.data.message || ''))
    }
  } catch (error) {
    // console.error('注册请求出错:', error)
    // 统一提示注册失败
    message.error('注册失败，请稍后重试' + error.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbbbbb;
  margin-bottom: 16px;
}

.tips {
  text-align: right;
  font-size: 13px;
  color: #bbbbbb;
  margin-bottom: 16px;
}
</style>
