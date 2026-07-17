import axios from 'axios'
import { normalizeApiError } from './apiError.js'
import { createRequestId, REQUEST_ID_HEADER } from './requestId.js'
import { clearAccessToken, getAccessToken, setAccessToken } from './authSession.js'

const DEFAULT_API_BASE_URL = 'http://localhost:8081/api/v1'
const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const apiBaseUrl = (configuredBaseUrl || DEFAULT_API_BASE_URL).replace(/\/+$/, '')

const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
  },
  withCredentials: true,
})

let refreshRequest = null

httpClient.interceptors.request.use((config) => {
  const requestId = createRequestId()
  const accessToken = getAccessToken()

  if (typeof config.headers?.set === 'function') {
    config.headers.set(REQUEST_ID_HEADER, requestId)
  } else {
    config.headers = {
      ...config.headers,
      [REQUEST_ID_HEADER]: requestId,
    }
  }

  if (accessToken) {
    if (typeof config.headers?.set === 'function') {
      config.headers.set('Authorization', `Bearer ${accessToken}`)
    } else {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const isAuthRequest = originalRequest?.url?.includes('/auth/')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true
      try {
        refreshRequest ??= axios.post(
          `${apiBaseUrl}/auth/refresh`,
          {},
          { withCredentials: true, headers: { Accept: 'application/json' } },
        )
        const response = await refreshRequest
        setAccessToken(response.data.data.accessToken)
        return httpClient(originalRequest)
      } catch (refreshError) {
        clearAccessToken()
        return Promise.reject(normalizeApiError(refreshError))
      } finally {
        refreshRequest = null
      }
    }

    return Promise.reject(normalizeApiError(error))
  },
)

export default httpClient
