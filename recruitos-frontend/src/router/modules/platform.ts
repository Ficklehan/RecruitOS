import type { RouteRecordRaw } from 'vue-router'

const PlatformLayout = () => import('@/components/Layout/PlatformLayout.vue')

const platformRoutes: RouteRecordRaw[] = [
  {
    path: '/platform',
    component: PlatformLayout,
    redirect: '/platform/tenants',
    meta: { title: '平台管理', icon: 'Monitor', platformAdmin: true },
    children: [
      {
        path: 'tenants',
        name: 'PlatformTenants',
        component: () => import('@/views/platform/TenantList.vue'),
        meta: { title: '租户管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'tenants/:id',
        name: 'PlatformTenantDetail',
        component: () => import('@/views/platform/TenantDetail.vue'),
        meta: { title: '租户详情', hidden: true },
      },
    ],
  },
]

export default platformRoutes
