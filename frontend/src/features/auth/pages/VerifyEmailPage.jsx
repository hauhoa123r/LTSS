import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/authApi.js'
import AuthCard from '../components/AuthCard.jsx'
import FormMessage from '../components/FormMessage.jsx'

function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const started = useRef(false)
  const [state, setState] = useState({ loading: Boolean(token), error: null, success: '' })

  useEffect(() => {
    if (!token || started.current) return
    started.current = true
    authApi
      .verifyEmail(token)
      .then((response) => setState({ loading: false, error: null, success: response.message }))
      .catch((error) => setState({ loading: false, error, success: '' }))
  }, [token])

  return (
    <AuthCard title="Xác minh email" description="Liên kết chỉ sử dụng được một lần.">
      {!token && <FormMessage error={{ message: 'Liên kết xác minh không có token.' }} />}
      {state.loading && <p className="form-status">Đang xác minh…</p>}
      <FormMessage error={state.error} success={state.success} />
      {state.success && (
        <Link className="button button--primary" to="/login">
          Đăng nhập
        </Link>
      )}
    </AuthCard>
  )
}

export default VerifyEmailPage
