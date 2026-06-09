/** PRD 两轮面试结构化评价维度 */

export interface EvalDimension {
  key: string
  label: string
  weight: number
}

export interface EvalTemplate {
  roundLabel: string
  dimensions: EvalDimension[]
}

export const INTERVIEW_EVAL_TEMPLATES: Record<string, EvalTemplate> = {
  INITIAL: {
    roundLabel: '初试',
    dimensions: [
      { key: 'tech', label: '技术能力', weight: 0.4 },
      { key: 'communication', label: '沟通表达', weight: 0.2 },
      { key: 'project', label: '项目经验', weight: 0.25 },
      { key: 'learning', label: '学习能力', weight: 0.15 },
    ],
  },
  SECOND: {
    roundLabel: '复试',
    dimensions: [
      { key: 'comprehensive', label: '综合素养', weight: 0.3 },
      { key: 'teamwork', label: '团队协作', weight: 0.25 },
      { key: 'culture', label: '文化匹配', weight: 0.25 },
      { key: 'potential', label: '发展潜力', weight: 0.2 },
    ],
  },
  FINAL: {
    roundLabel: '终面',
    dimensions: [
      { key: 'comprehensive', label: '综合素养', weight: 0.3 },
      { key: 'teamwork', label: '团队协作', weight: 0.25 },
      { key: 'culture', label: '文化匹配', weight: 0.25 },
      { key: 'potential', label: '发展潜力', weight: 0.2 },
    ],
  },
}

export function evalTemplateForRound(round?: string | null): EvalTemplate {
  if (!round) return INTERVIEW_EVAL_TEMPLATES.INITIAL
  const key = round.toUpperCase()
  return INTERVIEW_EVAL_TEMPLATES[key] || INTERVIEW_EVAL_TEMPLATES.INITIAL
}

export function computeWeightedScore(
  dimensions: EvalDimension[],
  scores: Record<string, number>,
): number {
  let total = 0
  let weightSum = 0
  for (const d of dimensions) {
    const s = scores[d.key] ?? 0
    total += s * d.weight
    weightSum += d.weight
  }
  return weightSum > 0 ? Math.round(total / weightSum) : 0
}
