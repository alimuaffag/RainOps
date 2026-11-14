export class ApiError extends Error {
  status: number
  fields: Record<string, string>
  constructor(message: string, status = 0, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response
  const csrfHeaders: Record<string, string> = {}
  if (
    import.meta.env.VITE_DEMO_MODE === 'true' &&
    !['GET', 'HEAD'].includes(options.method || 'GET')
  ) {
    const csrf = await api<{ headerName: string; token: string }>('/csrf')
    csrfHeaders[csrf.headerName] = csrf.token
  }
  try {
    response = await fetch('/api/v1' + path, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...csrfHeaders,
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    })
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') throw error
    throw new ApiError('تعذّر الاتصال بالخادم. تحقق من الاتصال ثم أعد المحاولة.')
  }
  if (response.status === 401 && import.meta.env.VITE_DEMO_MODE === 'true') {
    window.location.assign('/login')
    throw new ApiError('انتهت جلسة العرض. سجّل الدخول مرة أخرى.', 401)
  }
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const messages: Record<number, string> = {
      403: 'تعذّر التحقق من الجلسة. حدّث الصفحة ثم حاول مجددًا.',
      400: 'بعض البيانات غير صحيحة. راجع الحقول وحاول مرة أخرى.',
      404: 'لم نتمكن من العثور على هذا البلاغ.',
      409: 'تغيّرت بيانات البلاغ أو لا يمكن تطبيق هذه الحالة. حدّث الصفحة ثم حاول مجددًا.',
    }
    throw new ApiError(
      messages[response.status] || 'تعذّر إكمال الطلب. حاول مرة أخرى بعد قليل.',
      response.status,
      body?.errors || {},
    )
  }
  if (body === null) throw new ApiError('وصل رد غير متوقع من الخادم. أعد المحاولة.')
  return body as T
}
