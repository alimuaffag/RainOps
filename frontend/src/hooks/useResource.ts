import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'

export function useResource<T>(path: string, revision = 0) {
  const [state, setState] = useState<{ key: string; data?: T; error?: ApiError; loading: boolean }>(
    { key: '', loading: true },
  )
  const key = path + ':' + revision
  useEffect(() => {
    const controller = new AbortController()
    api<T>(path, { signal: controller.signal }).then(
      (data) => {
        if (!controller.signal.aborted) setState({ key, data, loading: false })
      },
      (error) => {
        if (!controller.signal.aborted) setState({ key, error, loading: false })
      },
    )
    return () => controller.abort()
  }, [path, key])
  return state.key === key ? state : { key, loading: true, data: undefined, error: undefined }
}
