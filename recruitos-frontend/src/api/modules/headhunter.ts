import request from '../request'

export function getHeadhunterVendorList(params?: any) {
  return request.get('/api/headhunter/vendor/list', { params })
}

export function getHeadhunterRecommendationList(params?: any) {
  return request.get('/api/headhunter/recommendation/list', { params })
}

export function getHeadhunterPerformance(params?: any) {
  return request.get('/api/headhunter/recommendation/stats', { params })
}

export function createHeadhunterVendor(data: any) {
  return request.post('/api/headhunter/vendor', data)
}

export function updateHeadhunterVendor(id: number, data: any) {
  return request.put(`/api/headhunter/vendor/${id}`, data)
}
