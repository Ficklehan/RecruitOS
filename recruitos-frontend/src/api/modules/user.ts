import request from '../request'

export function getUserList(params?: any) {
  return request.get('/api/user/list', { params }) as any
}

export function getUserDetail(id: number) {
  return request.get(`/api/user/${id}`) as any
}

export function createUser(data: any) {
  return request.post('/api/user', data) as any
}

export function updateUser(id: number, data: any) {
  return request.put(`/api/user/${id}`, data) as any
}

export function deleteUser(id: number) {
  return request.delete(`/api/user/${id}`) as any
}

export function resetUserPassword(id: number) {
  return request.post(`/api/user/${id}/reset-password`) as any
}

export function assignUserRoles(userId: number, roleIds: number[]) {
  return request.put(`/api/user/${userId}/roles`, { roleIds }) as any
}
