import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 注入租户 ID（平台管理员路由不注入）
    if (!config.url?.startsWith('/api/platform')) {
      const tenantId = localStorage.getItem('tenantId')
      if (tenantId) {
        config.headers['X-Tenant-Id'] = tenantId
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    // 如果响应直接是文件流
    if (response.config.responseType === 'blob') {
      return response
    }
    // 业务成功
    if (data.code === 0 || data.code === 200) {
      return data
    }
    // 业务错误
    const message = data.msg || data.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          removeToken()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          // 404 由调用方处理，不全局提示
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default service
