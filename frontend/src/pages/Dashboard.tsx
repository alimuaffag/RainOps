import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Plus, RefreshCw } from 'lucide-react'
import { Empty, ErrorState, IncidentTable, Loading } from '../components/ui'
import { useResource } from '../hooks/useResource'
import { number, priorities } from '../lib/format'
import type { IncidentPage, Priority, Summary } from '../types'

export default function Dashboard() {
  const [revision, setRevision] = useState(0)
  const summary = useResource<Summary>('/dashboard/summary', revision)
  const recent = useResource<IncidentPage>('/incidents?page=0&size=5', revision)
  const cards = [
    {
      label: 'إجمالي البلاغات',
      value: summary.data?.total,
      tone: 'green',
      href: '/incidents',
      foot: 'كل البلاغات المسجلة',
    },
    {
      label: 'بلاغات جديدة',
      value: summary.data?.byStatus.NEW,
      tone: 'blue',
      href: '/incidents?status=NEW',
      foot: 'بانتظار بدء المعالجة',
    },
    {
      label: 'قيد المعالجة',
      value: summary.data?.byStatus.IN_PROGRESS,
      tone: 'amber',
      href: '/incidents?status=IN_PROGRESS',
      foot: 'تجري متابعة معالجتها',
    },
    {
      label: 'تم حلها',
      value: summary.data?.byStatus.RESOLVED,
      tone: 'mint',
      href: '/incidents?status=RESOLVED',
      foot: 'اكتملت أعمال المعالجة',
    },
  ]
  const total = summary.data?.total || 0
  const percent = total ? Math.round((summary.data!.byStatus.RESOLVED / total) * 100) : 0
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>ملخص البلاغات</h1>
          <p>أعداد البلاغات وحالة المعالجة.</p>
        </div>
        <div className="heading-actions">
          <button
            className="button secondary"
            onClick={() => setRevision((v) => v + 1)}
            aria-label="تحديث الإحصائيات"
          >
            <RefreshCw size={16} />
            تحديث
          </button>
          <Link className="button" to="/incidents/new">
            <Plus size={18} />
            بلاغ جديد
          </Link>
        </div>
      </div>
      {summary.error ? (
        <ErrorState message={summary.error.message} retry={() => setRevision((v) => v + 1)} />
      ) : (
        <div className="stats-grid" aria-busy={summary.loading}>
          {cards.map((card) => (
            <Link key={card.label} to={card.href} className={'stat-card ' + card.tone}>
              <div className="stat-top">
                <span>{card.label}</span>
              </div>
              <div className="stat-number">
                {card.value === undefined ? '—' : number(card.value)}
              </div>
              <span className="stat-foot">{card.foot}</span>
            </Link>
          ))}
        </div>
      )}
      <div className="dashboard-grid">
        <section className="panel recent-panel">
          <div className="panel-heading">
            <div>
              <h2>أحدث البلاغات</h2>
              <p>آخر ٥ بلاغات حسب تاريخ الإنشاء</p>
            </div>
            <Link className="text-link" to="/incidents">
              عرض الكل
              <ArrowLeft size={16} />
            </Link>
          </div>
          {recent.loading ? (
            <Loading />
          ) : recent.error ? (
            <ErrorState message={recent.error.message} retry={() => setRevision((v) => v + 1)} />
          ) : recent.data?.content.length ? (
            <IncidentTable incidents={recent.data.content} />
          ) : (
            <Empty />
          )}
        </section>
        <section className="panel priority-panel">
          <div className="panel-heading">
            <div>
              <h2>حسب الأولوية</h2>
              <p>جميع البلاغات المسجلة</p>
            </div>
          </div>
          {summary.loading ? (
            <Loading />
          ) : summary.error ? (
            <p className="muted">الإحصائيات غير متاحة حاليًا.</p>
          ) : (
            <>
              <div className="priority-total">
                <strong>{number(total)}</strong>
                <span>بلاغ مسجل</span>
              </div>
              <div className="priority-bars">
                {(['HIGH', 'MEDIUM', 'LOW'] as Priority[]).map((priority) => (
                  <Link
                    to={'/incidents?priority=' + priority}
                    className="priority-row"
                    key={priority}
                  >
                    <div>
                      <span>
                        <i className={'priority-dot ' + priority} />
                        {priorities[priority]}
                      </span>
                      <strong>{number(summary.data!.byPriority[priority])}</strong>
                    </div>
                    <div className="bar-track">
                      <span
                        className={priority}
                        style={{
                          width:
                            (total ? (summary.data!.byPriority[priority] / total) * 100 : 0) + '%',
                        }}
                      />
                    </div>
                  </Link>
                ))}
              </div>
            </>
          )}
        </section>
      </div>
      <div className="dashboard-footnote">
        <span>نسبة البلاغات المحلولة</span>
        <strong>{summary.loading || summary.error ? '—' : number(percent) + '٪'}</strong>
        <span>من إجمالي البلاغات</span>
      </div>
    </>
  )
}
