import { getJobList } from './job'
import { getWorkflowDetail, getWorkflowList } from './agent'
import { getFunnelData } from './analytics'
import { getEvolutionProposalList } from './evolution'
import { getJobHealth } from './evolution'
import { loadInboxConfirmItems } from './workspace'

export interface DashboardAlert {
  id: string
  level: 'danger' | 'warning' | 'info'
  title: string
  path: string
}

export interface SourcingFunnelCell {
  label: string
  count: number
}

export async function loadDashboardSourcingFunnel(): Promise<SourcingFunnelCell[]> {
  const totals = { published: 0, searched: 0, greeted: 0, imported: 0 }
  try {
    const jobsRes: any = await getJobList({ pageNum: 1, pageSize: 10, status: 'ACTIVE' })
    const jobs = jobsRes.data?.list || jobsRes.data?.records || []
    for (const job of jobs) {
      const wfRes: any = await getWorkflowList({ jobId: job.id }).catch(() => ({ data: [] }))
      const workflows = wfRes.data || []
      const active = workflows.find((w: any) => w.status === 'RUNNING' || w.status === 'PAUSED')
      if (!active?.id) continue
      const detailRes: any = await getWorkflowDetail(active.id).catch(() => null)
      const stats = detailRes?.data?.stats || active.stats || {}
      totals.published += stats.published || 0
      totals.searched += stats.searched || 0
      totals.greeted += stats.greeted || 0
      totals.imported += stats.imported || 0
    }
  } catch {
    // ignore
  }
  return [
    { label: '已发布', count: totals.published },
    { label: '已检索', count: totals.searched },
    { label: '已联系', count: totals.greeted },
    { label: '已纳入', count: totals.imported },
  ]
}

export async function loadDashboardAlerts(): Promise<DashboardAlert[]> {
  const alerts: DashboardAlert[] = []
  try {
    const [confirmItems, proposalRes, jobsRes] = await Promise.all([
      loadInboxConfirmItems(),
      getEvolutionProposalList({ status: 'PENDING', pageNum: 1, pageSize: 10 }).catch(() => ({ data: { list: [] } })),
      getJobList({ pageNum: 1, pageSize: 10, status: 'ACTIVE' }),
    ])

    if (confirmItems.length) {
      alerts.push({
        id: 'confirm-pending',
        level: 'warning',
        title: `${confirmItems.length} 项平台招人待你确认`,
        path: '/workspace/inbox?tab=confirm',
      })
    }

    const proposals = proposalRes.data?.list || []
    if (proposals.length) {
      alerts.push({
        id: 'evolution-pending',
        level: 'info',
        title: `${proposals.length} 条招人方式建议待处理`,
        path: '/planning/evolution/proposals',
      })
    }

    const jobs = jobsRes.data?.list || jobsRes.data?.records || []
    for (const job of jobs.slice(0, 5)) {
      const wfRes: any = await getWorkflowList({ jobId: job.id }).catch(() => ({ data: [] }))
      const active = (wfRes.data || []).find((w: any) => w.status === 'RUNNING')
      if (active?.id) {
        const detailRes: any = await getWorkflowDetail(active.id).catch(() => null)
        const pending = detailRes?.data?.pendingActions?.length || 0
        if (pending > 0) {
          alerts.push({
            id: `job-action-${job.id}`,
            level: 'danger',
            title: `「${job.title}」有 ${pending} 项需处理`,
            path: `/planning/jobs/${job.id}?tab=sourcing`,
          })
        }
      }
      try {
        const healthRes: any = await getJobHealth(job.id)
        const health = healthRes.data || healthRes
        if (health?.status === 'CRITICAL') {
          alerts.push({
            id: `health-${job.id}`,
            level: 'danger',
            title: `「${job.title}」招聘健康度偏低（${health.score ?? '—'}分）`,
            path: '/planning/evolution/health',
          })
        }
      } catch {
        // ignore health errors
      }
    }
  } catch {
    // ignore
  }
  return alerts.slice(0, 6)
}

export async function loadPipelineFunnel() {
  try {
    const res: any = await getFunnelData()
    return res.data?.stages || []
  } catch {
    return []
  }
}
