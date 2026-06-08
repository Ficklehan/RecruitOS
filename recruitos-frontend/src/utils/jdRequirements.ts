/** 任职要求条目：招聘专员视图 ↔ 后端 tri-weight 映射 */

export type RequirementType = 'REQUIRED' | 'PREFERRED'
export type ImportanceLevel = 'HIGH' | 'MEDIUM' | 'LOW'

export interface RequirementItem {
  name: string
  requirementType: RequirementType
  importance: ImportanceLevel
  locked: boolean
  matchWeight: number
  searchWeight: number
  decisionWeight: number
}

const TYPE_BASE: Record<RequirementType, { match: number; decision: number }> = {
  REQUIRED: { match: 0.88, decision: 0.92 },
  PREFERRED: { match: 0.55, decision: 0.5 },
}

const IMPORTANCE_FACTOR: Record<ImportanceLevel, number> = {
  HIGH: 1,
  MEDIUM: 0.78,
  LOW: 0.55,
}

export const REQUIREMENT_TYPE_LABEL: Record<RequirementType, string> = {
  REQUIRED: '必备',
  PREFERRED: '加分',
}

export const IMPORTANCE_LABEL: Record<ImportanceLevel, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
}

export function weightsFromRequirement(
  requirementType: RequirementType,
  importance: ImportanceLevel,
): { matchWeight: number; searchWeight: number; decisionWeight: number } {
  const base = TYPE_BASE[requirementType]
  const factor = IMPORTANCE_FACTOR[importance]
  const matchWeight = round2(Math.min(1, base.match * factor))
  const decisionWeight = round2(Math.min(1, base.decision * factor))
  const searchWeight = round2(Math.min(1, matchWeight * 0.88))
  return { matchWeight, searchWeight, decisionWeight }
}

export function inferRequirementFromWeights(
  matchWeight = 0.5,
  decisionWeight = 0.5,
): { requirementType: RequirementType; importance: ImportanceLevel } {
  const requirementType: RequirementType = decisionWeight >= 0.68 ? 'REQUIRED' : 'PREFERRED'
  const score = Math.max(matchWeight, decisionWeight)
  const importance: ImportanceLevel =
    score >= 0.72 ? 'HIGH' : score >= 0.48 ? 'MEDIUM' : 'LOW'
  return { requirementType, importance }
}

/** 将 API 标签行转为任职要求条目 */
export function fromApiTag(raw: Record<string, unknown>): RequirementItem {
  const name = String(raw.tag || raw.name || '')
  const matchWeight = Number(raw.matchWeight ?? 0.5)
  const searchWeight = Number(raw.searchWeight ?? matchWeight * 0.88)
  const decisionWeight = Number(raw.decisionWeight ?? matchWeight * 0.7)
  const inferred = inferRequirementFromWeights(matchWeight, decisionWeight)
  return {
    name,
    requirementType: (raw.requirementType as RequirementType) || inferred.requirementType,
    importance: (raw.importance as ImportanceLevel) || inferred.importance,
    locked: raw.locked === true,
    matchWeight,
    searchWeight,
    decisionWeight,
  }
}

/** 保存前同步权重并输出 API 结构 */
export function toApiTag(item: RequirementItem): Record<string, unknown> {
  const weights = weightsFromRequirement(item.requirementType, item.importance)
  return {
    tag: item.name,
    name: item.name,
    requirementType: item.requirementType,
    importance: item.importance,
    matchWeight: weights.matchWeight,
    searchWeight: weights.searchWeight,
    decisionWeight: weights.decisionWeight,
    locked: item.locked,
  }
}

export function applyRequirementChange(item: RequirementItem): RequirementItem {
  const weights = weightsFromRequirement(item.requirementType, item.importance)
  return { ...item, ...weights }
}

function round2(n: number): number {
  return Math.round(n * 100) / 100
}
