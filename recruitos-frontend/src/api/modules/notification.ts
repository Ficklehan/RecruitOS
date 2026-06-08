import request from '../request'

export function getMyNotifications(limit = 20) {
  return request.get('/api/notification/my', { params: { limit } }) as any
}

export function markNotificationRead(id: number) {
  return request.put(`/api/notification/${id}/read`) as any
}
