import request from '../request'

export function getEvolutionSignalList(params?: any) {
  return request.get('/api/evolution/signal/list', { params })
}

export function getWeightSnapshotList(params?: any) {
  if (params?.jobId) {
    return request.get(`/api/evolution/weight/${params.jobId}`)
  }
  return request.get('/api/evolution/signal/list', { params })
}

export function getAbTestList(params?: any) {
  return request.get('/api/evolution/abtest/list', { params })
}

export function getAbTestDetail(id: number) {
  return request.get(`/api/evolution/abtest/${id}`)
}

export function createAbTest(data: any) {
  return request.post('/api/evolution/abtest', data)
}

export function startAbTest(id: number) {
  return request.post(`/api/evolution/abtest/${id}/start`)
}

export function stopAbTest(id: number, winnerVariant: 'A' | 'B') {
  return request.post(`/api/evolution/abtest/${id}/stop`, null, { params: { winnerVariant } })
}

export function getJobHealth(jobId: number) {
  return request.get(`/api/evolution/health/job/${jobId}`)
}

export function getSystemHealth() {
  return request.get('/api/evolution/health/system')
}

export function getHealthAlerts() {
  return request.get('/api/evolution/health/alerts')
}

export function getJobWeightSnapshot(jobId: number) {
  return request.get(`/api/evolution/weight/${jobId}`)
}

export function getJobWeightHistory(jobId: number) {
  return request.get(`/api/evolution/weight/${jobId}/history`)
}

export function applySignal(id: number) {
  return request.post(`/api/evolution/trigger/${id}`)
}

export function getEvolutionProposalList(params?: { jobId?: number; status?: string; pageNum?: number; pageSize?: number }) {
  return request.get('/api/evolution/proposals', { params })
}

export function getEvolutionProposal(id: number) {
  return request.get(`/api/evolution/proposals/${id}`)
}

export function confirmEvolutionProposal(id: number) {
  return request.post(`/api/evolution/proposals/${id}/confirm`)
}

export function rejectEvolutionProposal(id: number, reason?: string) {
  return request.post(`/api/evolution/proposals/${id}/reject`, { reason })
}

export function getEvolutionSettings() {
  return request.get('/api/evolution/settings')
}

export function updateEvolutionMinSignals(minSignals: number) {
  return request.put('/api/evolution/settings/min-signals', { minSignals })
}
