export const REQUEST_ID_HEADER = 'X-Request-Id'

function randomHex(bytes) {
  const values = new Uint8Array(bytes)
  globalThis.crypto.getRandomValues(values)
  return Array.from(values, (value) => value.toString(16).padStart(2, '0')).join('')
}

export function createRequestId() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }

  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    return `${randomHex(8)}-${randomHex(8)}`
  }

  return `ltss-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
}
