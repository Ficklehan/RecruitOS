import request from '../request'

export function getPermissionTree() {
  return request.get('/api/permission/tree') as any
}

export function getPermissionList(params?: any) {
  return request.get('/api/permission/list', { params }) as any
}

export function createPermission(data: any) {
  return request.post('/api/permission', data) as any
}

export function updatePermission(id: number, data: any) {
  return request.put(`/api/permission/${id}`, data) as any
}

export function deletePermission(id: number) {
  return request.delete(`/api/permission/${id}`) as any
}
