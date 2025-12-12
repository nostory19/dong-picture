// 全局请求文件

// 定义请求实例
import axios from 'axios'
import { message } from 'ant-design-vue'

const myAxios = axios.create({
  baseURL: 'http://localhost:8123',
  // baseURL: "", // 留空，默认请求的地址才是前端地址
  timeout: 60000,
  withCredentials: true,
})

// 添加请求拦截器
axios.interceptors.request.use(
  function (config) {
    // 在发送请求之前做些什么
    return config
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error)
  },
)

// 添加响应拦截器
axios.interceptors.response.use(
  function (response) {
    // 取出数据内容
    const { data } = response
    if (data.code === 40100) {
      // 用户未登录
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('user/login')
      ) {
        message.warning('请先登录')
        window.location.href = '/user/login?redirect=' + window.location.href
      }
      return response
    }
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么
    return response
  },
  function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error)
  },
)
export default myAxios
