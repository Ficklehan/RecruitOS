import request from '../request'

// ===== Types =====

export interface CopilotMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
}

export interface QuickAction {
  label: string
  prompt: string
  action: string
  touchpoint: string
}

export interface ChatResponse {
  sessionId: string
  message: string
  suggestedActions: SuggestedAction[]
  confidence: number
  touchpointHint?: string
}

export interface SuggestedAction {
  label: string
  path: string
  type: 'navigate' | 'execute' | 'insert'
}

export interface DiagnoseResponse {
  sessionId: string
  diagnosis: string
  timestamp: number
}

export interface EvaluateResponse {
  sessionId: string
  evaluation: string
  timestamp: number
}

export interface SearchStrategyResponse {
  sessionId: string
  strategy: string
  timestamp: number
}

// ===== API =====

export type ApiResult<T> = { code: number; data: T; msg: string }

/** 多轮对话 */
export function copilotChat(params: {
  sessionId?: string
  message: string
  contextPage?: string
}) {
  return request.post('/api/copilot/chat', params) as Promise<ApiResult<ChatResponse>>
}

/** 需求诊断 */
export function copilotDiagnose(params: {
  sessionId?: string
  businessObjective: string
  departmentId?: number
}) {
  return request.post('/api/copilot/diagnose', params) as Promise<ApiResult<DiagnoseResponse>>
}

/** 候选人评估 */
export function copilotEvaluate(params: {
  sessionId?: string
  candidateInfo: string
  jobContext: string
}) {
  return request.post('/api/copilot/evaluate', params) as Promise<ApiResult<EvaluateResponse>>
}

/** 人才搜索策略 */
export function copilotSearchStrategy(params: {
  sessionId?: string
  jobDescription: string
  constraints?: string
}) {
  return request.post('/api/copilot/search-strategy', params) as Promise<ApiResult<SearchStrategyResponse>>
}

/** 获取会话历史 */
export function getCopilotHistory(sessionId: string) {
  return request.get(`/api/copilot/history/${sessionId}`) as Promise<ApiResult<CopilotMessage[]>>
}

/** 清空会话 */
export function clearCopilotSession(sessionId: string) {
  return request.delete(`/api/copilot/session/${sessionId}`) as Promise<ApiResult<{ status: string }>>
}

/** 获取 Quick Actions */
export function getQuickActions(contextPage?: string) {
  const params = contextPage ? `?contextPage=${encodeURIComponent(contextPage)}` : ''
  return request.get(`/api/copilot/quick-actions${params}`) as Promise<ApiResult<QuickAction[]>>
}
