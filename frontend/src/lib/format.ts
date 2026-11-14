import type { Priority, Status } from '../types'

export const statuses: Record<Status, string> = {
  NEW: 'جديد',
  IN_PROGRESS: 'قيد المعالجة',
  RESOLVED: 'تم الحل',
}
export const priorities: Record<Priority, string> = {
  HIGH: 'عالية',
  MEDIUM: 'متوسطة',
  LOW: 'منخفضة',
}
export const number = (value: number) => new Intl.NumberFormat('ar-SA').format(value)
export const date = (value: string) =>
  new Intl.DateTimeFormat('ar-SA', {
    calendar: 'gregory',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
export const shortId = (id: string) => 'RO-' + id.slice(0, 8).toUpperCase()
