import httpClient from '../../../services/apiClient.js'

const data = (response) => response.data.data

export const businessOwnerApi = {
  profile: () => httpClient.get('/business-owner/profile').then(data),
  posts: (params) => httpClient.get('/business-owner/posts', { params }).then(data),
  promotions: (params) => httpClient.get('/business-owner/promotions', { params }).then(data),
}
