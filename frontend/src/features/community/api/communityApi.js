import httpClient from '../../../services/httpClient.js'

const data = (response) => response.data.data

export const communityApi = {
  reviews: (params) => httpClient.get('/reviews', { params }).then(data),
  createReview: (targetType, targetId, payload) => httpClient.post(`/reviews/${targetType}/${targetId}`, payload).then(data),
  myReviews: (params) => httpClient.get('/account/reviews', { params }).then(data),
  reply: (reviewId, payload) => httpClient.post(`/reviews/${reviewId}/reply`, payload).then(data),
}
