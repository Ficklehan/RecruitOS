import request from '../request'

export function loginApi(data: { username: string; password: string; tenantId?: number }) {
  return request.post('/api/auth/login', data) as any
}

export function logoutApi() {
  return request.post('/api/auth/logout') as any
}

export function getCurrentUserApi() {
  return request.get('/api/auth/me') as any
}
