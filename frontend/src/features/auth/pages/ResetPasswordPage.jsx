import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/authApi.js'
import AuthCard from '../components/AuthCard.jsx'
import FormMessage from '../../../shared/components/FormMessage.jsx'

function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const response = await authApi.resetPassword({ token, newPassword })
      setSuccess(response.message)
      setNewPassword('')
    } catch (requestError) {
      setError(requestError)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="Đặt lại mật khẩu" description="Mật khẩu mới không được trùng mật khẩu hiện tại hoặc lịch sử gần đây.">
      {!token ? (
        <FormMessage error={{ message: 'Liên kết đặt lại mật khẩu không có token.' }} />
      ) : (
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Mật khẩu mới
            <input
              type="password"
              required
              minLength="8"
              maxLength="32"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
          </label>
          <FormMessage error={error} success={success} />
          <button className="button button--primary" disabled={submitting || Boolean(success)}>
            {submitting ? 'Đang cập nhật…' : 'Đặt lại mật khẩu'}
          </button>
          {success && <Link to="/login">Đi đến đăng nhập</Link>}
        </form>
      )}
    </AuthCard>
  )
}

export default ResetPasswordPage
