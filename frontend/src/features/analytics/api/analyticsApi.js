import httpClient from '../../../services/apiClient.js'

const data = (response) => response.data.data

function sessionKey() {
  const key = 'ltss.analytics.session'
  let value = sessionStorage.getItem(key)
  if (!value) {
    value = crypto.randomUUID()
    sessionStorage.setItem(key, value)
  }
  return value
}

export const analyticsApi = {
  track: (eventTypeCode, targetType, targetId, metadata = {}) => httpClient.post('/engagement-events', {
    eventTypeCode,
    sessionKey: sessionKey(),
    targetType,
    targetId,
    metadata,
  }).then(data),
  system: (params) => httpClient.get('/analytics/system', { params }).then(data),
  business: (params) => httpClient.get('/analytics/business', { params }).then(data),
  dashboard: (params) => httpClient.get('/admin/dashboard', { params }).then(data),
  monumentStatistics: (params) => httpClient.get('/admin/monument-statistics', { params }).then(data),
  businessStatistics: (params) => httpClient.get('/admin/business-statistics', { params }).then(data),
  monthlyEventStatistics: (params) => httpClient.get('/admin/monthly-event-statistics', { params }).then(data),
  retention: () => httpClient.get('/admin/retention-status').then(data),
}
