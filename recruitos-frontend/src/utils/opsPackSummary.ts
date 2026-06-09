import { greetStrategyLabel, OBJECTS, runModeLabel } from '@/constants/businessLabels'

export interface OpsPackBody {
  searchKeywords?: string[]
  screeningProfile?: {
    passThreshold?: number
    stage1CardRules?: unknown[]
    stage2ResumeRules?: unknown[]
  }
  communicationProfile?: Record<string, unknown>
  rechatPolicy?: { maxAttempts?: number; intervalHours?: number }
  greetStrategy?: string
  platformQuotas?: Record<string, number>
}

export function parseOpsPackBody(pack: unknown): OpsPackBody {
  if (!pack) return {}
  if (typeof pack === 'string') {
    try {
      return JSON.parse(pack) as OpsPackBody
    } catch {
      return {}
    }
  }
  return pack as OpsPackBody
}

/** 招人方式人话摘要（3 条，用于确认弹窗/列表） */
export function opsPackHumanSummary(pack: unknown): string[] {
  const body = parseOpsPackBody(pack)
  const lines: string[] = []
  const kw = body.searchKeywords?.filter(Boolean) || []
  lines.push(kw.length ? `找什么人：${kw.slice(0, 5).join('、')}${kw.length > 5 ? ' 等' : ''}` : '找什么人：尚未设置关键词')
  const threshold = body.screeningProfile?.passThreshold
  lines.push(threshold != null ? `怎么筛：通过线 ${threshold} 分` : '怎么筛：使用默认筛选规则')
  lines.push(`怎么联系：${greetStrategyLabel(body.greetStrategy)}`)
  return lines
}

export function opsPackOneLine(pack: unknown): string {
  return opsPackHumanSummary(pack).join(' · ')
}

export function proposalTypeLabel(type?: string | null): string {
  if (type === 'ROLLBACK') return OBJECTS.rollbackSuggestion
  if (type === 'KEYWORD') return '搜索关键词优化'
  if (type === 'SCREENING') return '筛选规则优化'
  if (type === 'COMMUNICATION') return '对外沟通优化'
  return OBJECTS.evolutionSuggestion
}

/** 将 diff 对象转人话行（避免 JSON） */
export function diffHumanLines(diff: unknown): string[] {
  if (!diff || typeof diff !== 'object') return ['暂无详细变更说明']
  const d = diff as Record<string, unknown>
  const lines: string[] = []
  if (d.searchKeywords) lines.push('调整搜索关键词')
  if (d.screeningProfile || d.passThreshold) lines.push('调整筛选规则或通过线')
  if (d.greetStrategy) lines.push(`联系策略改为：${greetStrategyLabel(String(d.greetStrategy))}`)
  if (d.communicationProfile) lines.push('调整对外沟通风格')
  if (d.rechatPolicy) lines.push('调整跟进节奏')
  if (d.platformQuotas) lines.push('调整平台日配额')
  return lines.length ? lines : ['优化招人方式部分配置']
}

export function defaultOpsPackBody(): OpsPackBody {
  return {
    searchKeywords: [],
    screeningProfile: { passThreshold: 60, stage1CardRules: [], stage2ResumeRules: [] },
    communicationProfile: {},
    rechatPolicy: { maxAttempts: 2, intervalHours: 48 },
    greetStrategy: 'SCREEN_THEN_GREET',
    platformQuotas: { BOSS: 30, LIEPIN: 30 },
  }
}

export function mergeOpsPackBody(raw: unknown): OpsPackBody {
  return { ...defaultOpsPackBody(), ...parseOpsPackBody(raw) }
}

export { runModeLabel }
