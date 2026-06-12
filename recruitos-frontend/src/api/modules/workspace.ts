import { getMyApprovals } from './demand'
import { getInterviewList } from './interview'
import { getOfferList } from './offer'
import { getOnboardList } from './onboard'
import { getMyNotifications } from './notification'
import { getEvolutionProposalList } from './evolution'
import { getJobList } from './job'
import { getWorkflowList, getWorkflowDetail } from './agent'
import { OBJECTS, ACTIONS } from '@/constants/businessLabels'

export type InboxItemType =
  | 'confirm'
  | 'approval'
  | 'interview'
  | 'feedback'
  | 'offer'
  | 'onboard'
  | 'evolution'
  | 'message'

export interface InboxItem {
  id: string
  type: InboxItemType
  title: string
  path: string
  time: string
  /** 面试反馈抽屉用 */
  interview?: Record<string, unknown>
}

/** 收件箱 Tab 与 item.type 映射（v3.1 动词导向） */
export const INBOX_TAB_TYPES: Record<string, InboxItemType[] | 'all'> = {
  all: 'all',
  confirm: ['confirm'],
  interview: ['interview', 'feedback'],
  hiring: ['offer', 'onboard'],
  evolution: ['evolution'],
  approval: ['approval'],
}

export async function loadInboxConfirmItems(): Promise<InboxItem[]> {
  const items: InboxItem[] = []
  try {
    const jobsRes: any = await getJobList({ pageNum: 1, pageSize: 10, status: 'ACTIVE' })
    const jobs = jobsRes.data?.list || jobsRes.data?.records || []
    for (const job of jobs) {
      const wfRes: any = await getWorkflowList({ jobId: job.id }).catch(() => ({ data: [] }))
      const workflows = wfRes.data || []
      const running = workflows.find((w: any) => w.status === 'RUNNING' || w.status === 'PAUSED') || workflows[0]
      if (!running?.id) continue
      const detailRes: any = await getWorkflowDetail(running.id).catch(() => null)
      const detail = detailRes?.data || detailRes
      const pending = detail?.pendingActions || []
      for (const p of pending) {
        const isGreet = p.traceStatus === 'PENDING_GREET_CONFIRM'
        const isImport = p.traceStatus === 'PENDING_IMPORT'
        if (!isGreet && !isImport) continue
        items.push({
          id: `confirm-${p.id}`,
          type: 'confirm',
          title: isGreet
            ? `向 ${p.candidateName || '候选人'} 发送首次联系？ · ${job.title}`
            : `${ACTIONS.importCandidate}：${p.candidateName || '候选人'} · ${job.title}`,
          path: `/planning/jobs/${job.id}?tab=sourcing`,
          time: p.updatedAt || p.createdAt || '',
        })
      }
    }
  } catch {
    // ignore
  }
  return items
}

export async function loadInboxFeedbackItems(): Promise<InboxItem[]> {
  const items: InboxItem[] = []
  try {
    const res: any = await getInterviewList({ pageNum: 1, pageSize: 30, status: 'COMPLETED' })
    const list = res.data?.list || []
    for (const i of list) {
      if (i.evaluation) continue
      items.push({
        id: `feedback-${i.id}`,
        type: 'feedback',
        title: `待反馈：${i.candidateName || '候选人'} · ${i.jobTitle || '在招职位'}`,
        path: i.candidateId ? `/pipeline/candidates/${i.candidateId}` : '/workspace/inbox?tab=interview',
        time: i.scheduledEndTime || i.scheduledStartTime || i.updatedAt || '',
        interview: i,
      })
    }
  } catch {
    // ignore
  }
  return items
}

