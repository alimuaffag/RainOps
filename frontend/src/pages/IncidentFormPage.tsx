import { useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowRight, Check, MapPin, Save } from 'lucide-react'
import { ErrorState, Loading } from '../components/ui'
import { useResource } from '../hooks/useResource'
import { api, ApiError } from '../lib/api'
import { priorities } from '../lib/format'
import type { Incident, IncidentInput, Priority } from '../types'

export default function IncidentFormPage() {
  const { id } = useParams()
  return id ? <EditLoader key={id} id={id} /> : <IncidentForm key="new" />
}

function EditLoader({ id }: { id: string }) {
  const [revision, setRevision] = useState(0)
  const result = useResource<Incident>('/incidents/' + id, revision)
  if (result.loading) return <Loading />
  if (result.error)
    return <ErrorState message={result.error.message} retry={() => setRevision((v) => v + 1)} />
  return <IncidentForm initial={result.data} />
}

function IncidentForm({ initial }: { initial?: Incident }) {
  const navigate = useNavigate()
  const formRef = useRef<HTMLFormElement>(null)
  const saving = useRef(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [values, setValues] = useState({
    title: initial?.title || '',
    description: initial?.description || '',
    district: initial?.district || '',
    locationDescription: initial?.locationDescription || '',
    priority: initial?.priority || ('MEDIUM' as Priority),
    latitude: initial?.latitude?.toString() || '',
    longitude: initial?.longitude?.toString() || '',
  })
  function update(name: keyof typeof values, value: string) {
    setValues((previous) => ({ ...previous, [name]: value }))
    setErrors((previous) => ({ ...previous, [name]: '', coordinatesComplete: '' }))
  }
  function focusError(fields: Record<string, string>) {
    requestAnimationFrame(() => {
      const key =
        Object.keys(fields)[0] === 'coordinatesComplete' ? 'latitude' : Object.keys(fields)[0]
      formRef.current?.querySelector<HTMLElement>('[name="' + key + '"]')?.focus()
    })
  }
  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving.current) return
    const validation: Record<string, string> = {}
    for (const field of ['title', 'description', 'district', 'locationDescription'] as const) {
      if (!values[field].trim()) validation[field] = 'هذا الحقل مطلوب.'
    }
    if (Boolean(values.latitude) !== Boolean(values.longitude))
      validation.coordinatesComplete = 'أدخل خط العرض وخط الطول معًا، أو اتركهما فارغين.'
    if (Object.keys(validation).length) {
      setErrors(validation)
      focusError(validation)
      return
    }
    saving.current = true
    setBusy(true)
    setError('')
    setErrors({})
    const input: IncidentInput = {
      title: values.title.trim(),
      description: values.description.trim(),
      district: values.district.trim(),
      locationDescription: values.locationDescription.trim(),
      priority: values.priority,
      latitude: values.latitude === '' ? null : Number(values.latitude),
      longitude: values.longitude === '' ? null : Number(values.longitude),
    }
    try {
      const saved = await api<Incident>(initial ? '/incidents/' + initial.id : '/incidents', {
        method: initial ? 'PUT' : 'POST',
        body: JSON.stringify(input),
      })
      navigate('/incidents/' + saved.id, {
        replace: true,
        state: { notice: initial ? 'تم حفظ تعديلات البلاغ.' : 'تم إنشاء البلاغ بنجاح.' },
      })
    } catch (caught) {
      const failure = caught as ApiError
      setError(failure.message)
      setErrors(failure.fields || {})
      focusError(failure.fields || {})
    } finally {
      saving.current = false
      setBusy(false)
    }
  }
  function field(name: keyof typeof values) {
    return {
      id: name,
      name,
      value: values[name],
      onChange: (
        event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
      ) => update(name, event.target.value),
      'aria-invalid': Boolean(errors[name]),
      'aria-describedby': errors[name] ? name + '-error' : undefined,
    }
  }
  function fieldError(name: string) {
    return errors[name] ? (
      <span id={name + '-error'} className="field-error">
        {errors[name]}
      </span>
    ) : null
  }
  const back = initial ? '/incidents/' + initial.id : '/incidents'
  return (
    <>
      <Link className="back-link" to={back}>
        <ArrowRight size={17} />
        {initial ? 'العودة إلى تفاصيل البلاغ' : 'العودة إلى البلاغات'}
      </Link>
      <div className="page-heading">
        <div>
          <div className="eyebrow">تسجيل ومتابعة</div>
          <h1>{initial ? 'تعديل البلاغ' : 'بلاغ جديد'}</h1>
          <p>
            {initial ? 'عدّل البيانات المطلوبة ثم احفظ التغييرات.' : 'أدخل بيانات البلاغ وموقعه.'}
          </p>
        </div>
      </div>
      <div className="form-layout">
        <form
          ref={formRef}
          onSubmit={submit}
          className="panel incident-form"
          aria-label={initial ? 'تعديل البلاغ' : 'إنشاء بلاغ'}
        >
          <div className="form-section-heading">
            <span className="section-number">01</span>
            <div>
              <h2>تفاصيل البلاغ</h2>
              <p>الحقول المعلّمة بـ * مطلوبة</p>
            </div>
          </div>
          {error && (
            <div className="form-alert" role="alert">
              {error}
            </div>
          )}
          <fieldset disabled={busy}>
            <div className="field">
              <label htmlFor="title">
                عنوان البلاغ <span>*</span>
              </label>
              <input
                {...field('title')}
                required
                maxLength={150}
                placeholder="مثال: تجمع مياه أمام المدرسة"
              />
              {fieldError('title')}
            </div>
            <div className="field">
              <label htmlFor="description">
                وصف البلاغ <span>*</span>
              </label>
              <textarea
                {...field('description')}
                required
                rows={4}
                placeholder="صف تجمع المياه وتأثيره على الحركة أو الوصول…"
              />
              {fieldError('description')}
            </div>
            <div className="field">
              <label htmlFor="priority">
                الأولوية <span>*</span>
              </label>
              <select {...field('priority')}>
                {Object.entries(priorities).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
              {fieldError('priority')}
            </div>
            <div className="form-section-heading second">
              <span className="section-number">02</span>
              <div>
                <h2>الموقع</h2>
                <p>حدّد مكان البلاغ بأكبر قدر ممكن من الدقة</p>
              </div>
            </div>
            <div className="field">
              <label htmlFor="district">
                الحي <span>*</span>
              </label>
              <input
                {...field('district')}
                required
                maxLength={100}
                placeholder="مثال: حي النزهة"
              />
              {fieldError('district')}
            </div>
            <div className="field">
              <label htmlFor="locationDescription">
                وصف الموقع <span>*</span>
              </label>
              <textarea
                {...field('locationDescription')}
                required
                maxLength={500}
                rows={2}
                placeholder="اسم الشارع، أقرب معلم، أو المدخل…"
              />
              {fieldError('locationDescription')}
            </div>
            <div className="coordinates-heading">
              <MapPin size={16} />
              الإحداثيات <span>اختياري · حتى ٦ منازل عشرية</span>
            </div>
            <div className="form-row">
              <div className="field">
                <label htmlFor="latitude">خط العرض</label>
                <input
                  {...field('latitude')}
                  type="number"
                  min={-90}
                  max={90}
                  step="0.000001"
                  dir="ltr"
                  placeholder="21.543333"
                />
                {fieldError('latitude')}
              </div>
              <div className="field">
                <label htmlFor="longitude">خط الطول</label>
                <input
                  {...field('longitude')}
                  type="number"
                  min={-180}
                  max={180}
                  step="0.000001"
                  dir="ltr"
                  placeholder="39.172778"
                />
                {fieldError('longitude')}
              </div>
            </div>
            {errors.coordinatesComplete && (
              <p className="field-error" role="alert">
                {errors.coordinatesComplete}
              </p>
            )}
            <div className="form-actions">
              <button className="button" type="submit">
                <Save size={17} />
                {busy ? 'جارٍ الحفظ…' : initial ? 'حفظ التعديلات' : 'إنشاء البلاغ'}
              </button>
              <Link to={back} className="button secondary">
                إلغاء
              </Link>
            </div>
          </fieldset>
        </form>
        <aside className="form-guide">
          <h2>بيانات الموقع</h2>
          <p>
            اسم الحي ووصف الموقع مطلوبان. الإحداثيات اختيارية، ويجب إدخال خط العرض وخط الطول معًا.
          </p>
          <ul>
            <li>
              <Check size={17} />
              وضّح تأثير تجمع المياه.
            </li>
            <li>
              <Check size={17} />
              اذكر أقرب معلم للموقع.
            </li>
            <li>
              <Check size={17} />
              اختر الأولوية بحسب الحالة.
            </li>
          </ul>
          <div className="guide-bottom">
            {initial
              ? 'تعديل التفاصيل لا يغيّر حالة البلاغ. يمكنك متابعة الحالة من صفحة التفاصيل.'
              : 'يبدأ البلاغ بحالة «جديد»، ثم تتم متابعته حتى الحل.'}
          </div>
        </aside>
      </div>
    </>
  )
}
