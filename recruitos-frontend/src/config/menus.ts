export interface MenuItem {
  key: string
  label: string
  path: string
  permission?: string
  icon?: string
  hidden?: boolean
  children?: MenuItem[]
}

export const topNavMenus: MenuItem[] = [
  {
    key: 'workspace',
    label: '工作台',
    path: '/workspace/inbox',
    permission: 'workspace',
    children: [
      { key: 'inbox', label: '收件箱', path: '/workspace/inbox', permission: 'workspace:inbox', icon: 'Message' },
      { key: 'today', label: '今日', path: '/workspace/today', permission: 'workspace:today', icon: 'Calendar' },
      { key: 'dashboard', label: '驾驶舱', path: '/workspace/dashboard', permission: 'workspace:dashboard', icon: 'Odometer' },
    ],
  },
  {
    key: 'pipeline',
    label: '招聘执行',
    path: '/pipeline/board',
    permission: 'pipeline',
    children: [
      { key: 'board', label: '招聘进展', path: '/pipeline/board', permission: 'pipeline:board', icon: 'Grid' },
      { key: 'candidates', label: '候选人', path: '/pipeline/candidates', permission: 'pipeline:candidate', icon: 'User' },
      { key: 'match', label: '匹配评估', path: '/pipeline/decision', permission: 'pipeline:candidate', icon: 'DataAnalysis', hidden: true },
      { key: 'calendar', label: '面试日历', path: '/pipeline/calendar', permission: 'pipeline:calendar', icon: 'Calendar' },
      { key: 'offers', label: '录用通知', path: '/pipeline/offers', permission: 'pipeline:offer', icon: 'Tickets' },
      { key: 'onboards', label: '入职', path: '/pipeline/onboards', permission: 'pipeline:onboard', icon: 'Finished' },
    ],
  },
  {
    key: 'planning',
    label: '招聘规划',
    path: '/planning/demands',
    permission: 'planning',
    children: [
      { key: 'demands', label: '招聘需求', path: '/planning/demands', permission: 'planning:demand', icon: 'List' },
      { key: 'demand-board', label: '需求看板', path: '/planning/demands/board', permission: 'planning:demand:board', icon: 'Grid' },
      { key: 'approvals', label: '审批', path: '/planning/approvals/pending', permission: 'planning:approval', icon: 'Checked' },
      { key: 'jobs', label: '在招职位', path: '/planning/jobs', permission: 'planning:job', icon: 'Briefcase' },
      { key: 'evolution-proposals', label: '招人方式建议', path: '/planning/evolution/proposals', permission: 'planning:job', icon: 'Bell' },
    ],
  },
  {
    key: 'talent',
    label: '人才库',
    path: '/talent/pool',
    permission: 'talent',
    children: [
      { key: 'pool', label: '人才库', path: '/talent/pool', permission: 'talent:pool', icon: 'Files' },
      { key: 'resumes', label: '简历收件', path: '/talent/resumes', permission: 'talent:resume', icon: 'Document' },
      { key: 'channels', label: '渠道与账号', path: '/talent/channels', permission: 'talent:channel', icon: 'Share' },
      { key: 'channel-staging', label: '跨职位待联系池', path: '/talent/channel-staging', permission: 'talent:channel', icon: 'Box' },
      { key: 'agents', label: '自动招聘任务', path: '/talent/channels/agents', permission: 'talent:channel:agent', icon: 'Connection' },
      { key: 'comm-profile', label: '对外沟通风格', path: '/talent/communication-profile', permission: 'talent:template', icon: 'ChatDotRound' },
      { key: 'referral', label: '内推', path: '/talent/referral', permission: 'talent:referral', icon: 'Share' },
      { key: 'headhunter', label: '猎头', path: '/talent/headhunters', permission: 'talent:headhunter', icon: 'OfficeBuilding' },
      { key: 'templates', label: '话术模板', path: '/talent/templates', permission: 'talent:template', icon: 'ChatLineRound' },
    ],
  },
  {
    key: 'insight',
    label: '数据洞察',
    path: '/insight/funnel',
    permission: 'insight',
    children: [
      { key: 'funnel', label: '招聘漏斗', path: '/insight/funnel', permission: 'insight:funnel', icon: 'Filter' },
      { key: 'cycle', label: '招聘周期', path: '/insight/cycle', permission: 'insight:cycle', icon: 'Timer' },
      { key: 'roi', label: '渠道ROI', path: '/insight/roi', permission: 'insight:roi', icon: 'Coin' },
      { key: 'interviewer', label: '面试官效能', path: '/insight/interviewer', permission: 'insight:interviewer', icon: 'User' },
    ],
  },
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
      { key: 'sso', label: 'SSO配置', path: '/settings/integration/sso', permission: 'settings:sso', icon: 'Key' },
      { key: 'license', label: '许可配额', path: '/settings/compliance/license', permission: 'settings:license', icon: 'Stamp' },
      { key: 'safety', label: '对话安全', path: '/settings/compliance/safety', permission: 'settings:safety', icon: 'Warning' },
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
    .filter(m => hasPermission(permissions, m.permission))
    .map(m => ({
      ...m,
      children: m.children?.filter(c => hasPermission(permissions, c.permission) && !c.hidden),
    }))
    .filter(m => !m.children || m.children.length > 0)
}

export function getDefaultRoute(roleCodes: string[]): string {
  if (roleCodes.includes('HR_MANAGER') || roleCodes.includes('SUPER_ADMIN')) {
    return '/workspace/dashboard'
  }
  if (roleCodes.includes('INTERVIEWER')) {
    return '/workspace/today'
  }
  if (roleCodes.includes('DEPT_HEAD')) {
    return '/workspace/inbox'
  }
  if (roleCodes.includes('EMPLOYEE')) {
    return '/talent/referral'
  }
  return '/workspace/inbox'
}
