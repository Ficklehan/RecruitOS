import request from '../request'

// 候选人列表
export function getCandidateList(params: any) {
  return request.get('/api/candidate/list', { params })
}

// 候选人详情
export function getCandidateDetail(id: number) {
  return request.get(`/api/candidate/${id}`)
}

// 创建候选人
export function createCandidate(data: any) {
  return request.post('/api/candidate', data)
}

// 更新候选人
export function updateCandidate(id: number, data: any) {
  return request.put(`/api/candidate/${id}`, data)
}

// 关联岗位
export function addToJob(candidateId: number, jobId: number) {
  return request.post(`/api/candidate/${candidateId}/jobs/${jobId}`)
}

// 筛选评估
export function screening(candidateId: number, jobId: number, data: any) {
  return request.post(`/api/candidate/${candidateId}/jobs/${jobId}/screening`, data)
}

// 决策面板
export function getDecisionPanel(candidateId: number, jobId: number) {
  return request.get(`/api/candidate/${candidateId}/jobs/${jobId}/decision`)
}

// 人才库
export function getTalentPool(params: any) {
  return request.get('/api/candidate/talent-pool', { params })
}
