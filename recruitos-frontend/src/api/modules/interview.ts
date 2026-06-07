import request from '../request'

// 安排面试
export function arrangeInterview(data: any) {
  return request.post('/api/interview', data)
}

// 获取面试详情
export function getInterviewDetail(id: number) {
  return request.get(`/api/interview/${id}`)
}

// 获取面试列表
export function getInterviewList(params: any) {
  return request.get('/api/interview/list', { params })
}

// 确认面试
export function confirmInterview(id: number, slotId: number) {
  return request.post(`/api/interview/${id}/confirm`, { slotId })
}

// 开始面试
export function startInterview(id: number) {
  return request.post(`/api/interview/${id}/start`)
}

// 完成面试
export function completeInterview(id: number) {
  return request.post(`/api/interview/${id}/complete`)
}

// 取消面试
export function cancelInterview(id: number, reason: string) {
  return request.post(`/api/interview/${id}/cancel`, { reason })
}

// 获取面试日历
export function getInterviewCalendar(params: any) {
  return request.get('/api/interview/calendar', { params })
}

// 触发下一轮面试
export function triggerNextRound(id: number) {
  return request.post(`/api/interview/${id}/trigger-next`)
}

// 提交评价
export function submitEvaluation(data: any) {
  return request.post('/api/evaluation', data)
}

// 获取评价
export function getEvaluation(interviewId: number) {
  return request.get(`/api/evaluation/interview/${interviewId}`)
}
