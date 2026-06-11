import request from '../request'

export function getReferralList(params?: any) {
  return request.get('/api/referral/list', { params })
}

export function createReferral(data: any) {
  return request.post('/api/referral', data)
}

export function createReferralShareLink(data: { jobId: number; referrerId?: number; referrerName?: string }) {
  return request.post('/api/referral/link', data)
}

export function getReferralLinkInfo(token: string) {
  return request.get(`/api/referral/public/link/${token}`)
}

export function submitReferralPublic(data: {
  token: string
  candidateName: string
  phone?: string
  email?: string
  remark?: string
}) {
  return request.post('/api/referral/public/submit', data)
}

export function getReferralRewardList(params?: any) {
  return request.get('/api/referral/reward/list', { params })
}

export function getReferralRewardStats() {
  return request.get('/api/referral/reward/stats')
}

export function approveReferralReward(id: number) {
  return request.post(`/api/referral/reward/${id}/approve`)
}

export function payReferralReward(id: number) {
  return request.post(`/api/referral/reward/${id}/pay`)
}
