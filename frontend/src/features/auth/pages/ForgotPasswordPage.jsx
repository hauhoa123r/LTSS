import { useState } from 'react'
import { authApi } from '../api/authApi.js'
import AuthCard from '../components/AuthCard.jsx'
import FormMessage from '../../../shared/components/FormMessage.jsx'

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const response = await authApi.forgotPassword(email)
      setSuccess(response.message)
    } catch (requestError) {
      setError(requestError)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="Quên mật khẩu" description="Nếu tài khoản hợp lệ, LTSS sẽ gửi liên kết đặt lại mật khẩu.">
      <form className="auth-form" onSubmit={handleSubmit}>
        <label>
          Email
          <input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <FormMessage error={error} success={success} />
        <button className="button button--primary" disabled={submitting}>
          {submitting ? 'Đang gửi…' : 'Gửi liên kết'}
        </button>
      </form>
    </AuthCard>
  )
}

export default ForgotPasswordPage
