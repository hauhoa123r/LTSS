import { isAxiosError } from 'axios'

const DEFAULT_ERROR_MESSAGE = 'Yêu cầu không thể hoàn tất. Vui lòng thử lại.'

function safeEnvelopeMessage(payload) {
  const isApiEnvelope =
    payload &&
    typeof payload === 'object' &&
    (payload.success === false || typeof payload.code === 'string')

  return isApiEnvelope && typeof payload.message === 'string' && payload.message.trim()
    ? payload.message.trim()
    : null
}

function statusMessage(status) {
  if (status === 400) return 'Yêu cầu không hợp lệ.'
  if (status === 401) return 'Yêu cầu cần được xác thực.'
  if (status === 403) return 'Bạn không có quyền thực hiện yêu cầu này.'
  if (status === 404) return 'Không tìm thấy tài nguyên được yêu cầu.'
  if (status === 409) return 'Dữ liệu đã thay đổi hoặc đang xung đột.'
  if (status === 429) return 'Có quá nhiều yêu cầu. Vui lòng thử lại sau.'
  if (status >= 500) return 'Backend đang gặp sự cố. Vui lòng thử lại sau.'
  return DEFAULT_ERROR_MESSAGE
}

function responseHeader(headers, name) {
  if (typeof headers?.get === 'function') {
    return headers.get(name)
  }

  return headers?.[name] ?? null
}

export class ApiError extends Error {
  constructor({
    message = DEFAULT_ERROR_MESSAGE,
    code = 'API_ERROR',
    status = null,
    requestId = null,
    fieldErrors = [],
    isNetworkError = false,
    isCanceled = false,
  } = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.requestId = requestId
    this.fieldErrors = Array.isArray(fieldErrors) ? fieldErrors : []
    this.isNetworkError = isNetworkError
    this.isCanceled = isCanceled
  }
}

export function normalizeApiError(error) {
  if (error instanceof ApiError) return error

  if (!isAxiosError(error)) {
    return new ApiError()
  }

  if (error.code === 'ERR_CANCELED') {
    return new ApiError({
      message: 'Yêu cầu đã bị hủy.',
      code: 'REQUEST_CANCELED',
      isCanceled: true,
    })
  }

  if (!error.response) {
    return new ApiError({
      message: 'Không thể kết nối tới backend. Hãy kiểm tra dịch vụ và thử lại.',
      code: 'BACKEND_UNAVAILABLE',
      isNetworkError: true,
    })
  }

  const { data, status, headers } = error.response
  const requestId = data?.requestId ?? responseHeader(headers, 'x-request-id')

  return new ApiError({
    message: safeEnvelopeMessage(data) ?? statusMessage(status),
    code: typeof data?.code === 'string' ? data.code : 'API_ERROR',
    status,
    requestId,
    fieldErrors:
      data?.error?.fieldErrors ?? data?.fieldErrors ?? data?.errors ?? [],
  })
}
