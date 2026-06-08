import request from '../request'

export function getChannelList(params?: Record<string, unknown>) {
  return request.get('/api/agent/channel/list', { params })
}

export function getChannelDetail(id: number | string) {
  return request.get(`/api/agent/channel/${id}`)
}

export function getChannelStats() {
  return request.get('/api/agent/channel/stats')
}

export function getAgentChannelOptions() {
  return request.get('/api/agent/channel/agent-options')
}

export function createChannel(data: Record<string, unknown>) {
  return request.post('/api/agent/channel', data)
}

export function updateChannel(id: number | string, data: Record<string, unknown>) {
  return request.put(`/api/agent/channel/${id}`, data)
}

export function updateChannelStatus(id: number | string, status: string) {
  return request.put(`/api/agent/channel/${id}/status`, null, { params: { status } })
}

export function deleteChannel(id: number | string) {
  return request.delete(`/api/agent/channel/${id}`)
}
