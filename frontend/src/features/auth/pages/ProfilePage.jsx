import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/authApi.js'
import FormMessage from '../components/FormMessage.jsx'
import { useAuth } from '../context/AuthContext.jsx'

function ProfilePage() {
  const { user, updateProfile, clearSession } = useAuth()
  const navigate = useNavigate()
  const [profile, setProfile] = useState({ fullName: '', displayName: '', phone: '', address: '' })
  const [profileState, setProfileState] = useState({ error: null, success: '', loading: false })
  const [password, setPassword] = useState({ currentPassword: '', otp: '', newPassword: '' })
  const [passwordState, setPasswordState] = useState({ error: null, success: '', loading: false })

  useEffect(() => {
    if (!user) return
    setProfile({
      fullName: user.fullName || '',
      displayName: user.displayName || '',
      phone: user.phone || '',
      address: user.address || '',
    })
  }, [user])

  async function saveProfile(event) {
    event.preventDefault()
    setProfileState({ error: null, success: '', loading: true })
    try {
      await updateProfile({ ...profile, phone: profile.phone || null, address: profile.address || null })
      setProfileState({ error: null, success: 'Đã cập nhật hồ sơ.', loading: false })
    } catch (error) {
      setProfileState({ error, success: '', loading: false })
    }
  }

  async function requestOtp() {
    setPasswordState({ error: null, success: '', loading: true })
    try {
      const response = await authApi.requestChangePasswordOtp()
      setPasswordState({ error: null, success: response.message, loading: false })
    } catch (error) {
      setPasswordState({ error, success: '', loading: false })
    }
  }

  async function changePassword(event) {
    event.preventDefault()
    setPasswordState({ error: null, success: '', loading: true })
    try {
      await authApi.changePassword(password)
      clearSession()
      navigate('/login', { replace: true })
    } catch (error) {
      setPasswordState({ error, success: '', loading: false })
    }
  }

  return (
    <section className="account-page" aria-labelledby="profile-title">
      <header className="page-heading">
        <p className="eyebrow">Tài khoản</p>
        <h1 id="profile-title">Hồ sơ của bạn</h1>
        <p>{user.email}</p>
        <div className="role-list" aria-label="Vai trò">
          {user.roles.map((role) => <span key={role}>{role}</span>)}
        </div>
      </header>

      <div className="account-grid">
        <form className="auth-form account-card" onSubmit={saveProfile}>
          <h2>Thông tin cá nhân</h2>
          <label>
            Họ và tên
            <input required maxLength="150" value={profile.fullName} onChange={(event) => setProfile({ ...profile, fullName: event.target.value })} />
          </label>
          <label>
            Tên hiển thị
            <input required maxLength="150" value={profile.displayName} onChange={(event) => setProfile({ ...profile, displayName: event.target.value })} />
          </label>
          <label>
            Số điện thoại
            <input inputMode="numeric" pattern="[0-9]{10}" value={profile.phone} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} />
          </label>
          <label>
            Địa chỉ
            <textarea maxLength="500" value={profile.address} onChange={(event) => setProfile({ ...profile, address: event.target.value })} />
          </label>
          <FormMessage error={profileState.error} success={profileState.success} />
          <button className="button button--primary" disabled={profileState.loading}>
            {profileState.loading ? 'Đang lưu…' : 'Lưu hồ sơ'}
          </button>
        </form>

        <form className="auth-form account-card" onSubmit={changePassword}>
          <h2>Đổi mật khẩu</h2>
          <label>
            Mật khẩu hiện tại
            <input type="password" required maxLength="32" autoComplete="current-password" value={password.currentPassword} onChange={(event) => setPassword({ ...password, currentPassword: event.target.value })} />
          </label>
          <div className="otp-row">
            <label>
              Mã OTP
              <input required inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={password.otp} onChange={(event) => setPassword({ ...password, otp: event.target.value })} />
            </label>
            <button className="button button--secondary" type="button" onClick={requestOtp} disabled={passwordState.loading}>
              Gửi OTP
            </button>
          </div>
          <label>
            Mật khẩu mới
            <input type="password" required minLength="8" maxLength="32" autoComplete="new-password" value={password.newPassword} onChange={(event) => setPassword({ ...password, newPassword: event.target.value })} />
          </label>
          <FormMessage error={passwordState.error} success={passwordState.success} />
          <button className="button button--primary" disabled={passwordState.loading}>
            {passwordState.loading ? 'Đang cập nhật…' : 'Đổi mật khẩu'}
          </button>
        </form>
      </div>
    </section>
  )
}

export default ProfilePage
