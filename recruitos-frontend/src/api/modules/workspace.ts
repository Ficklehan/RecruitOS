import { getMyApprovals } from './approval'
import { getInterviewList } from './interview'
import { getOfferList } from './offer'
import { getMyNotifications } from './notification'

export interface InboxItem {
  id: string
  type: 'approval' | 'interview' | 'offer' | 'message'
  title: string
  path: string
  time: string
}

export async function loadInboxItems(): Promise<InboxItem[]> {
  const items: InboxItem[] = []

  try {
    const [approvalRes, interviewRes, offerRes, notifRes] = await Promise.all([
      getMyApprovals({ pageNum: 1, pageSize: 20 }).catch(() => ({ data: { list: [] } })),
      getInterviewList({ pageNum: 1, pageSize: 20, status: 'ARRANGED' }).catch(() => ({ data: { list: [] } })),
      getOfferList({ pageNum: 1, pageSize: 20, status: 'PENDING' }).catch(() => ({ data: { list: [] } })),
      getMyNotifications(10).catch(() => ({ data: [] })),
    ])

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
        title: `录用通知待审批：${o.candidateName} · ${o.jobTitle}`,
        path: '/pipeline/offers/approvals',
        time: o.createdAt || '',
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

  return items
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
