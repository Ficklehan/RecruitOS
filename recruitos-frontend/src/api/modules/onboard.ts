import request from '../request'

export function getOnboardList(params: any) {
  return request.get('/api/onboard/list', { params })
}

export function getOnboardDetail(id: number) {
  return request.get(`/api/onboard/${id}`)
}

export function createOnboard(data: any) {
  return request.post('/api/onboard', data)
}

export function updateOnboard(id: number, data: any) {
  return request.put(`/api/onboard/${id}`, data)
}

export function confirmOnboard(id: number) {
  return request.post(`/api/onboard/${id}/confirm`)
}

export function completeOnboard(id: number) {
  return request.post(`/api/onboard/${id}/complete`)
}

export function getOnboardTasks(onboardId: number) {
  return request.get(`/api/onboard/${onboardId}/tasks`)
}

export function updateOnboardTask(onboardId: number, taskId: number, data: any) {
  return request.put(`/api/onboard/${onboardId}/tasks/${taskId}`, data)
}
