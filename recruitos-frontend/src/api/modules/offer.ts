import request from '../request'

export function getOfferList(params: any) {
  return request.get('/api/offer/list', { params })
}

export function getOfferDetail(id: number) {
  return request.get(`/api/offer/${id}`)
}

export function createOffer(data: any) {
  return request.post('/api/offer', data)
}

export function updateOffer(id: number, data: any) {
  return request.put(`/api/offer/${id}`, data)
}

export function submitOfferApproval(id: number) {
  return request.post(`/api/offer/${id}/submit`)
}

export function approveOffer(id: number, comment?: string) {
  return request.post(`/api/offer/${id}/approve`, { comment })
}

export function rejectOffer(id: number, comment: string) {
  return request.post(`/api/offer/${id}/reject`, { comment })
}

export function sendOffer(id: number) {
  return request.post(`/api/offer/${id}/send`)
}

export function acceptOffer(id: number) {
  return request.post(`/api/offer/${id}/accept`)
}

export function rejectOfferByCandidate(id: number, reason: string) {
  return request.post(`/api/offer/${id}/reject-by-candidate`, { reason })
}

export function updateBgCheckStatus(id: number, status: string) {
  return request.put(`/api/offer/${id}/bg-check`, null, { params: { status } })
}
