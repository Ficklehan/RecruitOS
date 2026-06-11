export interface MenuItem {
  key: string
  label: string
  path: string
  permission?: string
  icon?: string
  hidden?: boolean
  /** 侧边栏匹配用的路由前缀，多个菜单共享同一前缀时用 key 区分 */
  routeKeys?: string[]
  children?: MenuItem[]
}

// === RecruitOS v7.1: 按招聘工作流组织 ===
// 人机协同的核心原则：AI 嵌入在每一步，不是独立菜单
//
// 工作台 → 需求 → 找人 → 面试 → Offer → 分析 → 设置

export const topNavMenus: MenuItem[] = [
  // ================================================================
  // 一、工作台 —— 每日入口 + AI 总控
  // ================================================================
  {
    key: 'workspace',
    label: '工作台',
    path: '/workspace',
    permission: 'workspace',
    children: [
      { key: 'dashboard', label: '今日概览', path: '/workspace/dashboard', permission: 'workspace:dashboard', icon: 'HomeFilled' },
      { key: 'inbox', label: '待办事项', path: '/workspace/inbox', permission: 'workspace:inbox', icon: 'Inbox' },
    ],
  },

  // ================================================================
  // 二、需求 —— AI 诊断 → 生成 JD → 审批 → 发布
  // ================================================================
  {
    key: 'demand',
    label: '需求',
    path: '/planning',
    permission: 'planning',
    routeKeys: ['planning'],
    children: [
      { key: 'demands', label: '需求列表', path: '/planning/demands', permission: 'planning:demand', icon: 'List' },
      { key: 'jobs', label: '在招职位', path: '/planning/jobs', permission: 'planning:job', icon: 'Briefcase' },
      { key: 'demand-board', label: '需求看板', path: '/planning/demands/board', permission: 'planning:demand:board', icon: 'Grid' },
      { key: 'approvals', label: '审批', path: '/planning/approvals/pending', permission: 'planning:approval', icon: 'Checked' },
      { key: 'demand-create', label: '新建需求（AI诊断）', path: '/planning/demands/create', permission: 'planning:demand', icon: 'Plus' },
    ],
  },

  // ================================================================
  // 三、找人 —— AI 搜索 → 匹配 → 排名 → 人筛选 → 联系
  // ================================================================
  {
    key: 'sourcing',
    label: '找人',
    path: '/pipeline/board',
    permission: 'pipeline',
    routeKeys: ['pipeline', 'talent'],
    children: [
      // -- 工作流核心 --
      { key: 'pipeline', label: '候选人管道', path: '/pipeline/board', permission: 'pipeline:board', icon: 'Grid' },
      { key: 'candidates', label: '候选人列表', path: '/pipeline/candidates', permission: 'pipeline:candidate', icon: 'User' },
      // -- 人才库与渠道 --
      { key: 'pool', label: '人才库', path: '/talent/pool', permission: 'talent:pool', icon: 'Files' },
      { key: 'resumes', label: '简历收件', path: '/talent/resumes', permission: 'talent:resume', icon: 'Document' },
      { key: 'channels', label: '渠道运营', path: '/talent/channels', permission: 'talent:channel', icon: 'Share' },
      { key: 'referral', label: '内推', path: '/talent/referral', permission: 'talent:referral', icon: 'Share' },
      { key: 'headhunters', label: '猎头', path: '/talent/headhunters', permission: 'talent:headhunter', icon: 'OfficeBuilding' },
    ],
  },

  // ================================================================
  // 四、面试 —— AI 实时辅助 → 评分 → 校准 → 决策
  // ================================================================
  {
    key: 'interview',
    label: '面试',
    path: '/pipeline/calendar',
    permission: 'pipeline',
    // 和"找人"共享 /pipeline prefix，用子路径精确匹配
    routeKeys: [],
    children: [
      { key: 'interview-board', label: '面试看板', path: '/pipeline/interview-board', permission: 'pipeline:board', icon: 'Grid' },
      { key: 'calendar', label: '面试日历', path: '/pipeline/calendar', permission: 'pipeline:calendar', icon: 'Calendar' },
      { key: 'evaluation', label: '评价管理', path: '/pipeline/evaluation', permission: 'pipeline:evaluation', icon: 'Edit' },
      { key: 'decision', label: '候选人决策', path: '/pipeline/decision', permission: 'pipeline:candidate', icon: 'DataAnalysis' },
    ],
  },

  // ================================================================
  // 五、Offer —— AI 策略 → 话术 → 审批 → 入职
  // ================================================================
  {
    key: 'offer',
    label: 'Offer',
    path: '/pipeline/offers',
    permission: 'pipeline',
    routeKeys: [],
    children: [
      { key: 'offers', label: 'Offer 列表', path: '/pipeline/offers', permission: 'pipeline:offer', icon: 'Tickets' },
      { key: 'offer-approvals', label: 'Offer 审批', path: '/pipeline/offers/approvals', permission: 'pipeline:offer', icon: 'Checked' },
      { key: 'onboards', label: '入职管理', path: '/pipeline/onboards', permission: 'pipeline:onboard', icon: 'Finished' },
      { key: 'onboard-tasks', label: '入职任务', path: '/pipeline/onboards/tasks', permission: 'pipeline:onboard', icon: 'List' },
    ],
  },

  // ================================================================
  // 六、分析 —— AI 学习人的决策，反馈优化建议
  // ================================================================
  {
    key: 'insight',
    label: '分析',
    path: '/insight/funnel',
    permission: 'insight',
    children: [
      { key: 'funnel', label: '招聘漏斗', path: '/insight/funnel', permission: 'insight:funnel', icon: 'Filter' },
      { key: 'roi', label: '渠道 ROI', path: '/insight/roi', permission: 'insight:roi', icon: 'Coin' },
      { key: 'interviewer', label: '面试官分析', path: '/insight/interviewer', permission: 'insight:interviewer', icon: 'Users' },
      { key: 'cycle', label: '招聘周期', path: '/insight/cycle', permission: 'insight:cycle', icon: 'Timer' },
      { key: 'evolution-proposals', label: '招人方式建议', path: '/planning/evolution/proposals', permission: 'planning:job', icon: 'Bell' },
    ],
  },

  // ================================================================
  // 七、设置
  // ================================================================
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
      { key: 'ai-settings', label: 'AI 设置', path: '/settings/ai', permission: 'settings:tenant', icon: 'Cpu' },
      { key: 'sso', label: 'SSO 对接', path: '/settings/integration/sso', permission: 'settings:sso', icon: 'Key' },
      { key: 'license', label: '许可配额', path: '/settings/compliance/license', permission: 'settings:license', icon: 'Stamp' },
    ],
  },

  // ================================================================
  // 隐藏菜单 —— AI 专用页面（不从菜单进入，由工作流内嵌触发）
  // ================================================================
  {
    key: 'ai-hidden',
    label: 'AI 工具',
    path: '/ai',
    permission: 'workspace',
    hidden: true,
    children: [
      { key: 'ai-copilot', label: 'AI Co-Pilot', path: '/ai/copilot', permission: 'workspace', icon: 'Sparkles' },
      { key: 'ai-demand', label: 'AI 需求诊断', path: '/ai/demand/create', permission: 'planning:demand', icon: 'Search' },
      { key: 'ai-calibration', label: 'AI 校准会', path: '/ai/calibration', permission: 'pipeline:evaluation', icon: 'Scale' },
      { key: 'ai-intent', label: 'AI 意向预测', path: '/ai/intent', permission: 'pipeline:candidate', icon: 'TrendingUp' },
      { key: 'ai-offer', label: 'AI Offer 策略', path: '/ai/offer-strategy', permission: 'pipeline:offer', icon: 'DollarSign' },
      { key: 'ai-interviewer', label: 'AI 面试官质量', path: '/ai/interviewer-quality', permission: 'insight:interviewer', icon: 'UserCheck' },
      { key: 'ai-talent', label: 'AI 人才密度', path: '/ai/talent-density', permission: 'insight', icon: 'BarChart3' },
      { key: 'ai-diagnose', label: '诊断中心', path: '/ai/diagnose', permission: 'insight', icon: 'Activity' },
      { key: 'ai-report', label: '周报', path: '/ai/report', permission: 'insight', icon: 'FileText' },
    ],
  },
]

