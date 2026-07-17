import httpClient from '../../../services/httpClient.js'

const data = (response) => response.data.data

export const tourApi = {
  publicTours: (params) => httpClient.get('/tours', { params }).then(data),
  detail: (id) => httpClient.get(`/tours/${id}`).then(data),
  copy: (id) => httpClient.post(`/tours/${id}/copy`).then(data),
  mine: (params) => httpClient.get('/account/tours', { params }).then(data),
  create: (payload) => httpClient.post('/account/tours', payload).then(data),
  update: (id, payload) => httpClient.put(`/account/tours/${id}`, payload).then(data),
  visibility: (id, payload) => httpClient.put(`/account/tours/${id}/visibility`, payload).then(data),
  delete: (id, version) => httpClient.delete(`/account/tours/${id}`, { params: { version } }).then(data),
}
