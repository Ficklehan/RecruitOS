import request from '../request'

export function getReferralList(params?: any) {
  return request.get('/api/referral/list', { params })
}

export function createReferral(data: any) {
  return request.post('/api/referral', data)
}

export function getReferralRewardList(params?: any) {
  return request.get('/api/referral/reward/list', { params })
}

export function approveReferral(id: number) {
  return request.post(`/api/referral/${id}/approve`)
}

export function rejectReferral(id: number, reason: string) {
  return request.post(`/api/referral/${id}/reject`, { reason })
}
