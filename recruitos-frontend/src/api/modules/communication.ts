import request from '../request'

export function getTemplateList(params?: any) {
  return request.get('/api/communication/template/list', { params })
}

export function createTemplate(data: any) {
  return request.post('/api/communication/template', data)
}

export function updateTemplate(id: number, data: any) {
  return request.put(`/api/communication/template/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/api/communication/template/${id}`)
}

export function getConversationList(params?: any) {
  return request.get('/api/communication/conversation/list', { params })
}

export function getConversationDetail(id: number) {
  return request.get(`/api/communication/conversation/${id}`)
}

export function sendMessage(conversationId: number, data: { content: string; templateId?: number }) {
  return request.post(`/api/communication/conversation/${conversationId}/send`, null, {
    params: { content: data.content, templateId: data.templateId },
  })
}

export function getSafetyStats() {
  return request.get('/api/communication/safety/stats')
}

export function reviewSafetyLog(id: number, action: string) {
  return request.post(`/api/communication/safety/${id}/review`, null, { params: { action } })
}

export function getSafetyLogList(params?: any) {
  return request.get('/api/communication/safety/list', { params })
}
