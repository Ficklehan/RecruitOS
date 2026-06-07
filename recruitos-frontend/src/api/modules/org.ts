import request from '../request'

export function getOrgTree() {
  return request.get('/api/org/tree') as any
}

export function getOrgDetail(id: number) {
  return request.get(`/api/org/${id}`) as any
}

export function createOrg(data: any) {
  return request.post('/api/org', data) as any
}

export function updateOrg(id: number, data: any) {
  return request.put(`/api/org/${id}`, data) as any
}

export function deleteOrg(id: number) {
  return request.delete(`/api/org/${id}`) as any
}
