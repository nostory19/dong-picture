import axios from 'axios'
import { message } from 'ant-design-vue'

const DEV_BASE_URL = 'http://localhost:8200'
const PROD_BASE_URL = 'http://82.156.14.62'

const myAxios = axios.create({
  baseURL: DEV_BASE_URL,
  timeout: 30000,
  withCredentials: true,
})

myAxios.interceptors.request.use(config => {
  const token = localStorage.getItem('authToken')
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        localStorage.removeItem('authToken')
        message.warning('请先登录')
        window.location.href = '/user/login?redirect=' + window.location.href
      }
    }
    return response
  },
  function (error) {
    if (error.response && error.response.status === 401) {
      if (
        !error.response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        localStorage.removeItem('authToken')
        message.warning('请先登录')
        window.location.href = '/user/login?redirect=' + window.location.href
      }
    }
    return Promise.reject(error)
  },
)
export default myAxios
