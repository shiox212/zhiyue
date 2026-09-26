export const formatDate = (value?: string | null) => {
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未知'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(date)
}

export const displayName = (name?: string | null) => name?.trim() || '未命名读者'

export const avatarLetter = (name?: string | null) => displayName(name).slice(0, 1).toUpperCase()

export const getErrorMessage = (error: unknown, fallback = '请求未能完成，请稍后重试') => {
  if (error instanceof Error && error.message) return error.message
  return fallback
}
