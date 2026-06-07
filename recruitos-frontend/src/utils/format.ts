import dayjs from 'dayjs'

/**
 * 格式化日期
 */
export function formatDate(date: string | Date | undefined, format: string = 'YYYY-MM-DD'): string {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date | undefined): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化金额
 */
export function formatMoney(amount: number | undefined, prefix: string = '¥'): string {
  if (amount === undefined || amount === null) return '-'
  return `${prefix}${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/**
 * 格式化百分比
 */
export function formatPercent(value: number | undefined, decimals: number = 1): string {
  if (value === undefined || value === null) return '-'
  return `${value.toFixed(decimals)}%`
}

/**
 * 状态标签映射
 */
const statusMap: Record<string, { label: string; type: string }> = {
  active: { label: '启用', type: 'success' },
  inactive: { label: '禁用', type: 'danger' },
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  draft: { label: '草稿', type: 'info' },
  published: { label: '已发布', type: 'success' },
  closed: { label: '已关闭', type: 'info' },
  interviewing: { label: '面试中', type: 'primary' },
  offer: { label: '已发Offer', type: 'warning' },
  onboarded: { label: '已入职', type: 'success' },
  withdrawn: { label: '已撤回', type: 'danger' },
}

/**
 * 格式化状态
 */
export function formatStatus(status: string): { label: string; type: string } {
  return statusMap[status] || { label: status, type: 'info' }
}

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number = 50): string {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
