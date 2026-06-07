import request from '../request'

// Auth
export function platformLoginApi(data: { username: string; password: string }) {
  return request.post('/api/platform/login', data) as any
}
export function platformLogoutApi() {
  return request.post('/api/platform/logout') as any
}
export function getPlatformAdminInfo() {
  return request.get('/api/platform/me') as any
}

// Tenant management
export function getPlatformTenantList(params?: any) {
  return request.get('/api/platform/tenants', { params }) as any
}
export function getPlatformTenantDetail(id: number) {
  return request.get(`/api/platform/tenants/${id}`) as any
}
export function createPlatformTenant(data: any) {
  return request.post('/api/platform/tenants', data) as any
}
export function updateTenantStatus(id: number, status: number) {
  return request.put(`/api/platform/tenants/${id}/status`, { status }) as any
}
export function updateTenantPlan(id: number, plan: string) {
  return request.put(`/api/platform/tenants/${id}/plan`, { plan }) as any
}
export function getTenantLicenseDetail(id: number) {
  return request.get(`/api/platform/tenants/${id}/license`) as any
}
export function updateTenantLicense(id: number, data: any) {
  return request.put(`/api/platform/tenants/${id}/license`, data) as any
}
