import httpClient from '../../../services/httpClient.js'

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
  events: (params) => httpClient.get('/events', { params }).then(data),
  event: (slug) => httpClient.get(`/events/${encodeURIComponent(slug)}`).then(data),
}
