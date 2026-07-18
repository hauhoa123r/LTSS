import httpClient from '../../../services/apiClient.js'

const data = (response) => response.data.data

export const administrationApi = {
  users: (params) => httpClient.get('/admin/users', { params }).then(data),
  user: (id) => httpClient.get(`/admin/users/${id}`).then(data),
  status: (id, payload) => httpClient.put(`/admin/users/${id}/status`, payload).then(data),
  resetPassword: (id, payload) => httpClient.put(`/admin/users/${id}/password`, payload).then(data),
  assignRole: (id, role, payload) => httpClient.put(`/admin/users/${id}/roles/${role}`, payload).then(data),
  revokeRole: (id, role, payload) => httpClient.delete(`/admin/users/${id}/roles/${role}`, { data: payload }).then(data),
  auditLogs: (params) => httpClient.get('/admin/audit-logs', { params }).then(data),
  auditLog: (id) => httpClient.get(`/admin/audit-logs/${id}`).then(data),
  auditMetadata: () => httpClient.get('/admin/audit-metadata').then(data),
}
