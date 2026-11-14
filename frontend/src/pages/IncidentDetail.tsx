import { useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Clock3,
  ExternalLink,
  MapPin,
  Pencil,
  RefreshCw,
} from 'lucide-react'
import { Badge, ErrorState, Loading } from '../components/ui'
import { useResource } from '../hooks/useResource'
import { api } from '../lib/api'
import { date, shortId, statuses } from '../lib/format'
import type { HistoryEntry, Incident, Status } from '../types'

export default function IncidentDetail() {
  const { id } = useParams()
  return <Detail key={id} id={id!} />
}

function Detail({ id }: { id: string }) {
  const location = useLocation()
  const [revision, setRevision] = useState(0)
  const [notice, setNotice] = useState<string>(location.state?.notice || '')
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const saving = useRef(false)
  const result = useResource<Incident>('/incidents/' + id, revision)
  const history = useResource<HistoryEntry[]>('/incidents/' + id + '/history', revision)
  async function changeStatus(target: Status) {
    if (saving.current) return
    saving.current = true
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await api<Incident>('/incidents/' + id + '/status', {
        method: 'PATCH',
        body: JSON.stringify({ status: target, note: note.trim() || null }),
      })
      setNotice('تم تحديث حالة البلاغ إلى «' + statuses[target] + '».')
      setNote('')
      setRevision((v) => v + 1)
    } catch (caught) {
      setError((caught as Error).message)
    } finally {
      saving.current = false
      setBusy(false)
    }
  }
  if (result.loading) return <Loading />
  if (result.error)
    return (
      <>
        <Link className="back-link" to="/incidents">
          <ArrowRight size={17} />
          العودة إلى البلاغات
        </Link>
        <ErrorState message={result.error.message} retry={() => setRevision((v) => v + 1)} />
      </>
    )
  const incident = result.data!
  const next: Status = incident.status === 'IN_PROGRESS' ? 'RESOLVED' : 'IN_PROGRESS'
  const action =
    incident.status === 'NEW'
      ? 'بدء المعالجة'
      : incident.status === 'IN_PROGRESS'
        ? 'تأكيد حل البلاغ'
        : 'إعادة فتح البلاغ'
  return (
    <>
      <Link className="back-link" to="/incidents">
        <ArrowRight size={17} />
        العودة إلى البلاغات
      </Link>
      <div className="page-heading detail-heading">
        <div>
          <div className="eyebrow" dir="ltr">
            {shortId(incident.id)}
          </div>
          <h1>{incident.title}</h1>
          <div className="detail-badges">
            <Badge status={incident.status} />
            <Badge priority={incident.priority} />
            <span>
              <MapPin size={15} />
              {incident.district}
            </span>
          </div>
        </div>
        <Link className="button secondary" to={'/incidents/' + id + '/edit'}>
          <Pencil size={16} />
          تعديل التفاصيل
        </Link>
      </div>
      {notice && (
        <div className="success-notice" role="status">
          <CheckCircle2 size={18} />
          {notice}
        </div>
      )}
      <div className="detail-grid">
        <div className="detail-main">
          <section className="panel detail-panel">
            <h2>تفاصيل البلاغ</h2>
            <p className="body-copy">{incident.description}</p>
            <div className="location-block">
              <span className="soft-icon">
                <MapPin size={21} />
              </span>
              <div>
                <h3>{incident.district}</h3>
                <p>{incident.locationDescription}</p>
                {incident.latitude !== null && incident.longitude !== null && (
                  <>
                    <div className="coordinate-value" dir="ltr">
                      {incident.latitude}, {incident.longitude}
                    </div>
                    <a
                      className="text-link"
                      target="_blank"
                      rel="noopener noreferrer"
                      href={
                        'https://www.google.com/maps/search/?api=1&query=' +
                        encodeURIComponent(incident.latitude + ',' + incident.longitude)
                      }
                    >
                      عرض الموقع على الخريطة
                      <ExternalLink size={14} />
                    </a>
                  </>
                )}
              </div>
            </div>
            <dl className="metadata">
              <div>
                <dt>تاريخ الإنشاء</dt>
                <dd>{date(incident.createdAt)}</dd>
              </div>
              <div>
                <dt>آخر تحديث</dt>
                <dd>{date(incident.updatedAt)}</dd>
              </div>
              {incident.resolvedAt && (
                <div>
                  <dt>تاريخ الحل</dt>
                  <dd>{date(incident.resolvedAt)}</dd>
                </div>
              )}
            </dl>
          </section>
          <section className="panel detail-panel">
            <div className="panel-heading compact">
              <div>
                <h2>سجل الحالة</h2>
                <p>تسلسل مراحل معالجة البلاغ</p>
              </div>
              <Clock3 size={21} />
            </div>
            {history.loading ? (
              <Loading />
            ) : history.error ? (
              <ErrorState message={history.error.message} retry={() => setRevision((v) => v + 1)} />
            ) : (
              <ol className="timeline">
                {history.data?.map((entry) => (
                  <li key={entry.id}>
                    <span className={'timeline-dot ' + entry.toStatus} />
                    <div>
                      <h3>
                        {entry.fromStatus
                          ? statuses[entry.fromStatus] + ' ← ' + statuses[entry.toStatus]
                          : 'تم إنشاء البلاغ'}
                      </h3>
                      <time dateTime={entry.changedAt}>{date(entry.changedAt)}</time>
                      {entry.note && <p>{entry.note}</p>}
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </div>
        <aside>
          <section className="panel status-panel">
            <div className="panel-heading compact">
              <div>
                <h2>متابعة الحالة</h2>
                <p>الخطوة التالية لهذا البلاغ</p>
              </div>
              <span className="soft-icon">
                <RefreshCw size={19} />
              </span>
            </div>
            <div className="status-flow">
              <Badge status={incident.status} />
              <ArrowLeft size={16} />
              <Badge status={next} />
            </div>
            <p className="status-description">
              {incident.status === 'RESOLVED'
                ? 'إذا احتاج الموقع إلى متابعة إضافية، يمكنك إعادته إلى قيد المعالجة.'
                : incident.status === 'NEW'
                  ? 'ابدأ المعالجة عند بدء متابعة البلاغ والعمل على حلّه.'
                  : 'أكّد الحل بعد اكتمال معالجة تجمع المياه في الموقع.'}
            </p>
            <form
              onSubmit={(event) => {
                event.preventDefault()
                void changeStatus(next)
              }}
            >
              <label htmlFor="status-note">
                ملاحظة التحديث <span className="muted">(اختياري)</span>
              </label>
              <textarea
                id="status-note"
                value={note}
                onChange={(event) => setNote(event.target.value)}
                rows={4}
                maxLength={1000}
                placeholder="أضف ما تم إنجازه أو سبب تغيير الحالة…"
                disabled={busy}
              />
              <div className="character-count" dir="ltr">
                {note.length} / 1000
              </div>
              {error && (
                <div className="form-alert" role="alert">
                  {error}
                  <button
                    type="button"
                    className="text-link"
                    onClick={() => setRevision((v) => v + 1)}
                  >
                    تحديث بيانات البلاغ
                  </button>
                </div>
              )}
              <button className="button full" disabled={busy} type="submit">
                {busy ? 'جارٍ تحديث الحالة…' : action}
                <ArrowLeft size={17} />
              </button>
            </form>
          </section>
          <div className="detail-note">
            <CheckCircle2 size={18} />
            <p>كل تغيير في الحالة يُحفظ في السجل لتتمكن من متابعة رحلة البلاغ.</p>
          </div>
        </aside>
      </div>
    </>
  )
}
