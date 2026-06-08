import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import { getDefaultRoute, hasPermission } from '@/config/menus'
import { useUserStore } from '@/stores/user'
import workspaceRoutes from './modules/workspace'
import pipelineRoutes from './modules/pipeline'
import planningRoutes from './modules/planning'
import talentRoutes from './modules/talent-new'
import insightRoutes from './modules/insight'
import settingsRoutes from './modules/settings'
import platformRoutes from './modules/platform'
import legacyRedirects from './modules/legacy-redirects'
import screeningRoutes from './modules/screening'

const LoginRoute: RouteRecordRaw = {
  path: '/login',
  name: 'Login',
  component: () => import('@/views/login/Login.vue'),
  meta: { title: '登录', hidden: true },
}

const RootRoute: RouteRecordRaw = {
  path: '/',
  redirect: () => {
    const userStore = useUserStore()
    const roleCodes = extractRoleCodes(userStore.roles)
    return getDefaultRoute(roleCodes)
  },
}

function extractRoleCodes(roles: any[]): string[] {
  if (!roles?.length) return []
  if (typeof roles[0] === 'string') return roles as string[]
  return roles.map((r: any) => r.roleCode || r.code || r).filter(Boolean)
}

const routes: RouteRecordRaw[] = [
  LoginRoute,
  RootRoute,
  ...workspaceRoutes,
  ...pipelineRoutes,
  ...screeningRoutes,
  ...planningRoutes,
  ...talentRoutes,
  ...insightRoutes,
  ...settingsRoutes,
  ...platformRoutes,
  ...legacyRedirects,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const token = getToken()
  const isPlatformAdmin = localStorage.getItem('isPlatformAdmin') === 'true'

  if (to.path === '/login') {
    const userStore = useUserStore()
    if (token && userStore.userInfo) {
      next(isPlatformAdmin ? '/platform/tenants' : '/')
    } else {
      if (token && !userStore.userInfo) {
        userStore.resetState()
      }
      next()
    }
    return
  }

  if (!token) {
    next('/login')
    return
  }

  if (to.path.startsWith('/platform') && !isPlatformAdmin) {
    next('/')
    return
  }
  if (!to.path.startsWith('/platform') && isPlatformAdmin) {
    next('/platform/tenants')
    return
  }

  const userStore = useUserStore()
  if (!userStore.userInfo && !isPlatformAdmin) {
    try {
      await userStore.getUserInfo()
    } catch {
      userStore.resetState()
      next('/login')
      return
    }
  }

  const perm = to.meta?.permission as string | undefined
  if (perm && !isPlatformAdmin && userStore.permissions.length) {
    if (!hasPermission(userStore.permissions, perm)) {
      next(getDefaultRoute(extractRoleCodes(userStore.roles)))
      return
    }
  }

  next()
})

router.afterEach((to) => {
  const title = to.meta?.title ? `${to.meta.title} - RecruitOS` : 'RecruitOS'
  document.title = title as string
})

export default router
