export interface MenuItem {
  key: string
  label: string
  path: string
  permission?: string
  icon?: string
  hidden?: boolean
  /** AI 状态指示: 数字=未处理数量 */
  aiStatus?: number
  children?: MenuItem[]
}

// ================================================================
// RecruitOS v8 菜单：按人机协同模式组织
// AI 渗透到每个协同场景中，不再有独立的 "AI 助手" 菜单
// 每个菜单项的 aiStatus 由 cognitive_observation 表驱动
// ================================================================

export const topNavMenus: MenuItem[] = [
  // ═══════ 在招岗位：AI+人的协同主战场 ═══════
  {
    key: 'jobs',
    label: '在招岗位',
    path: '/jobs',
    permission: 'planning',
    children: [
      { key: 'job-list',   label: '全部岗位',   path: '/jobs',              permission: 'planning:job',      icon: 'Briefcase' },
      { key: 'pipeline',   label: '候选人管道', path: '/pipeline/board',    permission: 'pipeline:board',    icon: 'KanbanSquare' },
      { key: 'calendar',   label: '面试日历',   path: '/pipeline/calendar', permission: 'pipeline:calendar', icon: 'Calendar' },
      { key: 'offers',     label: '录用通知',   path: '/pipeline/offers',   permission: 'pipeline:offer',    icon: 'FileCheck' },
      { key: 'onboards',   label: '入职',       path: '/pipeline/onboards', permission: 'pipeline:onboard',  icon: 'UserCheck' },
    ],
  },

  // ═══════ 人才发现：AI搜+你搜，合并去重 ═══════
  {
    key: 'talent',
    label: '人才发现',
    path: '/talent/pool',
    permission: 'talent',
    children: [
      { key: 'agent-feed',   label: 'AI 推荐',    path: '/talent/agent-feed',   permission: 'talent:pool',    icon: 'Sparkles' },
      { key: 'active-search',label: '主动搜索',    path: '/talent/search',       permission: 'talent:resume',  icon: 'Search' },
      { key: 'pool',         label: '人才库激活',  path: '/talent/pool',         permission: 'talent:pool',    icon: 'Database' },
      { key: 'agent',        label: 'Agent 调度',  path: '/talent/channels/agents', permission: 'talent:channel', icon: 'Bot' },
      { key: 'referral',     label: '内推',        path: '/talent/referral',     permission: 'talent:referral',icon: 'Users' },
      { key: 'headhunter',   label: '猎头',        path: '/talent/headhunters',  permission: 'talent:headhunter', icon: 'Building2' },
    ],
  },

  // ═══════ 面试与评估：AI初评+校准+你定级 ═══════
  {
    key: 'evaluate',
    label: '面试评估',
    path: '/evaluate',
    permission: 'pipeline',
    children: [
      { key: 'eval-prep',      label: '面试准备',   path: '/evaluate/prep',        permission: 'pipeline:candidate',  icon: 'ClipboardList' },
      { key: 'eval-scorecard', label: '面试评估',   path: '/evaluate/scorecard',   permission: 'pipeline:candidate',  icon: 'PenTool' },
      { key: 'calibration',    label: '校准会',     path: '/ai/calibration',       permission: 'insight:interviewer', icon: 'Scale' },
      { key: 'interviewer',    label: '面试官质量', path: '/evaluate/interviewer',  permission: 'insight:interviewer', icon: 'TrendingUp' },
    ],
  },

  // ═══════ 洞察：AI主动来找你 ═══════
  {
    key: 'insight',
    label: '洞察',
    path: '/insight/alerts',
    permission: 'insight',
    children: [
      { key: 'alerts',          label: '需要处理', path: '/insight/alerts',    permission: 'insight:funnel', icon: 'AlertTriangle' },
      { key: 'attention',       label: '值得关注', path: '/insight/attention', permission: 'insight:funnel', icon: 'Eye' },
      { key: 'observe',         label: '长期观察', path: '/insight/observe',   permission: 'insight:funnel', icon: 'TrendingUp' },
      { key: 'demand-diagnose', label: '需求诊断', path: '/ai/demand/create',  permission: 'planning:demand', icon: 'Stethoscope' },
      { key: 'funnel',          label: '招聘漏斗', path: '/insight/funnel',    permission: 'insight:funnel', icon: 'Funnel' },
      { key: 'roi',             label: '渠道ROI',  path: '/insight/roi',       permission: 'insight:roi',    icon: 'Coins' },
    ],
  },

  // ═══════ 知识与模式：AI学到了什么 ═══════
  {
    key: 'knowledge',
    label: '知识',
    path: '/knowledge',
    permission: 'insight',
    children: [
      { key: 'success',       label: '成功画像', path: '/knowledge/profiles',    permission: 'insight:funnel', icon: 'Target' },
      { key: 'decisions',     label: '决策记录', path: '/knowledge/decisions',   permission: 'insight:funnel', icon: 'History' },
      { key: 'lessons',       label: '教训库',   path: '/knowledge/lessons',     permission: 'insight:funnel', icon: 'BookOpen' },
      { key: 'my-patterns',   label: '我的模式', path: '/knowledge/my-patterns', permission: 'insight:funnel', icon: 'UserCog' },
      { key: 'weekly-report', label: '周报',     path: '/ai/report',             permission: 'insight',        icon: 'FileText' },
    ],
  },

  // ═══════ 设置 ═══════
  {
    key: 'settings',
    label: '设置',
    path: '/settings/tenant',
    permission: 'settings',
    children: [
      { key: 'tenant-settings', label: '租户设置', path: '/settings/tenant', permission: 'settings:tenant', icon: 'Settings' },
      { key: 'org',             label: '组织架构', path: '/settings/org',    permission: 'settings:org',    icon: 'OrganisationChart' },
      { key: 'role',            label: '角色管理', path: '/settings/role',   permission: 'settings:role',   icon: 'Shield' },
      { key: 'user',            label: '用户管理', path: '/settings/user',   permission: 'settings:user',   icon: 'Users' },
      { key: 'ai-config',       label: 'AI 设置',  path: '/settings/ai',     permission: 'settings:tenant', icon: 'Brain' },
    ],
  },

  // ═══════ 兼容旧路由 (hidden) ═══════
  {
    key: 'ai-legacy', label: '旧AI助手', path: '/ai', permission: 'workspace', hidden: true,
    children: [
      { key: 'ai-home',       label: 'AI首页',   path: '/ai',               permission: 'workspace:inbox' },
      { key: 'ai-diagnose',   label: '诊断',     path: '/ai/diagnose',      permission: 'insight' },
      { key: 'ai-intent',     label: '意向预测',  path: '/ai/intent',        permission: 'insight' },
      { key: 'ai-cycle',      label: '周期预测',  path: '/ai/cycle',         permission: 'insight' },
      { key: 'ai-offer',      label: 'Offer策略', path: '/ai/offer-strategy',permission: 'insight' },
      { key: 'ai-density',    label: '人才密度',  path: '/ai/talent-density',permission: 'insight' },
    ],
  },
  {
    key: 'workspace-legacy', label: '旧工作台', path: '/workspace/inbox', permission: 'workspace', hidden: true,
    children: [
      { key: 'inbox', label: '收件箱', path: '/workspace/inbox', permission: 'workspace:inbox' },
      { key: 'today', label: '今日',   path: '/workspace/today', permission: 'workspace:today' },
    ],
  },
  {
    key: 'planning-legacy', label: '旧招聘规划', path: '/planning/demands', permission: 'planning', hidden: true,
    children: [
      { key: 'demands',   label: '需求', path: '/planning/demands' },
      { key: 'approvals', label: '审批', path: '/planning/approvals/pending' },
    ],
  },
  {
    key: 'hiring-legacy', label: '旧招人', path: '/planning/jobs', permission: 'planning', hidden: true,
    children: [
      { key: 'old-jobs',  label: '在招职位', path: '/planning/jobs',         permission: 'planning:job' },
      { key: 'old-match', label: '匹配评估', path: '/pipeline/decision',     permission: 'pipeline:candidate' },
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
  if (roleCodes.includes('HR_MANAGER') || roleCodes.includes('SUPER_ADMIN') || roleCodes.includes('DEPT_HEAD')) {
    return '/jobs'
  }
  if (roleCodes.includes('INTERVIEWER')) {
    return '/evaluate'
  }
  if (roleCodes.includes('EMPLOYEE')) {
    return '/talent/referral'
  }
  return '/jobs'
}
