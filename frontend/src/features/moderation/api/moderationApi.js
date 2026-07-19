import httpClient from '../../../services/apiClient.js'

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
  articleCategories: (params) => httpClient.get('/moderation/article-categories', { params }).then(data),
  getArticleCategory: (id) => httpClient.get(`/moderation/article-categories/${id}`).then(data),
  createArticleCategory: (payload) => httpClient.post('/moderation/article-categories', payload).then(data),
  updateArticleCategory: (id, payload) => httpClient.put(`/moderation/article-categories/${id}`, payload).then(data),
  deleteArticleCategory: (id) => httpClient.delete(`/moderation/article-categories/${id}`).then(data),
  placeCategories: (params) => httpClient.get('/moderation/place-categories', { params }).then(data),
  getPlaceCategory: (id) => httpClient.get(`/moderation/place-categories/${id}`).then(data),
  createPlaceCategory: (payload) => httpClient.post('/moderation/place-categories', payload).then(data),
  updatePlaceCategory: (id, payload) => httpClient.put(`/moderation/place-categories/${id}`, payload).then(data),
  deletePlaceCategory: (id) => httpClient.delete(`/moderation/place-categories/${id}`).then(data),
}
