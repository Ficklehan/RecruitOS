import request from '../request'

// 上传简历
export function uploadResume(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/resume/upload', formData)
}

// 批量上传简历
export function batchUploadResumes(files: File[]) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post('/api/resume/batch-upload', formData)
}

// 简历列表
export function getResumeList(params: any) {
  return request.get('/api/resume/list', { params })
}

// 简历详情
export function getResumeDetail(id: number | string) {
  return request.get(`/api/resume/${id}`)
}

// AI解析简历
export function parseResume(id: number | string) {
  return request.post(`/api/resume/${id}/parse`)
}

// 更新简历
export function updateResume(id: number | string, data: any) {
  return request.put(`/api/resume/${id}`, data)
}

// 删除简历
export function deleteResume(id: number | string) {
  return request.delete(`/api/resume/${id}`)
}

// 导入人才库
export function importToTalentPool(id: number | string) {
  return request.post(`/api/resume/${id}/import`)
}

// 批量导入人才库
export function batchImportToTalentPool(ids: number[]) {
  return Promise.all(ids.map(id => request.post(`/api/resume/${id}/import`)))
}
