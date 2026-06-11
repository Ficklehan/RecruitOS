export interface MenuItem {
  key: string
  label: string
  path: string
  permission?: string
  icon?: string
  hidden?: boolean
  children?: MenuItem[]
}

// === AI-Native Menu v2 ===
// 按场景组织：AI助手 → 招人 → 人才网络 → 诊断中心 → 设置

export const topNavMenus: MenuItem[] = [
  // ── 第一入口：AI助手 ──
  {
    key: 'ai',
    label: 'AI 助手',
    path: '/ai',
    permission: 'workspace',
    children: [
      { key: 'ai-home', label: '首页', path: '/ai', permission: 'workspace:inbox', icon: 'Sparkles' },
      { key: 'ai-demand', label: '发起招聘需求', path: '/ai/demand/create', permission: 'planning:demand', icon: 'Plus' },
      { key: 'ai-diagnose', label: '诊断中心', path: '/ai/diagnose', permission: 'insight', icon: 'Search' },
      { key: 'ai-report', label: '周报', path: '/ai/report', permission: 'insight', icon: 'BarChart3' },
    ],
  },
  // ── 招人（按职位一站式工作台）──
  {
    key: 'hiring',
    label: '招人',
    path: '/planning/jobs',
    permission: 'planning',
    children: [
      { key: 'jobs', label: '在招职位', path: '/planning/jobs', permission: 'planning:job', icon: 'Briefcase' },
      { key: 'pipeline', label: '候选人管道', path: '/pipeline/board', permission: 'pipeline:board', icon: 'Grid' },
      { key: 'match', label: '匹配评估', path: '/pipeline/decision', permission: 'pipeline:candidate', icon: 'DataAnalysis', hidden: true },
      { key: 'calendar', label: '面试日历', path: '/pipeline/calendar', permission: 'pipeline:calendar', icon: 'Calendar' },
      { key: 'offers', label: '录用通知', path: '/pipeline/offers', permission: 'pipeline:offer', icon: 'Tickets' },
      { key: 'onboards', label: '入职', path: '/pipeline/onboards', permission: 'pipeline:onboard', icon: 'Finished' },
    ],
  },
  // ── 人才网络 ──
  {
    key: 'talent',
    label: '人才网络',
    path: '/talent/pool',
    permission: 'talent',
    children: [
      { key: 'pool', label: '人才库', path: '/talent/pool', permission: 'talent:pool', icon: 'Files' },
      { key: 'resumes', label: '简历收件', path: '/talent/resumes', permission: 'talent:resume', icon: 'Document' },
      { key: 'referral', label: '内推', path: '/talent/referral', permission: 'talent:referral', icon: 'Share' },
      { key: 'headhunter', label: '猎头', path: '/talent/headhunters', permission: 'talent:headhunter', icon: 'OfficeBuilding' },
      { key: 'channels', label: '渠道与账号', path: '/talent/channels', permission: 'talent:channel', icon: 'Share' },
      { key: 'comm-profile', label: '对外沟通风格', path: '/talent/communication-profile', permission: 'talent:template', icon: 'ChatDotRound' },
    ],
  },
  // ── 诊断中心（主动巡检，替代旧数据洞察）──
  {
    key: 'insight',
    label: '诊断中心',
    path: '/insight/funnel',
    permission: 'insight',
    children: [
      { key: 'health', label: '进化健康', path: '/insight/health', permission: 'insight:funnel', icon: 'Activity' },
      { key: 'funnel', label: '招聘漏斗', path: '/insight/funnel', permission: 'insight:funnel', icon: 'Filter' },
      { key: 'interviewer', label: '面试官分析', path: '/insight/interviewer', permission: 'insight:interviewer', icon: 'Users' },
      { key: 'roi', label: '渠道ROI', path: '/insight/roi', permission: 'insight:roi', icon: 'Coin' },
    ],
  },
  // ── 设置 ──
  {
    key: 'settings',
    label: '设置',
    path: '/settings/tenant',
    permission: 'settings',
    children: [
      { key: 'tenant', label: '租户设置', path: '/settings/tenant', permission: 'settings:tenant', icon: 'Setting' },
      { key: 'org', label: '组织架构', path: '/settings/org', permission: 'settings:org', icon: 'Share' },
      { key: 'role', label: '角色管理', path: '/settings/role', permission: 'settings:role', icon: 'Lock' },
      { key: 'user', label: '用户管理', path: '/settings/user', permission: 'settings:user', icon: 'UserFilled' },
      { key: 'sso', label: 'SSO', path: '/settings/integration/sso', permission: 'settings:sso', icon: 'Key' },
      { key: 'license', label: '许可配额', path: '/settings/compliance/license', permission: 'settings:license', icon: 'Stamp' },
    ],
  },
  // ── 旧入口兼容（不显示在主菜单）──
  {
    key: 'workspace-legacy',
    label: '旧工作台',
    path: '/workspace/inbox',
    permission: 'workspace',
    hidden: true,
    children: [
      { key: 'inbox', label: '收件箱', path: '/workspace/inbox', permission: 'workspace:inbox' },
      { key: 'today', label: '今日', path: '/workspace/today', permission: 'workspace:today' },
    ],
  },
  {
    key: 'planning-legacy',
    label: '旧招聘规划',
    path: '/planning/demands',
    permission: 'planning',
    hidden: true,
    children: [
      { key: 'demands', label: '需求', path: '/planning/demands' },
      { key: 'approvals', label: '审批', path: '/planning/approvals/pending' },
    ],
  },
]

export function hasPermission(permissions: string[], code?: string): boolean {
  if (!code) return true
  if (permissions.includes('PLATFORM_ADMIN')) return true
  if (permissions.includes(code)) return true
  const prefix = code.split(':')[0]
  return permissions.includes(prefix)
}

export function filterMenus(menus: MenuItem[], permissions: string[]): MenuItem[] {
  return menus
    .filter(m => !m.hidden && hasPermission(permissions, m.permission))
    .map(m => ({
      ...m,
      children: m.children?.filter(c => !c.hidden && hasPermission(permissions, c.permission)),
    }))
    .filter(m => !m.children || m.children.length > 0)
}

export function getDefaultRoute(roleCodes: string[]): string {
  // 所有角色的默认首页改为 AI 助手
  if (roleCodes.includes('HR_MANAGER') || roleCodes.includes('SUPER_ADMIN') || roleCodes.includes('DEPT_HEAD')) {
    return '/ai'
  }
  if (roleCodes.includes('INTERVIEWER')) {
    return '/ai'
  }
  if (roleCodes.includes('EMPLOYEE')) {
    return '/talent/referral'
  }
  return '/ai'
}
