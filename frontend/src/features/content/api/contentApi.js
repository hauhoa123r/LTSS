import httpClient from '../../../services/apiClient.js'

function data(response) {
  return response.data.data
}

export const contentApi = {
  businesses: (params) => httpClient.get('/businesses', { params }).then(data),
  business: (id) => httpClient.get(`/businesses/${id}`).then(data),
  posts: (params) => httpClient.get('/business-posts', { params }).then(data),
  post: (slug) => httpClient.get(`/business-posts/${encodeURIComponent(slug)}`).then(data),
  promotions: (params) => httpClient.get('/promotions', { params }).then(data),
  promotion: (id) => httpClient.get(`/promotions/${id}`).then(data),
  articleCategories: () => httpClient.get('/article-categories').then(data),
  articles: (params) => httpClient.get('/articles', { params }).then(data),
  article: (slug) => httpClient.get(`/articles/${encodeURIComponent(slug)}`).then(data),
  managedArticles: (params) => httpClient.get('/management/articles', { params }).then(data),
  managedArticle: (id) => httpClient.get(`/management/articles/${id}`).then(data),
  createArticle: (payload) => httpClient.post('/management/articles', payload).then(data),
  updateArticle: (id, payload) => httpClient.put(`/management/articles/${id}`, payload).then(data),
  deleteArticle: (id, version) => httpClient.delete(`/management/articles/${id}`, { params: { version } }).then(data),
  events: (params) => httpClient.get('/events', { params }).then(data),
  event: (slug) => httpClient.get(`/events/${encodeURIComponent(slug)}`).then(data),
}
