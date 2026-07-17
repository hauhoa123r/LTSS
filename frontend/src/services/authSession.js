let accessToken = null
let sessionListener = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token) {
  accessToken = token || null
  sessionListener?.(accessToken)
}

export function clearAccessToken() {
  setAccessToken(null)
}

export function onAccessTokenChanged(listener) {
  sessionListener = listener
  return () => {
    if (sessionListener === listener) sessionListener = null
  }
}
