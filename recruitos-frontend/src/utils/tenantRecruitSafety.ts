/** 租户级渠道招聘安全设置（前端持久化，P1；后续可接 tenant API） */

const KEY_PREFIX = 'recruitos:tenant:'

export interface TenantRecruitSafety {
  allowFullAuto: boolean
  allowCardGreet: boolean
  defaultRunMode: 'SEMI_AUTO' | 'FULL_AUTO' | 'PUBLISH_SEARCH_ONLY'
}

const DEFAULTS: TenantRecruitSafety = {
  allowFullAuto: false,
  allowCardGreet: false,
  defaultRunMode: 'SEMI_AUTO',
}

function storageKey(tenantId?: string | number) {
  return `${KEY_PREFIX}${tenantId || 'default'}:recruitSafety`
}

export function loadTenantRecruitSafety(tenantId?: string | number): TenantRecruitSafety {
  try {
    const raw = localStorage.getItem(storageKey(tenantId))
    if (!raw) return { ...DEFAULTS }
    return { ...DEFAULTS, ...JSON.parse(raw) }
  } catch {
    return { ...DEFAULTS }
  }
}

export function saveTenantRecruitSafety(settings: TenantRecruitSafety, tenantId?: string | number) {
  localStorage.setItem(storageKey(tenantId), JSON.stringify(settings))
}
