import { ApiError } from '../../../services/apiError.js'
import httpClient from '../../../services/apiClient.js'

function responseRequestId(response, envelope) {
  if (typeof response.headers?.get === 'function') {
    return envelope?.requestId ?? response.headers.get('x-request-id') ?? null
  }

  return envelope?.requestId ?? response.headers?.['x-request-id'] ?? null
}

function healthData(envelope) {
  if (envelope?.success === true && envelope.data && typeof envelope.data === 'object') {
    return envelope.data
  }

  return envelope
}

export async function getSystemHealth({ signal } = {}) {
  const response = await httpClient.get('/health', { signal })
  const payload = healthData(response.data)

  if (!payload || typeof payload !== 'object' || typeof payload.status !== 'string') {
    throw new ApiError({
      message: 'Backend trả về dữ liệu trạng thái không hợp lệ.',
      code: 'INVALID_HEALTH_RESPONSE',
      status: response.status,
      requestId: responseRequestId(response, response.data),
    })
  }

  return {
    status: payload.status,
    application: typeof payload.application === 'string' ? payload.application : null,
    requestId: responseRequestId(response, response.data),
  }
}
