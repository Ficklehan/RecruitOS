import request from '../request'

export function getSimpleTenantList() {
  return request.get('/api/tenant/simple-list') as any
}

export function getTenantList(params?: any) {
  return request.get('/api/tenant/list', { params }) as any
}

export function getTenantDetail(id: number) {
  return request.get(`/api/tenant/${id}`) as any
}

export function createTenant(data: any) {
  return request.post('/api/tenant', data) as any
}

export function updateTenant(id: number, data: any) {
  return request.put(`/api/tenant/${id}`, data) as any
}

export function deleteTenant(id: number) {
  return request.delete(`/api/tenant/${id}`) as any
}
