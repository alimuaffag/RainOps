import { test as base } from '@playwright/test'
import type { HistoryEntry, Incident, Status } from '../src/types'

export const seed: Incident = {
  id: 'a1111111-1111-4111-8111-111111111111',
  title: 'تجمع مياه أمام المدرسة',
  description: 'تجمع مياه يعيق الوصول إلى المدخل الرئيسي.',
  district: 'النزهة',
  locationDescription: 'المدخل الرئيسي للمدرسة',
  priority: 'HIGH',
  status: 'NEW',
  latitude: 21.543333,
  longitude: 39.172778,
  createdAt: '2026-11-13T09:00:00Z',
  updatedAt: '2026-11-13T09:00:00Z',
  resolvedAt: null,
}
export const test = base.extend<{ mockApi: void }>({
  mockApi: [
    async ({ page }, use) => {
      let rows: Incident[] = [structuredClone(seed)]
      let history: HistoryEntry[] = [
        {
          id: 'history-1',
          incidentId: seed.id,
          fromStatus: null,
          toStatus: 'NEW',
          note: null,
          changedAt: seed.createdAt,
        },
      ]
      await page.route('**/api/v1/**', async (route) => {
        const req = route.request()
        const url = new URL(req.url())
        const path = url.pathname.replace('/api/v1', '')
        const body = req.method() === 'GET' ? null : req.postDataJSON()
        const send = (data: unknown, status = 200) =>
          route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
        if (path === '/dashboard/summary')
          return send({
            total: rows.length,
            byStatus: Object.fromEntries(
              ['NEW', 'IN_PROGRESS', 'RESOLVED'].map((s) => [
                s,
                rows.filter((r) => r.status === s).length,
              ]),
            ),
            byPriority: Object.fromEntries(
              ['HIGH', 'MEDIUM', 'LOW'].map((p) => [
                p,
                rows.filter((r) => r.priority === p).length,
              ]),
            ),
          })
        if (path === '/incidents' && req.method() === 'POST') {
          const item = {
            ...seed,
            ...body,
            id: 'b2222222-2222-4222-8222-222222222222',
            status: 'NEW' as Status,
            latitude: body.latitude ?? null,
            longitude: body.longitude ?? null,
          }
          rows.unshift(item)
          history = [
            {
              id: 'history-new',
              incidentId: item.id,
              fromStatus: null,
              toStatus: 'NEW',
              note: null,
              changedAt: seed.createdAt,
            },
          ]
          return send(item, 201)
        }
        if (path === '/incidents') {
          const search = url.searchParams.get('search') || ''
          const status = url.searchParams.get('status')
          const priority = url.searchParams.get('priority')
          const found = rows.filter(
            (r) =>
              (!search || (r.title + r.district).includes(search)) &&
              (!status || r.status === status) &&
              (!priority || r.priority === priority),
          )
          const pageNumber = Number(url.searchParams.get('page') || 0)
          const size = Number(url.searchParams.get('size') || 10)
          return send({
            content: found.slice(pageNumber * size, (pageNumber + 1) * size),
            totalElements: found.length,
            totalPages: Math.ceil(found.length / size),
            page: pageNumber,
            size,
          })
        }
        const id = path.split('/')[2]
        const item = rows.find((r) => r.id === id)
        if (!item) return send({ detail: 'Not found' }, 404)
        if (path.endsWith('/history')) return send(history.filter((h) => h.incidentId === id))
        if (path.endsWith('/status')) {
          history.push({
            id: 'history-change-' + history.length,
            incidentId: id,
            fromStatus: item.status,
            toStatus: body.status,
            note: body.note,
            changedAt: seed.updatedAt,
          })
          Object.assign(item, {
            status: body.status,
            resolvedAt: body.status === 'RESOLVED' ? seed.updatedAt : null,
          })
        } else if (req.method() === 'PUT') Object.assign(item, body)
        return send(item)
      })
      await use()
    },
    { auto: true },
  ],
})
export { expect } from '@playwright/test'
