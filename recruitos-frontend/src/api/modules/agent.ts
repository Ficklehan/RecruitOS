import request from '../request'

// ── 平台账号 ──────────────────────────
export function getAgentAccountList(params?: any) {
  return request.get('/api/agent/account/list', { params })
}

export function createAgentAccount(data: any) {
  return request.post('/api/agent/account', data)
}

export function updateAgentAccount(id: number | string, data: any) {
  return request.put(`/api/agent/account/${id}`, data)
}

export function deleteAgentAccount(id: number | string) {
  return request.delete(`/api/agent/account/${id}`)
}

export function testAgentAccount(id: number | string) {
  return request.post(`/api/agent/account/${id}/test`)
}

// ── Agent任务 ──────────────────────────
export function getAgentTaskList(params?: any) {
  return request.get('/api/agent/task/list', { params })
}

export function createAgentTask(data: any) {
  return request.post('/api/agent/task', data)
}

export function stopAgentTask(id: number) {
  return request.post(`/api/agent/task/${id}/stop`)
}

// ── Agent日志 ──────────────────────────
export function getAgentLogList(params?: any) {
  return request.get('/api/agent/log/list', { params })
}

// ── AI工作流 ──────────────────────────
export function createWorkflow(data: any) {
  return request.post('/api/agent/workflow', data)
}

export function getWorkflowList(params?: any) {
  return request.get('/api/agent/workflow/list', { params })
}

export function getWorkflowDetail(id: number | string) {
  return request.get(`/api/agent/workflow/${id}`)
}

export function getWorkflowStats(id: number | string) {
  return request.get(`/api/agent/workflow/${id}/stats`)
}

export function pauseWorkflow(id: number | string) {
  return request.post(`/api/agent/workflow/${id}/pause`)
}

export function resumeWorkflow(id: number | string) {
  return request.post(`/api/agent/workflow/${id}/resume`)
}
