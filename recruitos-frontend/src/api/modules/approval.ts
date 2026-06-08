import request from '../request'

export function getMyApprovals(params?: { pageNum?: number; pageSize?: number }) {
  return request.get('/api/approval/my', { params }) as any
}

export function approveDemand(instanceId: number, comment?: string) {
  return request.post(`/api/approval/${instanceId}/approve`, null, { params: { comment } }) as any
}

export function rejectDemand(instanceId: number, comment?: string) {
  return request.post(`/api/approval/${instanceId}/reject`, null, { params: { comment } }) as any
}
