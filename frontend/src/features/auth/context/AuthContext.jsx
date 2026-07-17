import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { clearAccessToken, setAccessToken } from '../../../services/authSession.js'
import { authApi } from '../api/authApi.js'

const AuthContext = createContext(null)
let bootstrapSessionRequest = null

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isReady, setIsReady] = useState(false)

  const acceptSession = useCallback((session) => {
    setAccessToken(session.accessToken)
    setUser(session.user)
  }, [])

  const clearSession = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  useEffect(() => {
    let active = true

    bootstrapSessionRequest ??= authApi.refresh()
    bootstrapSessionRequest
      .then((session) => {
        if (active) acceptSession(session)
      })
      .catch(() => {
        if (active) clearSession()
      })
      .finally(() => {
        if (active) setIsReady(true)
      })

    return () => {
      active = false
    }
  }, [acceptSession, clearSession])

  const login = useCallback(
    async (credentials) => {
      const session = await authApi.login(credentials)
      acceptSession(session)
      return session.user
    },
    [acceptSession],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }, [clearSession])

  const updateProfile = useCallback(async (profile) => {
    const updated = await authApi.updateProfile(profile)
    setUser(updated)
    return updated
  }, [])

  const value = useMemo(
    () => ({ user, isReady, isAuthenticated: Boolean(user), login, logout, updateProfile, clearSession }),
    [user, isReady, login, logout, updateProfile, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
