import request from '../request'

// 岗位列表
export function getJobList(params: any) {
  return request.get('/api/job/list', { params })
}

// 岗位详情
export function getJobDetail(id: number) {
  return request.get(`/api/job/${id}`)
}

// 创建岗位
export function createJob(data: any) {
  return request.post('/api/job', data)
}

// 更新岗位
export function updateJob(id: number, data: any) {
  return request.put(`/api/job/${id}`, data)
}

// 激活岗位
export function activateJob(id: number) {
  return request.post(`/api/job/${id}/activate`)
}

// 暂停岗位
export function pauseJob(id: number) {
  return request.post(`/api/job/${id}/pause`)
}

// 关闭岗位
export function closeJob(id: number, reason: string) {
  return request.post(`/api/job/${id}/close`, { reason })
}

// 解析JD
export function parseJd(id: number) {
  return request.post(`/api/job/${id}/parse-jd`)
}

// 获取标签
export function getTags(id: number) {
  return request.get(`/api/job/${id}/tags`)
}

// 更新标签
export function updateTags(id: number, data: any) {
  return request.put(`/api/job/${id}/tags`, data)
}

// 渠道运营包
export function generateOpsPack(jobId: number) {
  return request.post(`/api/job/${jobId}/ops-pack/generate`)
}

export function getActiveOpsPack(jobId: number) {
  return request.get(`/api/job/${jobId}/ops-pack/active`)
}

export function listOpsPackVersions(jobId: number) {
  return request.get(`/api/job/${jobId}/ops-pack/versions`)
}

export function getOpsPackById(jobId: number, packId: number) {
  return request.get(`/api/job/${jobId}/ops-pack/${packId}`)
}

export function updateOpsPack(jobId: number, packId: number, packBody: Record<string, unknown>) {
  return request.put(`/api/job/${jobId}/ops-pack/${packId}`, packBody)
}

export function confirmOpsPack(jobId: number, packId: number) {
  return request.post(`/api/job/${jobId}/ops-pack/${packId}/confirm`)
}
