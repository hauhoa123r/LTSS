import httpClient from '../../../services/httpClient.js'

function data(response) {
  return response.data.data
}

export const authApi = {
  register: (payload) => httpClient.post('/auth/register', payload).then(data),
  verifyEmail: (token) => httpClient.post('/auth/email/verify', { token }).then(data),
  resendVerification: (email) => httpClient.post('/auth/email/resend', { email }).then(data),
  login: (payload) => httpClient.post('/auth/login', payload).then(data),
  refresh: () => httpClient.post('/auth/refresh', {}).then(data),
  logout: () => httpClient.post('/auth/logout', {}).then(data),
  forgotPassword: (email) => httpClient.post('/auth/password/forgot', { email }).then(data),
  resetPassword: (payload) => httpClient.post('/auth/password/reset', payload).then(data),
  profile: () => httpClient.get('/account/me').then(data),
  updateProfile: (payload) => httpClient.put('/account/me', payload).then(data),
  requestChangePasswordOtp: () => httpClient.post('/account/password/change-otp', {}).then(data),
  changePassword: (payload) => httpClient.put('/account/password', payload).then(data),
}
