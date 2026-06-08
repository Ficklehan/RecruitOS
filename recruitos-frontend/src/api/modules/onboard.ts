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

export function updateOnboardStatus(id: number, status: string) {
  return request.put(`/api/onboard/${id}/status`, null, { params: { status } })
}

export function getOnboardTasks(onboardId: number) {
  return request.get(`/api/onboard/${onboardId}/tasks`)
}

export function createOnboardTask(data: any) {
  return request.post('/api/onboard/task', data)
}

export function updateOnboardTaskStatus(taskId: number, status: string) {
  return request.put(`/api/onboard/task/${taskId}/status`, null, { params: { status } })
}
