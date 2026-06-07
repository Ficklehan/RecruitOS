import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const settingsRoutes: RouteRecordRaw[] = [
  {
    path: '/settings',
    component: AppLayout,
    redirect: '/settings/tenant',
    meta: { title: '系统设置', icon: 'Setting' },
    children: [
      {
        path: 'tenant',
        name: 'SettingsTenant',
        component: () => import('@/views/settings/TenantSetting.vue'),
        meta: { title: '租户设置', icon: 'HomeFilled' },
      },
      {
        path: 'org',
        name: 'SettingsOrg',
        component: () => import('@/views/settings/OrgManage.vue'),
        meta: { title: '组织架构', icon: 'Share' },
      },
      {
        path: 'role',
        name: 'SettingsRole',
        component: () => import('@/views/settings/RoleManage.vue'),
        meta: { title: '角色管理', icon: 'Lock' },
      },
      {
        path: 'user',
        name: 'SettingsUser',
        component: () => import('@/views/settings/UserManage.vue'),
        meta: { title: '用户管理', icon: 'UserFilled' },
      },
      {
        path: 'sso',
        name: 'SettingsSso',
        component: () => import('@/views/settings/SsoConfig.vue'),
        meta: { title: 'SSO配置', icon: 'Key' },
      },
      {
        path: 'license',
        name: 'SettingsLicense',
        component: () => import('@/views/settings/LicenseInfo.vue'),
        meta: { title: '许可信息', icon: 'Stamp' },
      },
    ],
  },
]

export default settingsRoutes
