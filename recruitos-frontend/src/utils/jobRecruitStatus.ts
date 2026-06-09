import { getWorkflowDetail, getWorkflowList } from '@/api/modules/agent'
import { getActiveOpsPack } from '@/api/modules/job'

export interface JobRecruitStatus {
  label: string
  type: 'success' | 'warning' | 'danger' | 'info'
}

export async function resolveJobRecruitStatus(job: { id: number; status?: string }): Promise<JobRecruitStatus | null> {
  if (job.status !== 'ACTIVE') return null

  const [packRes, wfRes] = await Promise.all([
    getActiveOpsPack(job.id).catch(() => null),
    getWorkflowList({ jobId: job.id }).catch(() => ({ data: [] })),
  ])

  const pack = (packRes as any)?.data ?? packRes
  const workflows = (wfRes as any)?.data || []
  const active = workflows.find((w: any) => w.status === 'RUNNING' || w.status === 'PAUSED')

  if (active?.id) {
    const detailRes: any = await getWorkflowDetail(active.id).catch(() => null)
    const pending = detailRes?.data?.pendingActions?.length || 0
    if (pending > 0) return { label: '需处理', type: 'danger' }
    if (active.status === 'RUNNING') return { label: '运行中', type: 'success' }
    if (active.status === 'PAUSED') return { label: '已暂停', type: 'warning' }
  }

  if (!pack?.id && !pack?.packId) return { label: '待设置', type: 'info' }
  return { label: '待启动', type: 'info' }
}

export async function resolveJobRecruitStatuses(
  jobs: { id: number; status?: string }[],
): Promise<Map<number, JobRecruitStatus | null>> {
  const map = new Map<number, JobRecruitStatus | null>()
  await Promise.all(
    jobs.map(async (job) => {
      map.set(job.id, await resolveJobRecruitStatus(job))
    }),
  )
  return map
}
