import request from '../request'

export function getEvolutionSignalList(params?: any) {
  return request.get('/api/evolution/signal/list', { params })
}

export function getWeightSnapshotList(params?: any) {
  return request.get('/api/evolution/weight/list', { params })
}

export function getAbTestList(params?: any) {
  return request.get('/api/evolution/abtest/list', { params })
}

export function getJobHealth(jobId: number) {
  return request.get(`/api/evolution/health/${jobId}`)
}

export function applySignal(id: number) {
  return request.post(`/api/evolution/signal/${id}/apply`)
}
