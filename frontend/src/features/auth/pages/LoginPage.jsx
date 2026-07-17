import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import AuthCard from '../components/AuthCard.jsx'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { useAuth } from '../context/AuthContext.jsx'

function authenticatedLandingPath(user) {
  if (user?.roles?.includes('ADMINISTRATOR')) return '/admin/dashboard'
  if (user?.roles?.includes('MODERATOR')) return '/moderation/articles'
  if (user?.roles?.includes('RELIC_MANAGER')) return '/relic-manager/articles'
  if (user?.roles?.includes('BUSINESS_OWNER')) return '/business-owner/overview'
  return '/'
}

function LoginPage() {
  const { login, isAuthenticated, user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to={authenticatedLandingPath(user)} replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const authenticatedUser = await login(form)
      navigate(authenticatedLandingPath(authenticatedUser), { replace: true })
    } catch (requestError) {
      setError(requestError)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Đăng nhập"
      description="Sử dụng tài khoản đã xác minh để tiếp tục."
      footer={
        <>
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label>
          Email
          <input
            type="email"
            autoComplete="email"
            required
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
          />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            autoComplete="current-password"
            required
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
          />
        </label>
        <Link className="auth-form__aside" to="/forgot-password">
          Quên mật khẩu?
        </Link>
        <FormMessage error={error} />
        <button className="button button--primary" disabled={submitting}>
          {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>
    </AuthCard>
  )
}

export default LoginPage
