import request from '../request'

export function getUserList(params?: any) {
  return request.get('/api/user/list', { params }) as any
}

export function getUserRoles(userId: number) {
  return request.get(`/api/user/${userId}/roles`) as any
}

export function resetUserPassword(id: number, password: string) {
  return request.put(`/api/user/${id}/password`, { password }) as any
}

export function assignUserRoles(userId: number, roleIds: number[]) {
  return request.post(`/api/user/${userId}/roles`, { roleIds }) as any
}
