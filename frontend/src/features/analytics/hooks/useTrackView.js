import { useEffect } from 'react'
import { analyticsApi } from '../api/analyticsApi.js'

export function useTrackView(targetType, targetId) {
  useEffect(() => {
    if (!targetType || !targetId) return
    analyticsApi.track('VIEW', targetType, targetId).catch(() => {})
  }, [targetType, targetId])
}
