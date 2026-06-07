import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'
import dashboardRoutes from './modules/dashboard'
import positionRoutes from './modules/position'
import talentRoutes from './modules/talent'
import screeningRoutes from './modules/screening'
import interviewRoutes from './modules/interview'
import hiringRoutes from './modules/hiring'
import onboardRoutes from './modules/onboard'
import aiToolsRoutes from './modules/ai-tools'
import analyticsRoutes from './modules/analytics'
import settingsRoutes from './modules/settings'
import platformRoutes from './modules/platform'

const LoginRoute: RouteRecordRaw = {
  path: '/login',
  name: 'Login',
  component: () => import('@/views/login/Login.vue'),
  meta: { title: '登录', hidden: true },
}

const RootRoute: RouteRecordRaw = {
  path: '/',
  redirect: '/dashboard',
}

const routes: RouteRecordRaw[] = [
  LoginRoute,
  RootRoute,
  ...dashboardRoutes,
  ...positionRoutes,
  ...talentRoutes,
  ...screeningRoutes,
  ...interviewRoutes,
  ...hiringRoutes,
  ...onboardRoutes,
  ...aiToolsRoutes,
  ...analyticsRoutes,
  ...settingsRoutes,
  ...platformRoutes,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = getToken()
  const isPlatformAdmin = localStorage.getItem('isPlatformAdmin') === 'true'

  if (to.path === '/login') {
    if (token) {
      next(isPlatformAdmin ? '/platform/tenants' : '/dashboard')
    } else {
      next()
    }
  } else {
    if (token) {
      // Platform admin routes require platform admin login
      if (to.path.startsWith('/platform') && !isPlatformAdmin) {
        next('/dashboard')
        return
      }
      // Tenant routes not accessible by platform admin
      if (!to.path.startsWith('/platform') && isPlatformAdmin) {
        next('/platform/tenants')
        return
      }
      next()
    } else {
      next('/login')
    }
  }
})

// 设置页面标题
router.afterEach((to) => {
  const title = to.meta?.title ? `${to.meta.title} - RecruitOS` : 'RecruitOS'
  document.title = title as string
})

export default router
