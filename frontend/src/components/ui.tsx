import { ArrowLeft, CloudRain, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { date, priorities, shortId, statuses } from '../lib/format'
import type { Incident, Priority, Status } from '../types'

export function Badge({ status, priority }: { status?: Status; priority?: Priority }) {
  return (
    <span className={'badge ' + (status || priority)}>
      <span className="badge-dot" />
      {status ? statuses[status] : priorities[priority!]}
    </span>
  )
}

export function Loading() {
  return (
    <div className="state" role="status">
      <RefreshCw className="spin" size={25} />
      <p>جارٍ تحميل البيانات…</p>
    </div>
  )
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="state error-state" role="alert">
      <CloudRain size={32} />
      <h3>تعذّر عرض البيانات</h3>
      <p>{message}</p>
      {retry && (
        <button className="button secondary" onClick={retry}>
          <RefreshCw size={16} />
          إعادة المحاولة
        </button>
      )}
    </div>
  )
}

export function Empty({ filtered = false }: { filtered?: boolean }) {
  return (
    <div className="state">
      <span className="empty-icon">
        <CloudRain size={32} />
      </span>
      <h3>{filtered ? 'لا توجد بلاغات مطابقة' : 'لا توجد بلاغات مسجلة'}</h3>
      <p>
        {filtered
          ? 'جرّب تغيير كلمات البحث أو إزالة الفلاتر.'
          : 'عند تسجيل بلاغ، سيظهر هنا مع حالته وأولويته.'}
      </p>
      {!filtered && (
        <Link className="button" to="/incidents/new">
          إنشاء أول بلاغ
          <ArrowLeft size={16} />
        </Link>
      )}
    </div>
  )
}

export function IncidentTable({ incidents }: { incidents: Incident[] }) {
  return (
    <div className="table-wrap">
      <table className="incident-table">
        <thead>
          <tr>
            <th>البلاغ</th>
            <th>الحي</th>
            <th>الأولوية</th>
            <th>الحالة</th>
            <th>تاريخ الإنشاء</th>
            <th>
              <span className="sr-only">التفاصيل</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((incident) => (
            <tr key={incident.id}>
              <td data-label="البلاغ">
                <Link className="incident-title" to={'/incidents/' + incident.id}>
                  {incident.title}
                </Link>
                <span className="record-id" dir="ltr">
                  {shortId(incident.id)}
                </span>
              </td>
              <td data-label="الحي">{incident.district}</td>
              <td data-label="الأولوية">
                <Badge priority={incident.priority} />
              </td>
              <td data-label="الحالة">
                <Badge status={incident.status} />
              </td>
              <td data-label="تاريخ الإنشاء" className="date-cell">
                {date(incident.createdAt)}
              </td>
              <td>
                <Link
                  className="icon-link"
                  to={'/incidents/' + incident.id}
                  aria-label={'تفاصيل ' + incident.title}
                >
                  <ArrowLeft size={18} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
