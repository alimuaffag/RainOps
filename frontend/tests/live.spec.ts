import { test, expect } from '@playwright/test'

test.skip(process.env.RAINOPS_LIVE !== '1', 'Requires an isolated backend database.')
test('real Spring Boot and PostgreSQL lifecycle through the UI', async ({ page, request }) => {
  await page.goto('/incidents/new')
  await page.getByLabel('عنوان البلاغ').fill('اختبار تكامل الواجهة ' + Date.now())
  await page.getByLabel('وصف البلاغ').fill('اختبار مع قاعدة بيانات معزولة')
  await page.getByLabel('الحي', { exact: false }).fill('النزهة')
  await page.getByLabel('وصف الموقع').fill('أمام الحديقة')
  await page.getByRole('button', { name: 'إنشاء البلاغ', exact: true }).click()
  await expect(page.getByRole('button', { name: 'بدء المعالجة' })).toBeVisible()
  const id = page.url().split('/').pop()
  await page.getByRole('link', { name: 'تعديل التفاصيل' }).click()
  await page.getByLabel('الأولوية', { exact: false }).selectOption('HIGH')
  await page.getByRole('button', { name: 'حفظ التعديلات' }).click()
  await page.getByLabel('ملاحظة التحديث').fill('تم بدء العمل')
  await page.getByRole('button', { name: 'بدء المعالجة' }).click()
  await page.getByRole('button', { name: 'تأكيد حل البلاغ' }).click()
  await expect(page.getByText('تاريخ الحل', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'إعادة فتح البلاغ' }).click()
  await expect(page.getByRole('button', { name: 'تأكيد حل البلاغ' })).toBeVisible()
  await page.reload()
  await expect(page.locator('.timeline li')).toHaveCount(4)
  const incident = await request.get('/api/v1/incidents/' + id)
  expect(incident.ok()).toBe(true)
  expect(await incident.json()).toMatchObject({
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    resolvedAt: null,
  })
  const history = await request.get('/api/v1/incidents/' + id + '/history')
  expect(await history.json()).toHaveLength(4)
  await page.goto(
    '/incidents?search=' + encodeURIComponent('النزهة') + '&status=IN_PROGRESS&priority=HIGH',
  )
  await expect(page.locator('.incident-title').first()).toBeVisible()
})
