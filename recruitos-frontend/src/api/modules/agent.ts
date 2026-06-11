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
  return request.put(`/api/agent/account/${id}/status`, { status: 'DISABLED' })
}

export function testAgentAccount(id: number | string) {
  return request.post(`/api/agent/account/${id}/rpa-test`, null, { timeout: 120000 })
}

export function rpaLoginAccount(id: number | string) {
  return request.post(`/api/agent/account/${id}/rpa-login`, null, { timeout: 360000 })
}

export function rpaTestAccount(id: number | string) {
  return request.post(`/api/agent/account/${id}/rpa-test`, null, { timeout: 120000 })
}

export function rpaLogoutAccount(id: number | string) {
  return request.post(`/api/agent/account/${id}/rpa-logout`)
}

export function getRpaStatus() {
  return request.get('/api/agent/rpa/status')
}

export function lockRpaTesting() {
  return request.post('/api/agent/rpa/lock-testing')
}

export function unlockRpaLive(data: { confirm: boolean; reason?: string }) {
  return request.post('/api/agent/rpa/unlock-live', data)
}

export function resetRpaAccess() {
  return request.post('/api/agent/rpa/reset-access')
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

export function startAgentTask(id: number) {
  return request.post(`/api/agent/task/${id}/start`)
}

export function pauseAgentTask(id: number) {
  return request.post(`/api/agent/task/${id}/pause`)
}

export function resumeAgentTask(id: number) {
  return request.post(`/api/agent/task/${id}/resume`)
}

export function getAgentTaskDetail(id: number) {
  return request.get(`/api/agent/task/${id}`)
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

export function getWorkflowCandidates(id: number | string, params?: any) {
  return request.get(`/api/agent/workflow/${id}/candidates`, { params })
}

export function confirmWorkflowPublish(id: number | string) {
  return request.post(`/api/agent/workflow/${id}/confirm-publish`)
}

export function confirmWorkflowGreet(traceId: number | string) {
  return request.post(`/api/agent/workflow/trace/${traceId}/confirm-greet`)
}

export function confirmWorkflowImport(traceId: number | string) {
  return request.post(`/api/agent/workflow/trace/${traceId}/confirm-import`)
}

export function getChannelStagingList(params?: any) {
  return request.get('/api/agent/channel-staging', { params })
}

export function getChannelStagingDetail(id: number) {
  return request.get(`/api/agent/channel-staging/${id}`)
}

export function updateChannelStagingFields(id: number, fields: Record<string, unknown>) {
  return request.put(`/api/agent/channel-staging/${id}/fields`, fields)
}

export function askChannelStaging(id: number, question: string) {
  return request.post(`/api/agent/channel-staging/${id}/ask`, { question })
}

export function batchStagingGreet(ids: number[]) {
  return request.post('/api/agent/channel-staging/batch/greet', { ids })
}

export function batchStagingImport(ids: number[]) {
  return request.post('/api/agent/channel-staging/batch/import', { ids })
}

export function batchStagingReject(ids: number[], reason?: string) {
  return request.post('/api/agent/channel-staging/batch/reject', { ids, reason })
}
