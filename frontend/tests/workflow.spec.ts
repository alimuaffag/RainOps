import { test, expect, seed } from './fixtures'

test('dashboard, navigation and responsive layout', async ({ page }, testInfo) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'ملخص البلاغات' })).toBeVisible()
  await expect(page.getByRole('link', { name: seed.title, exact: true })).toBeVisible()
  await expect(page.locator('.stat-number').first()).toContainText('١')
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    .toBe(true)
  await page.screenshot({ path: testInfo.outputPath('dashboard.png'), fullPage: true })
  if (testInfo.project.name === 'mobile') {
    await page.getByRole('button', { name: 'فتح القائمة' }).click()
    await page.getByRole('navigation').getByRole('link', { name: 'البلاغات', exact: true }).click()
  } else
    await page.getByRole('navigation').getByRole('link', { name: 'البلاغات', exact: true }).click()
  await expect(page).toHaveURL(/\/incidents$/)
  expect(errors).toEqual([])
})

test('create, update, resolve and reopen with status history', async ({ page }, testInfo) => {
  await page.goto('/incidents/new')
  await page.getByLabel('عنوان البلاغ').fill('تجمع مياه في شارع الحديقة')
  await page.getByLabel('وصف البلاغ').fill('مياه مرتفعة بجانب الرصيف')
  await page.getByLabel('الحي', { exact: false }).fill('الروضة')
  await page.getByLabel('وصف الموقع').fill('بجوار الحديقة')
  await page.getByLabel('الأولوية', { exact: false }).selectOption('HIGH')
  await page.getByLabel('خط العرض').fill('21.5')
  await page.getByRole('button', { name: 'إنشاء البلاغ', exact: true }).click()
  await expect(page.getByText('أدخل خط العرض وخط الطول معًا، أو اتركهما فارغين.')).toBeVisible()
  await page.getByLabel('خط الطول').fill('39.2')
  await page.screenshot({ path: testInfo.outputPath('form.png'), fullPage: true })
  await page.getByRole('button', { name: 'إنشاء البلاغ', exact: true }).click()
  await expect(
    page.getByRole('heading', { name: 'تجمع مياه في شارع الحديقة', exact: true }),
  ).toBeVisible()
  await expect(page.getByRole('status')).toContainText('تم إنشاء البلاغ بنجاح')
  await page.getByRole('link', { name: 'تعديل التفاصيل' }).click()
  await page.getByLabel('عنوان البلاغ').fill('بلاغ محدّث')
  await page.getByLabel('خط العرض').fill('')
  await page.getByLabel('خط الطول').fill('')
  await page.getByRole('button', { name: 'حفظ التعديلات' }).click()
  await expect(page.getByRole('heading', { name: 'بلاغ محدّث', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'عرض الموقع على الخريطة' })).toHaveCount(0)
  await page.getByLabel('ملاحظة التحديث').fill('بدأت المعالجة')
  await page.getByRole('button', { name: 'بدء المعالجة' }).click()
  await expect(page.locator('.timeline')).toContainText('بدأت المعالجة')
  await page.getByRole('button', { name: 'تأكيد حل البلاغ' }).click()
  await expect(page.getByText('تاريخ الحل', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'إعادة فتح البلاغ' }).click()
  await expect(page.getByRole('button', { name: 'تأكيد حل البلاغ' })).toBeVisible()
  await expect(page.getByText('تاريخ الحل', { exact: true })).toHaveCount(0)
  await expect(page.locator('.timeline li')).toHaveCount(4)
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    .toBe(true)
  await page.screenshot({ path: testInfo.outputPath('detail.png'), fullPage: true })
})

test('search, combined filters, URL persistence and empty results', async ({ page }) => {
  await page.goto('/incidents')
  await page.getByLabel('البحث بالعنوان أو الحي').fill('النزهة')
  await page.getByRole('button', { name: 'بحث', exact: true }).click()
  await page.getByLabel('تصفية الأولوية').selectOption('HIGH')
  await page.getByLabel('تصفية الحالة').selectOption('NEW')
  await expect(page.getByRole('link', { name: seed.title, exact: true })).toBeVisible()
  await page.reload()
  await expect(page.getByLabel('تصفية الأولوية')).toHaveValue('HIGH')
  await expect(page.getByLabel('البحث بالعنوان أو الحي')).toHaveValue('النزهة')
  await page.getByLabel('تصفية الحالة').selectOption('RESOLVED')
  await expect(page.getByRole('heading', { name: 'لا توجد بلاغات مطابقة' })).toBeVisible()
  await page.getByRole('button', { name: 'مسح الفلاتر' }).click()
  await expect(page.getByRole('link', { name: seed.title, exact: true })).toBeVisible()
})

