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
