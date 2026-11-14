export type Status = 'NEW' | 'IN_PROGRESS' | 'RESOLVED'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Incident {
  id: string
  title: string
  description: string
  district: string
  locationDescription: string
  latitude: number | null
  longitude: number | null
  priority: Priority
  status: Status
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export type IncidentInput = Pick<
  Incident,
  | 'title'
  | 'description'
  | 'district'
  | 'locationDescription'
  | 'latitude'
  | 'longitude'
  | 'priority'
>

export interface IncidentPage {
  content: Incident[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface HistoryEntry {
  id: string
  incidentId: string
  fromStatus: Status | null
  toStatus: Status
  note: string | null
  changedAt: string
}

export interface Summary {
  total: number
  byStatus: Record<Status, number>
  byPriority: Record<Priority, number>
}
