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
        meta: { title: '租户设置', permission: 'settings:tenant', icon: 'HomeFilled' },
      },
      {
        path: 'org',
        name: 'SettingsOrg',
        component: () => import('@/views/settings/OrgManage.vue'),
        meta: { title: '组织架构', permission: 'settings:org', icon: 'Share' },
      },
      {
        path: 'role',
        name: 'SettingsRole',
        component: () => import('@/views/settings/RoleManage.vue'),
        meta: { title: '角色管理', permission: 'settings:role', icon: 'Lock' },
      },
      {
        path: 'user',
        name: 'SettingsUser',
        component: () => import('@/views/settings/UserManage.vue'),
        meta: { title: '用户管理', permission: 'settings:user', icon: 'UserFilled' },
      },
      {
        path: 'ai',
        name: 'SettingsAI',
        component: () => import('@/views/settings/AISettings.vue'),
        meta: { title: 'AI 设置', permission: 'settings:tenant', icon: 'Cpu' },
      },
      {
        path: 'integration/sso',
        name: 'SettingsSso',
        component: () => import('@/views/settings/SsoConfig.vue'),
        meta: { title: 'SSO配置', permission: 'settings:sso', icon: 'Key' },
      },
      {
        path: 'compliance/license',
        name: 'SettingsLicense',
        component: () => import('@/views/settings/LicenseInfo.vue'),
        meta: { title: '许可配额', permission: 'settings:license', icon: 'Stamp' },
      },
      {
        path: 'ai',
        name: 'SettingsAi',
        component: () => import('@/views/settings/AISettings.vue'),
        meta: { title: 'AI 设置', permission: 'settings:tenant', icon: 'Brain' },
      },
      {
        path: 'compliance/safety',
        name: 'SettingsSafety',
        component: () => import('@/views/communication/CommunicationSafety.vue'),
        meta: { title: '对话安全', permission: 'settings:safety', icon: 'Warning' },
      },
      {
        path: 'compliance/audit',
        name: 'SettingsAudit',
        component: () => import('@/views/settings/AuditLog.vue'),
        meta: { title: '审计日志', permission: 'settings:audit', icon: 'List' },
      },
      {
        path: 'compliance/gdpr',
        name: 'SettingsGdpr',
        component: () => import('@/views/settings/GdprDeletion.vue'),
        meta: { title: 'GDPR 数据删除', permission: 'settings:gdpr', icon: 'Delete' },
      },
      {
        path: 'integration',
        name: 'SettingsIntegration',
        component: () => import('@/views/settings/IntegrationFeishu.vue'),
        meta: { title: '集成管理', permission: 'settings:integration', icon: 'Connection' },
      },
      { path: 'sso', redirect: '/settings/integration/sso' },
      { path: 'license', redirect: '/settings/compliance/license' },
    ],
  },
]

export default settingsRoutes
