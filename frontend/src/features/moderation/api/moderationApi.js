import httpClient from '../../../services/httpClient.js'

function data(response) {
  return response.data.data
}

export const moderationApi = {
  queue: (params) => httpClient.get('/moderation/queue', { params }).then(data),
  detail: (caseId) => httpClient.get(`/moderation/${caseId}`).then(data),
  history: (targetType, targetId, params) => httpClient.get(`/moderation/targets/${targetType}/${targetId}/history`, { params }).then(data),
  submit: (targetType, targetId, payload) => httpClient.post(`/moderation/targets/${targetType}/${targetId}/submit`, payload).then(data),
  approve: (caseId, payload) => httpClient.post(`/moderation/${caseId}/approve`, payload).then(data),
  reject: (caseId, payload) => httpClient.post(`/moderation/${caseId}/reject`, payload).then(data),
  cancel: (caseId, payload) => httpClient.post(`/moderation/${caseId}/cancel`, payload).then(data),
  notifications: (params) => httpClient.get('/account/notifications', { params }).then(data),
  unreadCount: () => httpClient.get('/account/notifications/unread-count').then(data),
  markNotificationRead: (id) => httpClient.post(`/account/notifications/${id}/read`).then(data),
}
