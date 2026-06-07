import request from '../request'

export function getRoleList(params?: any) {
  return request.get('/api/role', { params }) as any
}

export function getRoleDetail(id: number) {
  return request.get(`/api/role/${id}`) as any
}

export function createRole(data: any) {
  return request.post('/api/role', data) as any
}

export function updateRole(id: number, data: any) {
  return request.put(`/api/role/${id}`, data) as any
}

export function deleteRole(id: number) {
  return request.delete(`/api/role/${id}`) as any
}

export function getRolePermissions(roleId: number) {
  return request.get(`/api/role/${roleId}/permissions`) as any
}

export function assignRolePermissions(roleId: number, permissionIds: number[]) {
  return request.put(`/api/role/${roleId}/permissions`, { permissionIds }) as any
}
