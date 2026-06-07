import request from '../request'

// 需求列表
export function getDemandList(params: any) {
  return request.get('/api/demand/list', { params })
}

// 需求详情
export function getDemandDetail(id: number) {
  return request.get(`/api/demand/${id}`)
}

// 创建需求
export function createDemand(data: any) {
  return request.post('/api/demand', data)
}

// 更新需求
export function updateDemand(id: number, data: any) {
  return request.put(`/api/demand/${id}`, data)
}

// 提交审批
export function submitDemand(id: number) {
  return request.post(`/api/demand/${id}/submit`)
}

// 关闭需求
export function closeDemand(id: number) {
  return request.post(`/api/demand/${id}/close`)
}

// 需求看板
export function getDemandBoard() {
  return request.get('/api/demand/board')
}

// 我的审批列表
export function getMyApprovals(params: any) {
  return request.get('/api/approval/my', { params })
}

// 审批详情
export function getApprovalDetail(instanceId: number) {
  return request.get(`/api/approval/${instanceId}`)
}

// 通过审批
export function approveDemand(instanceId: number, comment?: string) {
  return request.post(`/api/approval/${instanceId}/approve`, { comment })
}

// 驳回审批
export function rejectDemand(instanceId: number, comment: string) {
  return request.post(`/api/approval/${instanceId}/reject`, { comment })
}
