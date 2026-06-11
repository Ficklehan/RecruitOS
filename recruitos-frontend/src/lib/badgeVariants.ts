export type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline'

const jobStatusMap: Record<string, BadgeVariant> = {
  DRAFT: 'secondary',
  ACTIVE: 'default',
  PAUSED: 'outline',
  CLOSED: 'secondary',
}

const demandStatusMap: Record<string, BadgeVariant> = {
  DRAFT: 'secondary',
  PENDING: 'outline',
  APPROVED: 'default',
  REJECTED: 'destructive',
  JOB_CREATED: 'default',
  RECRUITING: 'default',
  COMPLETED: 'secondary',
  CANCELLED: 'secondary',
}

const urgencyMap: Record<string, BadgeVariant> = {
  CRITICAL: 'destructive',
  URGENT: 'outline',
  HIGH: 'outline',
  NORMAL: 'secondary',
  LOW: 'secondary',
}

export function jobStatusBadge(status: string): BadgeVariant {
  return jobStatusMap[status] ?? 'secondary'
}

export function demandStatusBadge(status: string): BadgeVariant {
  return demandStatusMap[status] ?? 'secondary'
}

export function urgencyBadge(urgency: string): BadgeVariant {
  return urgencyMap[urgency] ?? 'secondary'
}

/** Maps legacy Element Plus el-tag `type` to shadcn Badge variant */
export function elTagTypeToBadge(type?: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    success: 'default',
    primary: 'default',
    warning: 'outline',
    danger: 'destructive',
    info: 'secondary',
  }
  return map[type ?? ''] ?? 'secondary'
}

const offerStatusMap: Record<string, BadgeVariant> = {
  DRAFT: 'secondary',
  PENDING: 'outline',
  APPROVED: 'default',
  REJECTED: 'destructive',
  SENT: 'default',
  ACCEPTED: 'default',
  DECLINED: 'destructive',
  EXPIRED: 'secondary',
}

const candidateStatusMap: Record<string, BadgeVariant> = {
  NEW: 'secondary',
  ACTIVE: 'default',
  HIRED: 'default',
  REJECTED: 'destructive',
  WITHDRAWN: 'secondary',
}

const resumeStatusMap: Record<string, BadgeVariant> = {
  PENDING: 'outline',
  PARSED: 'default',
  IMPORTED: 'default',
  FAILED: 'destructive',
}

export function offerStatusBadge(status: string): BadgeVariant {
  return offerStatusMap[status] ?? 'secondary'
}

export function candidateStatusBadge(status: string): BadgeVariant {
  return candidateStatusMap[status] ?? 'secondary'
}

export function resumeStatusBadge(status: string): BadgeVariant {
  return resumeStatusMap[status] ?? 'secondary'
}
