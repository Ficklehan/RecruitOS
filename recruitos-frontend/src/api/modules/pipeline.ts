import request from '../request'

export function getPipelineBoard(jobId?: number) {
  return request.get('/api/pipeline/board', { params: { jobId } }) as any
}

export function advancePipelineStage(candidateJobId: number, data: Record<string, unknown>) {
  return request.put(`/api/pipeline/candidate-jobs/${candidateJobId}/stage`, data) as any
}

export function getCandidate360(candidateId: number) {
  return request.get(`/api/pipeline/candidates/${candidateId}/360`) as any
}

export function saveHiringDecision(data: Record<string, unknown>) {
  return request.post('/api/pipeline/hiring-decisions', data) as any
}
