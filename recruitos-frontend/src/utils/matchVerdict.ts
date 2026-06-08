import {
  MATCH_ADVICE,
  matchAdviceLabel,
  normalizeMatchLabel,
} from '@/constants/businessLabels'

export type MatchTier = 'PRIORITY' | 'SCREEN' | 'REVIEW' | 'PASS' | 'PENDING'

export interface MatchBreakdown {
  skillMatch?: number
  experienceMatch?: number
  educationMatch?: number
  resumeCompleteness?: number
}

export interface JdTagMatchRow {
  name: string
  score: number
  weight: number
  correlation: number
  trend: 'up' | 'down' | 'stable'
  matched?: boolean
  requirementType?: 'REQUIRED' | 'PREFERRED'
  requirementLabel?: string
  importance?: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface MatchVerdictData {
  status: MatchTier
  label: string
  modelScore?: number | null
  confidence?: string
  pros: string[]
  cons: string[]
  summary?: string
  rankInJob?: number
  totalInJob?: number
  percentile?: number
  suggestedAction?: string
  autoActionAllowed?: boolean
  breakdown?: MatchBreakdown
  jdTagMatches?: JdTagMatchRow[]
  jdTagHitRate?: number
}

export interface RadarDimension {
  name: string
  value: number
}

export interface TagMatchRow {
  name: string
  score: number
  weight: number
  correlation: number
  trend: 'up' | 'down' | 'stable'
}

export interface AiInsightRow {
  text: string
  type: 'highlight' | 'warning' | 'info'
}

const TIER_TAG: Record<MatchTier, '' | 'success' | 'warning' | 'info' | 'danger'> = {
  PRIORITY: 'success',
  SCREEN: '',
  REVIEW: 'warning',
  PASS: 'danger',
  PENDING: 'info',
}

const BREAKDOWN_LABELS: Record<keyof MatchBreakdown, string> = {
  skillMatch: '技能符合度',
  experienceMatch: '经验符合度',
  educationMatch: '学历符合度',
  resumeCompleteness: '简历完整度',
}

/** 任职要求对照表：岗位要求 | 候选人情况 | 判断 */
export function extractRequirementRows(verdict: MatchVerdictData): {
  name: string
  judgment: '符合' | '待确认' | '不符合'
  hint?: string
}[] {
  if (verdict.jdTagMatches?.length) {
    return verdict.jdTagMatches.map((t) => {
      const typeLabel = t.requirementLabel || (t.requirementType === 'REQUIRED' ? '必备' : '加分')
      const name = `${typeLabel}：${t.name}`
      if (t.matched === false) {
        const judgment = t.requirementType === 'REQUIRED' ? '不符合' : '待确认'
        return {
          name,
          judgment,
          hint: t.requirementType === 'REQUIRED' ? '简历中未体现该必备要求' : '简历中未明显体现，可面试时确认',
        }
      }
      return {
        name,
        judgment: t.score >= 60 ? '符合' : '待确认',
      }
    })
  }
  const b = verdict.breakdown
  if (!b) return []
  const rows: { name: string; judgment: '符合' | '待确认' | '不符合' }[] = []
  for (const key of Object.keys(BREAKDOWN_LABELS) as (keyof MatchBreakdown)[]) {
    const score = b[key]
    if (score == null) continue
    rows.push({
      name: BREAKDOWN_LABELS[key],
      judgment: score >= 70 ? '符合' : score >= 45 ? '待确认' : '不符合',
    })
  }
  return rows
}

export function parseMatchDetail(
  matchDetail?: string | null,
  matchScore?: number | null,
): MatchVerdictData {
  if (matchDetail) {
    try {
      const raw = JSON.parse(matchDetail)
      const breakdown = raw.breakdown as MatchBreakdown | undefined
      const tier = (raw.status || 'PENDING') as MatchTier
      return {
        status: tier,
        label: normalizeMatchLabel(raw.label) || matchAdviceLabel(tier),
        modelScore: raw.modelScore ?? matchScore ?? null,
        confidence: raw.confidence,
        pros: Array.isArray(raw.pros) ? raw.pros : [],
        cons: Array.isArray(raw.cons) ? raw.cons : [],
        summary: raw.summary,
        rankInJob: raw.rankInJob,
        totalInJob: raw.totalInJob,
        percentile: raw.percentile,
        suggestedAction: raw.suggestedAction,
        autoActionAllowed: raw.autoActionAllowed === true,
        breakdown,
        jdTagMatches: parseJdTagMatches(raw.jdTagMatches),
        jdTagHitRate: raw.jdTagHitRate,
      }
    } catch {
      /* fall through */
    }
  }
  if (matchScore == null) {
    return {
      status: 'PENDING',
      label: MATCH_ADVICE.PENDING.label,
      pros: [],
      cons: ['请先选择在招职位，或完成简历解析后再评估'],
      suggestedAction: MATCH_ADVICE.PENDING.action,
    }
  }
  const tier: MatchTier = matchScore >= 72 ? 'SCREEN' : matchScore >= 55 ? 'REVIEW' : 'PASS'
  return {
    status: tier,
    label: matchAdviceLabel(tier),
    modelScore: matchScore,
    pros: [],
    cons: [],
    summary: '建议结合符合点与待确认项综合判断',
  }
}

export function extractRadarDimensions(verdict: MatchVerdictData): RadarDimension[] {
  const b = verdict.breakdown || {}
  const model = verdict.modelScore ?? 0
  const rankScore =
    verdict.percentile != null ? Math.max(0, Math.min(100, 100 - verdict.percentile)) : 50

  return [
    { name: '技能匹配', value: clampScore(b.skillMatch ?? model * 0.7) },
    { name: '经验匹配', value: clampScore(b.experienceMatch ?? model * 0.65) },
    { name: '学历匹配', value: clampScore(b.educationMatch ?? model * 0.6) },
    { name: '简历完整度', value: clampScore(b.resumeCompleteness ?? (verdict.status === 'PENDING' ? 30 : 65)) },
    { name: '岗位相对优势', value: clampScore(rankScore) },
  ]
}

function parseJdTagMatches(raw: unknown): JdTagMatchRow[] | undefined {
  if (!Array.isArray(raw)) return undefined
  return raw.map((item: any) => ({
    name: item.name || item.tag || '-',
    score: Number(item.score) || 0,
    weight: Number(item.weight) || 0,
    correlation: Number(item.correlation) || 0,
    trend: item.trend === 'up' || item.trend === 'down' ? item.trend : 'stable',
    matched: item.matched === true,
    requirementType: item.requirementType,
    requirementLabel: item.requirementLabel,
    importance: item.importance,
  }))
}

export function extractTagMatches(verdict: MatchVerdictData): TagMatchRow[] {
  if (verdict.jdTagMatches?.length) {
    return verdict.jdTagMatches.map((t) => ({
      name: t.matched === false ? `${t.name}（未命中）` : t.name,
      score: t.score,
      weight: t.weight,
      correlation: t.correlation,
      trend: t.trend,
    }))
  }

  const b = verdict.breakdown
  if (!b) return []

  const items: { name: string; score: number }[] = []
  for (const key of Object.keys(BREAKDOWN_LABELS) as (keyof MatchBreakdown)[]) {
    const score = b[key]
    if (score != null) {
      items.push({ name: BREAKDOWN_LABELS[key], score: clampScore(score) })
    }
  }
  if (!items.length) return []

  const total = items.reduce((sum, i) => sum + i.score, 0) || 1
  return items.map((i) => ({
    name: i.name,
    score: i.score,
    weight: i.score / total,
    correlation: i.score / 100,
    trend: i.score >= 65 ? 'up' : i.score >= 45 ? 'stable' : 'down',
  }))
}

export function extractAiInsights(verdict: MatchVerdictData): AiInsightRow[] {
  const rows: AiInsightRow[] = []
  if (verdict.summary) {
    rows.push({ text: verdict.summary, type: 'info' })
  }
  for (const p of verdict.pros) {
    rows.push({ text: p, type: 'highlight' })
  }
  for (const c of verdict.cons) {
    rows.push({ text: c, type: 'warning' })
  }
  if (verdict.suggestedAction) {
    rows.push({ text: verdict.suggestedAction, type: 'info' })
  }
  if (!rows.length) {
    rows.push({ text: '暂无更多分析，请完善简历与岗位 JD 后重试', type: 'info' })
  }
  return rows
}

function clampScore(v: number): number {
  return Math.max(0, Math.min(100, Math.round(v)))
}

export function tierTagType(tier: MatchTier) {
  return TIER_TAG[tier] || 'info'
}
