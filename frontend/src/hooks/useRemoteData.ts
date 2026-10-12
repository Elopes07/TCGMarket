import axios from 'axios'
import { useCallback, useEffect, useState } from 'react'

export type RemoteData<T> =
  | { status: 'loading' }
  | { status: 'ready'; data: T }
  | { status: 'notFound' }
  | { status: 'error' }

type Settled<T> = { key: string } & Exclude<
  RemoteData<T>,
  { status: 'loading' }
>

// `fetcher` precisa ser estável (useCallback); uma nova identidade dispara outra busca.
export function useRemoteData<T>(key: string, fetcher: () => Promise<T>) {
  const [settled, setSettled] = useState<Settled<T> | null>(null)
  const [attempt, setAttempt] = useState(0)
  const requestKey = `${key}#${attempt}`

  useEffect(() => {
    let active = true
    fetcher()
      .then((data) => {
        if (active) setSettled({ key: requestKey, status: 'ready', data })
      })
      .catch((cause: unknown) => {
        if (!active) return
        const notFound =
          axios.isAxiosError(cause) && cause.response?.status === 404
        setSettled({ key: requestKey, status: notFound ? 'notFound' : 'error' })
      })
    return () => {
      active = false
    }
  }, [requestKey, fetcher])

  const retry = useCallback(() => setAttempt((current) => current + 1), [])
  const state: RemoteData<T> =
    settled?.key === requestKey ? settled : { status: 'loading' }

  return { state, retry }
}
