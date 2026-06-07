import request from '../request'

export function getFunnelData(params?: { dateFrom?: string; dateTo?: string }) {
  return request.get('/api/analytics/funnel', { params })
}

export function getRoiData(params?: { dateFrom?: string; dateTo?: string }) {
  return request.get('/api/analytics/roi', { params })
}

export function getInterviewerData() {
  return request.get('/api/analytics/interviewer')
}

export function getCycleData(params?: { dateFrom?: string; dateTo?: string }) {
  return request.get('/api/analytics/cycle', { params })
}
