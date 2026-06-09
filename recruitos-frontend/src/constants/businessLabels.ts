/**
 * RecruitOS 招聘业务用语规范 v2（v3.1 UX）
 * 全站 UI 文案唯一出口（禁止页面硬编码同义不同词）
 */

export const OBJECTS = {
  job: '在招职位',
  jobShort: '职位',
  demand: '招聘需求',
  candidate: '候选人',
  activeCandidates: '在招候选人',
  resume: '简历',
  talentPool: '人才库',
  channel: '招聘渠道',
  matchEval: '匹配评估',
  offer: '录用通知',
  jobDescription: '职位描述',
  jobRequirements: '任职要求',
  sourcingMethod: '招人方式',
  stagingPool: '待联系池',
  platformTask: '平台招人任务',
  commStyle: '对外沟通风格',
  evolutionSuggestion: '招人方式优化建议',
  rollbackSuggestion: '恢复上一版招人方式',
} as const

/** 职位工作台 4 业务 Tab */
export const JOB_WORKSPACE_TABS = {
  overview: '总览',
  sourcing: '找人',
  candidates: '管人',
  rules: '规则',
} as const

export const PIPELINE_STAGE: Record<string, { label: string; column: string; hint?: string }> = {
  SOURCED: { label: '新人待筛选', column: '新人待筛选', hint: '刚纳入本职位，尚未初筛' },
  SCREENING: { label: '初筛中', column: '初筛中', hint: 'HR 初筛进行中' },
  CONTACTED: { label: '已联系上', column: '已联系上', hint: '已与候选人取得联系' },
  INTERVIEWING: { label: '面试安排中', column: '面试安排中', hint: '面试已安排或进行中' },
  EVALUATED: { label: '待决定是否录用', column: '待决定是否录用', hint: '面试完成，等待是否发录用通知' },
  OFFER: { label: '录用通知中', column: '录用通知中', hint: '录用通知审批或发送中' },
  HIRED: { label: '已入职', column: '已入职' },
  ARCHIVED: { label: '已结束', column: '已结束', hint: '本职位流程已关闭（不合适或放弃）' },
}

export const JOB_STATUS: Record<string, string> = {
  ACTIVE: '招聘中',
  PAUSED: '已暂停',
  CLOSED: '已关闭',
  DRAFT: '草稿',
}

export const CANDIDATE_STATUS: Record<string, string> = {
  NEW: '新简历',
  SCREENING: '进行中',
  INTERVIEWING: '面试中',
  OFFER: 'Offer 阶段',
  ONBOARD: '已入职',
  POOL: '已储备',
  BLACKLIST: '已拉黑',
}

export const MATCH_ADVICE: Record<string, { label: string; action: string }> = {
  PRIORITY: { label: '建议优先联系', action: '安排初筛' },
  SCREEN: { label: '建议安排初筛', action: '进入下一轮' },
  REVIEW: { label: '建议再了解一下', action: '查看匹配评估' },
  PASS: { label: '暂不合适（本职位）', action: '标记不合适' },
  PENDING: { label: '信息不足，暂无法判断', action: '完善简历或任职要求' },
}

export const ACTIONS = {
  viewMatch: '查看匹配',
  passScreen: '通过初筛',
  nextRound: '进入下一轮',
  markUnfit: '标记不合适',
  reserveToPool: '储备至人才库',
  contact: '联系候选人',
  scheduleInterview: '安排面试',
  startPlatformTask: '开始平台招人',
  startChannelHire: '开始平台招人',
  confirmGreet: '确认发送首次联系',
  importCandidate: '纳入本职位候选人',
  editRequirements: '编辑任职要求',
  editSourcingMethod: '设置招人方式',
  loadMatchEval: '查看匹配评估',
  goProcess: '去处理',
  goScreen: '去筛选',
} as const

export const GREET_STRATEGY_LABEL: Record<string, string> = {
  SCREEN_THEN_GREET: '合适了再联系',
  COLLECT_ONLY: '仅收藏到待联系池',
  CARD_GREET: '卡片即联系（高风险）',
}

export const RUN_MODE_LABEL: Record<string, string> = {
  SEMI_AUTO: '半自动（发送前请你确认）',
  FULL_AUTO: '全自动',
  PUBLISH_SEARCH_ONLY: '仅发布与搜索',
  COLLECT_ONLY: '仅收藏到待联系池',
}

export const JOB_RECRUIT_CHIP: Record<string, string> = {
  NO_METHOD: '待设置招人方式',
  NO_TASK: '待开始招人',
  RUNNING: '正在找人',
  PENDING: '需处理',
  PAUSED: '已暂停招人',
}

