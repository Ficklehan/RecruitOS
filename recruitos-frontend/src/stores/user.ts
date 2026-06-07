import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, logoutApi, getCurrentUserApi } from '@/api/modules/auth'
import { getPlatformAdminInfo, platformLogoutApi } from '@/api/modules/platform'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

interface UserInfo {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  avatar: string
  tenantId: number
  tenantName: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  function setTokenValue(t: string) {
    token.value = t
    setToken(t)
  }

  async function login(loginData: { username: string; password: string; tenantId?: number }) {
    try {
      const { data } = await loginApi(loginData)
      token.value = data.token
      setToken(data.token)
      return data
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
      throw error
    }
  }

  async function getUserInfo() {
    try {
      if (localStorage.getItem('isPlatformAdmin') === 'true') {
        const { data } = await getPlatformAdminInfo()
        userInfo.value = { id: data.adminId, username: data.username, realName: data.realName } as any
        roles.value = ['PLATFORM_ADMIN']
        permissions.value = ['PLATFORM_ADMIN']
        return data
      }
      const { data } = await getCurrentUserApi()
      userInfo.value = data.user
      roles.value = data.roles || []
      permissions.value = data.permissions || []
      return data
    } catch (error: any) {
      throw error
    }
  }

  async function logout() {
    try {
      if (localStorage.getItem('isPlatformAdmin') === 'true') {
        await platformLogoutApi()
      } else {
        await logoutApi()
      }
    } catch (e) {
      // 忽略登出接口错误
    } finally {
      resetState()
    }
  }

  function resetState() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeToken()
    localStorage.removeItem('isPlatformAdmin')
    localStorage.removeItem('platformAdminName')
    localStorage.removeItem('tenantId')
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    setToken: setTokenValue,
    login,
    getUserInfo,
    logout,
    resetState,
  }
})
