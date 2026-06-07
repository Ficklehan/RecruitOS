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

export function sendMessage(conversationId: number, data: any) {
  return request.post(`/api/communication/conversation/${conversationId}/message`, data)
}

export function getSafetyLogList(params?: any) {
  return request.get('/api/communication/safety/list', { params })
}
