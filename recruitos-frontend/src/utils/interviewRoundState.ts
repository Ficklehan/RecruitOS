/** 两轮面试状态机（PRD §10） */

export type InterviewRound = 'INITIAL' | 'SECOND' | 'FINAL'
export type InterviewStatus =
  | 'PENDING_ARRANGE'
  | 'ARRANGED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export interface RoundStateInfo {
  label: string
  phase: string
  tagType: 'info' | 'success' | 'warning' | 'danger' | 'primary'
  nextAction?: string
}

const STATUS_LABEL: Record<string, string> = {
  PENDING_ARRANGE: '待安排',
  ARRANGED: '已安排',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  NO_SHOW: '未到场',
}

export function roundTabLabel(round: InterviewRound): string {
  if (round === 'INITIAL') return '初面'
  if (round === 'SECOND') return '复试'
  return '终面'
}

export function interviewStatusLabel(status?: string): string {
  return STATUS_LABEL[status || ''] || status || '—'
}

export function describeInterviewState(interview: {
  round?: string
  status?: string
  evaluation?: unknown
}): RoundStateInfo {
  const round = (interview.round || 'INITIAL').toUpperCase()
  const roundName = roundTabLabel(round as InterviewRound)
  const status = interview.status || 'PENDING_ARRANGE'

  if (status === 'COMPLETED') {
    if (interview.evaluation) {
      return {
        label: `${roundName}已完成`,
        phase: '已提交反馈',
        tagType: 'success',
      }
    }
    return {
      label: `${roundName}已完成`,
      phase: '待提交反馈',
      tagType: 'warning',
      nextAction: '提交反馈',
    }
  }
  if (status === 'IN_PROGRESS') {
    return { label: `${roundName}进行中`, phase: '面试进行中', tagType: 'primary' }
  }
  if (status === 'ARRANGED') {
    return { label: `${roundName}已安排`, phase: '等待面试', tagType: 'info', nextAction: '查看日历' }
  }
  if (status === 'PENDING_ARRANGE') {
    return { label: `${roundName}待安排`, phase: '需安排面试时间', tagType: 'info', nextAction: '安排面试' }
  }
  return { label: `${roundName} · ${interviewStatusLabel(status)}`, phase: '', tagType: 'info' }
}

/** 初面通过且复试未开始时，提示安排复试 */
export function needsSecondRound(initial?: any, second?: any): boolean {
  if (!initial) return false
  if (initial.status !== 'COMPLETED' || !initial.evaluation) return false
  const decision = (initial.evaluation as any)?.decision
  if (decision !== 'PASS') return false
  if (!second) return true
  return second.status === 'PENDING_ARRANGE'
}
