/**
 * RecruitOS 招聘业务用语规范 v1
 * 全站 UI 文案唯一出口（禁止页面硬编码同义不同词）
 */

export const OBJECTS = {
  job: '在招职位',
  jobShort: '职位',
  demand: '招聘需求',
  candidate: '候选人',
  resume: '简历',
  talentPool: '人才库',
  channel: '招聘渠道',
  matchEval: '匹配评估',
  offer: '录用通知',
  jobDescription: '职位描述',
  jobRequirements: '任职要求',
} as const

export const PIPELINE_STAGE: Record<string, { label: string; column: string; hint?: string }> = {
  SOURCED: { label: '待处理', column: '新候选人', hint: '刚进入本职位，尚未初筛' },
  SCREENING: { label: '初筛中', column: '初筛中', hint: 'HR 初筛进行中' },
  CONTACTED: { label: '已联系', column: '已联系', hint: '已与候选人取得联系' },
  INTERVIEWING: { label: '面试中', column: '面试中' },
  EVALUATED: { label: '待录用决策', column: '待录用决策', hint: '面试完成，等待是否发 Offer' },
  OFFER: { label: 'Offer 阶段', column: 'Offer 阶段' },
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
  startChannelHire: '开始渠道招聘',
  confirmGreet: '确认发送招呼',
  importCandidate: '加入候选人',
  editRequirements: '编辑任职要求',
  loadMatchEval: '查看匹配评估',
} as const

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
] as const

export const INBOX_TYPE: Record<string, string> = {
  approval: '待审批',
  interview: '待面试',
  offer: '录用通知',
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
