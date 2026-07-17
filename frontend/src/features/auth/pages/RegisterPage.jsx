import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/authApi.js'
import AuthCard from '../components/AuthCard.jsx'
import FormMessage from '../../../shared/components/FormMessage.jsx'

const initialForm = { fullName: '', displayName: '', email: '', password: '' }

function RegisterPage() {
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setSuccess('')
    try {
      const response = await authApi.register(form)
      setSuccess(response.message)
      setForm(initialForm)
    } catch (requestError) {
      setError(requestError)
    } finally {
      setSubmitting(false)
    }
  }

  function field(name, label, props = {}) {
    return (
      <label>
        {label}
        <input
          required
          value={form[name]}
          onChange={(event) => setForm({ ...form, [name]: event.target.value })}
          {...props}
        />
      </label>
    )
  }

  return (
    <AuthCard
      title="Tạo tài khoản"
      description="Tài khoản cần xác minh email trước khi đăng nhập."
      footer={
        <>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        {field('fullName', 'Họ và tên', { autoComplete: 'name', maxLength: 150 })}
        {field('displayName', 'Tên hiển thị', { maxLength: 150 })}
        {field('email', 'Email', { type: 'email', autoComplete: 'email', maxLength: 255 })}
        {field('password', 'Mật khẩu', {
          type: 'password',
          autoComplete: 'new-password',
          minLength: 8,
          maxLength: 32,
        })}
        <p className="form-hint">8–32 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.</p>
        <FormMessage error={error} success={success} />
        <button className="button button--primary" disabled={submitting}>
          {submitting ? 'Đang đăng ký…' : 'Đăng ký'}
        </button>
      </form>
    </AuthCard>
  )
}

export default RegisterPage
