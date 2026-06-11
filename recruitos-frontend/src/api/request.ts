import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { toast } from '@/lib/notify'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 为 true 时不弹出全局错误提示 */
    silent?: boolean
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let lastErrorKey = ''
let lastErrorAt = 0
let redirectingToLogin = false

function showErrorOnce(key: string, message: string, silent?: boolean) {
  if (silent) return
  const now = Date.now()
  if (key === lastErrorKey && now - lastErrorAt < 3000) return
  lastErrorKey = key
  lastErrorAt = now
  toast.error(message)
}

function redirectToLogin(silent?: boolean) {
  removeToken()
  if (redirectingToLogin || router.currentRoute.value.path === '/login') return
  redirectingToLogin = true
  showErrorOnce('401', '登录已过期，请重新登录', silent)
  router.push('/login').finally(() => {
    redirectingToLogin = false
  })
}

function handleUnauthorized(silent?: boolean) {
  redirectToLogin(silent)
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (!config.url?.startsWith('/api/platform')) {
      const tenantId = localStorage.getItem('tenantId')
      if (tenantId) {
        config.headers['X-Tenant-Id'] = tenantId
      }
    }
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, config } = response
    if (response.config.responseType === 'blob') {
      return response
    }
    if (data.code === 0 || data.code === 200) {
      return data
    }
    if (data.code === 401) {
      handleUnauthorized(config.silent)
      return Promise.reject(new Error(data.msg || '请先登录'))
    }
    if (data.code === 403) {
      showErrorOnce('403', data.msg || '没有权限访问', config.silent)
      return Promise.reject(new Error(data.msg || '没有权限访问'))
    }
    const message = data.msg || data.message || '请求失败'
    showErrorOnce(`biz:${config.url}:${data.code}`, message, config.silent)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response, config } = error
    const silent = config?.silent
    if (response) {
      if (response.status === 401) {
        handleUnauthorized(silent)
      } else if (response.status === 403) {
        showErrorOnce('403', '没有权限访问', silent)
      } else if (response.status === 404) {
        // 404 由调用方处理
      } else if (response.status >= 500) {
        showErrorOnce(`http500:${config?.url}`, '服务器内部错误，请稍后重试', silent)
      } else {
        const message = response.data?.msg || response.data?.message || '请求失败'
        showErrorOnce(`http:${response.status}:${config?.url}`, message, silent)
      }
    } else {
      showErrorOnce('network', '网络连接异常，请检查后端服务是否已启动', silent)
    }
    return Promise.reject(error)
  }
)

export default service