// ================================================================
// 工具函数
// ================================================================

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

/** 根据当前路由找到对应的一级菜单 */
export function findActiveMenu(menus: MenuItem[], routePath: string): MenuItem | null {
  const prefix = '/' + routePath.split('/')[1]

  // 1. routeKeys 精确匹配（处理共享 prefix 的情况）
  for (const m of menus) {
    if (m.routeKeys && m.routeKeys.length > 0) {
      if (m.routeKeys.some(rk => routePath.startsWith('/' + rk))) {
        // 还需检查：该菜单的子路径是否真的匹配当前路由
        if (m.children?.some(c => routePath.startsWith(c.path))) {
          return m
        }
      }
    }
  }

  // 2. key 匹配 route prefix（向后兼容，适用于 key===prefix 的菜单）
  const byKey = menus.find(m => !m.routeKeys?.length && prefix === '/' + m.key)
  if (byKey) return byKey

  // 3. 兜底：第一个有子路径匹配的菜单
  return menus.find(m => m.children?.some(c => routePath.startsWith(c.path))) || null
}

export function getDefaultRoute(roleCodes: string[]): string {
  if (roleCodes.includes('SUPER_ADMIN') || roleCodes.includes('HR_MANAGER') || roleCodes.includes('DEPT_HEAD')) {
    return '/workspace/dashboard'
  }
  if (roleCodes.includes('INTERVIEWER')) {
    return '/pipeline/board'
  }
  if (roleCodes.includes('EMPLOYEE')) {
    return '/talent/referral'
  }
  return '/workspace/dashboard'
}
