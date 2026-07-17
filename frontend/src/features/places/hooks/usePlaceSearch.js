import { useCallback, useEffect, useState } from 'react'
import { placeApi } from '../api/placeApi.js'

export function usePlaceSearch(params) {
  const [state, setState] = useState({ data: null, loading: true, error: null })

  const load = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null }))
    try {
      const data = params.mode === 'nearby'
        ? await placeApi.nearby(params.query)
        : await placeApi.search(params.query)
      setState({ data, loading: false, error: null })
    } catch (error) {
      setState({ data: null, loading: false, error })
    }
  }, [params])

  useEffect(() => {
    load()
  }, [load])

  return { ...state, reload: load, setData: (data) => setState({ data, loading: false, error: null }) }
}
