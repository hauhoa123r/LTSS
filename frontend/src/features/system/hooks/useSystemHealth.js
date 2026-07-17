import { useCallback, useEffect, useRef, useState } from 'react'
import { getSystemHealth } from '../api/systemApi.js'

const INITIAL_STATE = {
  kind: 'loading',
  health: null,
  error: null,
}

export function useSystemHealth() {
  const [state, setState] = useState(INITIAL_STATE)
  const activeController = useRef(null)

  const checkHealth = useCallback(async () => {
    activeController.current?.abort()
    const controller = new AbortController()
    activeController.current = controller
    setState(INITIAL_STATE)

    try {
      const health = await getSystemHealth({ signal: controller.signal })

      if (controller.signal.aborted) return

      setState({
        kind: health.status.toUpperCase() === 'UP' ? 'success' : 'unavailable',
        health,
        error: null,
      })
    } catch (error) {
      if (controller.signal.aborted || error.isCanceled) return

      const backendUnavailable = error.isNetworkError || error.status >= 500
      setState({
        kind: backendUnavailable ? 'unavailable' : 'error',
        health: null,
        error,
      })
    }
  }, [])

  useEffect(() => {
    checkHealth()
    return () => activeController.current?.abort()
  }, [checkHealth])

  return {
    ...state,
    retry: checkHealth,
  }
}
