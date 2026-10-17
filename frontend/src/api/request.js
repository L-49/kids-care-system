import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动携带令牌
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('kids_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截：统一处理业务码与异常
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 200) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (err) => {
    const status = err.response && err.response.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('kids_token')
      localStorage.removeItem('kids_user')
      setTimeout(() => {
        window.location.hash = '#/login'
      }, 300)
    } else {
      ElMessage.error(
        (err.response && err.response.data && err.response.data.msg) ||
          '网络异常，请确认后端服务已启动（http://localhost:9090）'
      )
    }
    return Promise.reject(err)
  }
)

export default request
