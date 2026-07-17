import httpClient from '../../../services/apiClient.js'

const data = (response) => response.data.data

export const quizApi = {
  published: (params) => httpClient.get('/quizzes', { params }).then(data),
  detail: (id) => httpClient.get(`/quizzes/${id}`).then(data),
  start: (id, payload) => httpClient.post(`/quizzes/${id}/attempts`, payload).then(data),
  attempt: (id) => httpClient.get(`/quiz-attempts/${id}`).then(data),
  submit: (id, payload) => httpClient.post(`/quiz-attempts/${id}/submit`, payload).then(data),
  history: (params) => httpClient.get('/account/quiz-attempts', { params }).then(data),
  badges: (params) => httpClient.get('/account/badges', { params }).then(data),
  mine: (params) => httpClient.get('/management/quizzes', { params }).then(data),
  managementDetail: (id) => httpClient.get(`/management/quizzes/${id}`).then(data),
  create: (payload) => httpClient.post('/management/quizzes', payload).then(data),
  update: (id, payload) => httpClient.put(`/management/quizzes/${id}`, payload).then(data),
  delete: (id, version) => httpClient.delete(`/management/quizzes/${id}`, { params: { version } }).then(data),
}