test('failed loading can be retried and missing incidents are clear', async ({ page }) => {
  let failing = true
  await page.route('**/api/v1/incidents?**', (route) =>
    failing ? route.fulfill({ status: 503, body: '{}' }) : route.fallback(),
  )
  await page.goto('/incidents')
  await expect(page.getByRole('alert')).toBeVisible()
  failing = false
  await page.getByRole('button', { name: 'إعادة المحاولة' }).click()
  await expect(page.getByRole('link', { name: seed.title, exact: true })).toBeVisible()
  await page.goto('/incidents/00000000-0000-0000-0000-000000000000')
  await expect(page.getByRole('alert')).toContainText('لم نتمكن من العثور على هذا البلاغ')
})

test('failed save preserves input, server field errors and conflict are visible', async ({
  page,
}) => {
  await page.goto('/incidents/' + seed.id + '/edit')
  await page.getByLabel('عنوان البلاغ').fill('عنوان محفوظ')
  await page.route('**/api/v1/incidents/' + seed.id, async (route) => {
    if (route.request().method() === 'PUT')
      return route.fulfill({
        status: 400,
        contentType: 'application/problem+json',
        body: JSON.stringify({ errors: { title: 'Title is invalid' } }),
      })
    await route.fallback()
  })
  await page.getByRole('button', { name: 'حفظ التعديلات' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page.getByLabel('عنوان البلاغ')).toHaveValue('عنوان محفوظ')
  await expect(page.getByLabel('عنوان البلاغ')).toHaveAttribute('aria-invalid', 'true')
  await page.goto('/incidents/' + seed.id)
  await page.route('**/api/v1/incidents/*/status', (route) =>
    route.fulfill({ status: 409, body: '{}' }),
  )
  await page.getByRole('button', { name: 'بدء المعالجة' }).click()
  await expect(page.getByRole('alert')).toContainText('تغيّرت بيانات البلاغ')
})

test('pagination follows server pages and resets with filters', async ({ page }) => {
  await page.route('**/api/v1/incidents?**', async (route) => {
    const url = new URL(route.request().url())
    const pageNumber = Number(url.searchParams.get('page'))
    await route.fulfill({
      json: {
        content: [{ ...seed, title: pageNumber ? 'بلاغ الصفحة الثانية' : 'بلاغ الصفحة الأولى' }],
        page: pageNumber,
        size: 10,
        totalElements: 11,
        totalPages: 2,
      },
    })
  })
  await page.goto('/incidents')
  await expect(page.getByRole('button', { name: 'السابق', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: 'التالي', exact: true }).click()
  await expect(page.getByRole('link', { name: 'بلاغ الصفحة الثانية', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'التالي', exact: true })).toBeDisabled()
  await page.getByLabel('تصفية الأولوية').selectOption('HIGH')
  await expect(page.getByRole('link', { name: 'بلاغ الصفحة الأولى', exact: true })).toBeVisible()
  await expect(page).not.toHaveURL(/page=1/)
})

test('empty dashboard never invents data', async ({ page }) => {
  await page.route('**/api/v1/dashboard/summary', (route) =>
    route.fulfill({
      json: {
        total: 0,
        byStatus: { NEW: 0, IN_PROGRESS: 0, RESOLVED: 0 },
        byPriority: { HIGH: 0, MEDIUM: 0, LOW: 0 },
      },
    }),
  )
  await page.route('**/api/v1/incidents?**', (route) =>
    route.fulfill({ json: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 5 } }),
  )
  await page.goto('/')
  await expect(page.getByRole('link', { name: 'إنشاء أول بلاغ' })).toBeVisible()
  await expect(page.locator('.stat-number').first()).toContainText('٠')
})

test('repeated clicks send only one mutation', async ({ page }) => {
  let requests = 0
  await page.goto('/incidents/' + seed.id)
  await page.route('**/api/v1/incidents/*/status', async (route) => {
    requests++
    await new Promise((resolve) => setTimeout(resolve, 300))
    await route.fallback()
  })
  await page.getByRole('button', { name: 'بدء المعالجة' }).evaluate((button: HTMLButtonElement) => {
    button.click()
    button.click()
  })
  await expect(page.getByRole('button', { name: 'تأكيد حل البلاغ' })).toBeVisible()
  expect(requests).toBe(1)
})