export const SOURCE_LABEL: Record<string, string> = {
  PLATFORM: '招聘平台',
  BOSS: 'Boss直聘',
  LIEPIN: '猎聘',
  LAGOU: '拉勾',
  ZHILIAN: '智联招聘',
  REFERRAL: '内推',
  HEADHUNTER: '猎头',
  DIRECT: '官网投递',
  PORTAL: '招聘门户',
  MANUAL: '手动录入',
  AI_SEARCH: '主动搜寻',
}

export const DEMAND_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  JOB_CREATED: '已创建职位',
  RECRUITING: '招聘中',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
}

/** 对外禁用词（代码审查/文案检查参考） */
export const FORBIDDEN_UI_TERMS = [
  '获客', '寻源', '入库', 'Agent任务', '决策面板', '匹配分析', '已建岗', '保守档位',
  'OpsPack', 'evolution', 'proposal', 'ROLLBACK', 'CARD_GREET', 'SCREEN_THEN_GREET',
  'CommunicationProfile', '渠道暂存库', '策略进化', '运营包',
] as const

/** 收件箱 Tab（动词导向 v3.1） */
export const INBOX_TAB: Record<string, string> = {
  all: '全部',
  confirm: '待你确认',
  interview: '待面试',
  hiring: '录用相关',
  evolution: '招人方式建议',
  approval: '待审批',
}

export const INBOX_TYPE: Record<string, string> = {
  confirm: '待你确认',
  approval: '待审批',
  interview: '待面试',
  feedback: '待反馈',
  offer: '录用相关',
  onboard: '准备入职',
  evolution: '招人方式建议',
  message: '系统消息',
}

export const OFFER_STATUS: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  SENT: '已发送',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  EXPIRED: '已过期',
  CANCELLED: '已取消',
}

export const EDUCATION_LABEL: Record<string, string> = {
  PHD: '博士',
  MASTER: '硕士',
  BACHELOR: '本科',
  COLLEGE: '大专',
  HIGH_SCHOOL: '高中',
}

export function pipelineStageLabel(code?: string | null, mode: 'label' | 'column' = 'label'): string {
  if (!code) return PIPELINE_STAGE.SOURCED.label
  const item = PIPELINE_STAGE[code]
  if (!item) return code
  return mode === 'column' ? item.column : item.label
}

export function jobStatusLabel(status?: string | null): string {
  if (!status) return '-'
  return JOB_STATUS[status] || status
}

export function candidateStatusLabel(status?: string | null): string {
  if (!status) return '-'
  return CANDIDATE_STATUS[status] || status
}

export function matchAdviceLabel(tier?: string | null): string {
  if (!tier) return MATCH_ADVICE.PENDING.label
  return MATCH_ADVICE[tier]?.label || tier
}

export function sourceLabel(source?: string | null): string {
  if (!source) return '-'
  return SOURCE_LABEL[source] || source
}

export function demandStatusLabel(status?: string | null): string {
  if (!status) return '-'
  return DEMAND_STATUS[status] || status
}

export function inboxTypeLabel(type?: string | null): string {
  if (!type) return '-'
  return INBOX_TYPE[type] || type
}

export function offerStatusLabel(status?: string | null): string {
  if (!status) return '-'
  return OFFER_STATUS[status] || status
}

export function educationLabel(edu?: string | null): string {
  if (!edu) return '-'
  const key = edu.toUpperCase()
  return EDUCATION_LABEL[key] || edu
}

export function greetStrategyLabel(code?: string | null): string {
  if (!code) return GREET_STRATEGY_LABEL.SCREEN_THEN_GREET
  return GREET_STRATEGY_LABEL[code] || code
}

export function runModeLabel(code?: string | null): string {
  if (!code) return RUN_MODE_LABEL.SEMI_AUTO
  return RUN_MODE_LABEL[code] || code
}

export function inboxTabLabel(tab?: string | null): string {
  if (!tab) return INBOX_TAB.all
  return INBOX_TAB[tab] || tab
}

/** 将后端匹配档位文案映射到规范用语（兼容旧数据） */
export function normalizeMatchLabel(label?: string | null): string {
  if (!label) return MATCH_ADVICE.PENDING.label
  const legacy: Record<string, string> = {
    '优先推荐': MATCH_ADVICE.PRIORITY.label,
    '可进入筛选': MATCH_ADVICE.SCREEN.label,
    '建议人工复核': MATCH_ADVICE.REVIEW.label,
    '暂不建议': MATCH_ADVICE.PASS.label,
    '待评估': MATCH_ADVICE.PENDING.label,
  }
  return legacy[label] || label
}
