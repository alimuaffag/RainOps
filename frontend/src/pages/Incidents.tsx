import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Plus, Search, SlidersHorizontal, X } from 'lucide-react'
import { Empty, ErrorState, IncidentTable, Loading } from '../components/ui'
import { useResource } from '../hooks/useResource'
import { number, priorities, statuses } from '../lib/format'
import type { IncidentPage } from '../types'

export default function Incidents() {
  const [params, setParams] = useSearchParams()
  const [revision, setRevision] = useState(0)
  const rawPage = Number(params.get('page') || 0)
  const page = Number.isInteger(rawPage) && rawPage >= 0 && rawPage <= 1000000 ? rawPage : 0
  const status = Object.hasOwn(statuses, params.get('status') || '') ? params.get('status')! : ''
  const priority = Object.hasOwn(priorities, params.get('priority') || '')
    ? params.get('priority')!
    : ''
  const search = (params.get('search') || '').slice(0, 150)
  const query = new URLSearchParams({ page: String(page), size: '10' })
  if (status) query.set('status', status)
  if (priority) query.set('priority', priority)
  if (search) query.set('search', search)
  const result = useResource<IncidentPage>('/incidents?' + query, revision)
  const filtered = Boolean(status || priority || search)
  function change(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.delete('page')
    setParams(next)
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">مركز المتابعة</div>
          <h1>البلاغات</h1>
          <p>ابحث بالعنوان أو الحي، أو حدّد الحالة والأولوية.</p>
        </div>
        <Link className="button" to="/incidents/new">
          <Plus size={18} />
          بلاغ جديد
        </Link>
      </div>
      <section className="panel">
        <div className="filters">
          <form
            className="search-box"
            key={search}
            onSubmit={(event) => {
              event.preventDefault()
              change('search', String(new FormData(event.currentTarget).get('search') || '').trim())
            }}
          >
            <Search size={19} />
            <input
              aria-label="البحث بالعنوان أو الحي"
              name="search"
              placeholder="ابحث بعنوان البلاغ أو الحي…"
              defaultValue={search}
              maxLength={150}
            />
            <button type="submit">بحث</button>
          </form>
          <div className="filter-select">
            <SlidersHorizontal size={17} />
            <select
              aria-label="تصفية الحالة"
              value={status}
              onChange={(event) => change('status', event.target.value)}
            >
              <option value="">كل الحالات</option>
              {Object.entries(statuses).map(([key, label]) => (
                <option value={key} key={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <select
            aria-label="تصفية الأولوية"
            value={priority}
            onChange={(event) => change('priority', event.target.value)}
          >
            <option value="">كل الأولويات</option>
            {Object.entries(priorities).map(([key, label]) => (
              <option value={key} key={key}>
                {label}
              </option>
            ))}
          </select>
          {filtered && (
            <button className="text-link" onClick={() => setParams({})}>
              <X size={15} />
              مسح الفلاتر
            </button>
          )}
        </div>
        <div className="list-caption">
          <h2>{filtered ? 'نتائج البحث' : 'جميع البلاغات'}</h2>
          <span className="count-pill">
            {result.data ? number(result.data.totalElements) + ' بلاغ' : '…'}
          </span>
        </div>
        {result.loading ? (
          <Loading />
        ) : result.error ? (
          <ErrorState message={result.error.message} retry={() => setRevision((v) => v + 1)} />
        ) : result.data?.content.length ? (
          <IncidentTable incidents={result.data.content} />
        ) : page > 0 ? (
          <div className="state">
            <h3>لا توجد بلاغات في هذه الصفحة</h3>
            <button className="button secondary" onClick={() => change('page', '0')}>
              العودة لأول صفحة
            </button>
          </div>
        ) : (
          <Empty filtered={filtered} />
        )}
        {result.data && result.data.totalPages > 0 && (
          <div className="pagination">
            <span>
              الصفحة {number(page + 1)} من {number(result.data.totalPages)}
            </span>
            <div>
              <button
                className="button secondary small"
                disabled={page === 0}
                onClick={() => change('page', String(page - 1))}
              >
                <ArrowRight size={16} />
                السابق
              </button>
              <button
                className="button secondary small"
                disabled={page + 1 >= result.data.totalPages}
                onClick={() => change('page', String(page + 1))}
              >
                التالي
                <ArrowLeft size={16} />
              </button>
            </div>
          </div>
        )}
      </section>
    </>
  )
}