export async function loadInboxOnboardItems(): Promise<InboxItem[]> {
  const items: InboxItem[] = []
  try {
    const [offerRes, onboardRes] = await Promise.all([
      getOfferList({ pageNum: 1, pageSize: 30, status: 'ACCEPTED' }).catch(() => ({ data: { list: [] } })),
      getOnboardList({ pageNum: 1, pageSize: 100 }).catch(() => ({ data: { list: [] } })),
    ])
    const offers = offerRes.data?.list || []
    const onboards = onboardRes.data?.list || onboardRes.data?.records || []
    const coveredOfferIds = new Set(
      onboards.map((o: any) => o.offerId).filter(Boolean),
    )
    for (const o of offers) {
      if (coveredOfferIds.has(o.id)) continue
      items.push({
        id: `onboard-${o.id}`,
        type: 'onboard',
        title: `准备入职：${o.candidateName} · ${o.jobTitle}`,
        path: '/pipeline/onboards',
        time: o.updatedAt || o.createdAt || '',
      })
    }
  } catch {
    // ignore
  }
  return items
}

export async function loadPendingFeedbackInterviews(limit = 20) {
  try {
    const res: any = await getInterviewList({ pageNum: 1, pageSize: limit, status: 'COMPLETED' })
    return (res.data?.list || []).filter((i: any) => !i.evaluation)
  } catch {
    return []
  }
}

export async function loadInboxItems(): Promise<InboxItem[]> {
  const items: InboxItem[] = []

  try {
    const [confirmItems, feedbackItems, onboardItems, approvalRes, interviewRes, offerRes, evolutionRes, notifRes] =
      await Promise.all([
        loadInboxConfirmItems(),
        loadInboxFeedbackItems(),
        loadInboxOnboardItems(),
        getMyApprovals({ pageNum: 1, pageSize: 20 }).catch(() => ({ data: { list: [] } })),
        getInterviewList({ pageNum: 1, pageSize: 20, status: 'ARRANGED' }).catch(() => ({ data: { list: [] } })),
        getOfferList({ pageNum: 1, pageSize: 20, status: 'PENDING' }).catch(() => ({ data: { list: [] } })),
        getEvolutionProposalList({ status: 'PENDING', pageNum: 1, pageSize: 20 }).catch(() => ({ data: { list: [] } })),
        getMyNotifications(10).catch(() => ({ data: [] })),
      ])

    items.push(...confirmItems, ...feedbackItems, ...onboardItems)

    const approvals = approvalRes.data?.list || []
    for (const a of approvals) {
      items.push({
        id: `approval-${a.approvalInstanceId || a.id}`,
        type: 'approval',
        title: `招聘需求待审批：${a.title}`,
        path: '/planning/approvals/pending',
        time: a.updatedAt || a.createdAt || '',
      })
    }

    const interviews = interviewRes.data?.list || []
    for (const i of interviews) {
      items.push({
        id: `interview-${i.id}`,
        type: 'interview',
        title: `待面试：${i.candidateName || '候选人'} · ${i.jobTitle || '在招职位'}`,
        path: '/pipeline/calendar',
        time: i.scheduledStartTime || '',
      })
    }

    const offers = offerRes.data?.list || []
    for (const o of offers) {
      items.push({
        id: `offer-${o.id}`,
        type: 'offer',
        title: `录用待处理：${o.candidateName} · ${o.jobTitle}`,
        path: '/pipeline/offers/approvals',
        time: o.createdAt || '',
      })
    }

    const proposals = evolutionRes.data?.list || evolutionRes.data?.records || []
    for (const p of proposals) {
      items.push({
        id: `evolution-${p.id}`,
        type: 'evolution',
        title: `${OBJECTS.evolutionSuggestion}：${p.jobTitle || p.summary || '在招职位'}`,
        path: `/planning/evolution/proposals${p.id ? `?id=${p.id}` : ''}`,
        time: p.createdAt || p.updatedAt || '',
      })
    }

    const notifications = notifRes.data || []
    for (const n of notifications) {
      items.push({
        id: `msg-${n.id}`,
        type: 'message',
        title: n.title,
        path: '/workspace/inbox',
        time: n.createdAt || '',
      })
    }
  } catch {
    // ignore aggregate errors
  }

  return items.sort((a, b) => (b.time || '').localeCompare(a.time || ''))
}

export async function loadTodayInterviews() {
  try {
    const res: any = await getInterviewList({ pageNum: 1, pageSize: 50 })
    const list = res.data?.list || []
    const today = new Date().toISOString().slice(0, 10)
    return list.filter((i: any) => (i.scheduledStartTime || '').startsWith(today))
  } catch {
    return []
  }
}
