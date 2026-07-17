import httpClient from '../../../services/httpClient.js'

function data(response) {
  return response.data.data
}

export const placeApi = {
  categories: () => httpClient.get('/place-categories').then(data),
  search: (params) => httpClient.get('/places', { params }).then(data),
  nearby: (params) => httpClient.get('/places/nearby', { params }).then(data),
  detail: (slug) => httpClient.get(`/places/${encodeURIComponent(slug)}`).then(data),
  favorite: (placeId) => httpClient.post(`/places/${placeId}/favorite`).then(data),
  unfavorite: (placeId) => httpClient.delete(`/places/${placeId}/favorite`).then(data),
  favorites: (params) => httpClient.get('/account/favorites', { params }).then(data),
  searchHistory: () => httpClient.get('/account/search-history').then(data),
  deleteSearchHistory: (historyId) => httpClient.delete(`/account/search-history/${historyId}`).then(data),
  clearSearchHistory: () => httpClient.delete('/account/search-history').then(data),
}
