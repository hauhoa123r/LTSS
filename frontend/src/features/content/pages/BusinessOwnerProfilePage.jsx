import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { businessOwnerApi } from '../api/businessOwnerApi.js'

/* ─── Style tokens ────────────────────────────────────────── */
const C = {
  gold: 'rgba(240,192,64,0.2)',
  goldBorder: '1px solid rgba(240,192,64,0.2)',
  cream: '#fff2c6',
  creamFaint: 'rgba(253,246,236,0.5)',
  bg: 'rgba(0,0,0,0.2)',
  inputBg: 'rgba(255,255,255,0.03)',
  inputBorder: '1px solid rgba(253,246,236,0.15)',
  red: '#d32f2f',
}

const inputStyle = {
  background: C.inputBg,
  border: C.inputBorder,
  borderRadius: '8px',
  padding: '10px 14px',
  color: 'rgba(253,246,236,0.9)',
  fontSize: '0.95rem',
  outline: 'none',
  width: '100%',
  boxSizing: 'border-box',
}

const labelStyle = { fontSize: '0.8rem', color: C.creamFaint, fontWeight: 600 }
const fieldStyle = { display: 'flex', flexDirection: 'column', gap: '6px' }

function Field({ label, htmlFor, children }) {
  return (
    <div style={fieldStyle}>
      {label && <label htmlFor={htmlFor} style={labelStyle}>{label}</label>}
      {children}
    </div>
  )
}

const STATUS_LABELS = {
  PENDING: 'Chờ phê duyệt',
  ACTIVE: 'Đang hoạt động',
  REJECTED: 'Bị từ chối',
  SUSPENDED: 'Tạm ngưng',
  INACTIVE: 'Ngừng hoạt động',
}

function BusinessOwnerProfilePage() {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [saving, setSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const [profileForm, setProfileForm] = useState({
    name: '',
    registrationNumber: '',
    contactEmail: '',
    websiteUrl: '',
    address: '',
    phone: '0987654321',
    facebook: 'facebook.com/sunriseresort',
    type: 'Khách sạn / Resort',
    description: 'Top 1 Sơn Tây',
    openTime: '08:00',
    closeTime: '22:00',
    activeDays: ['T2-T6', 'T7-CN'],
  })

  useEffect(() => {
    let active = true
    businessOwnerApi.profile()
      .then((data) => {
        if (active) {
          setState({ data, loading: false, error: null })
          if (data) {
            setProfileForm(prev => ({
              ...prev,
              name: data.place?.name || 'Sunrise Resort & Spa',
              registrationNumber: data.registrationNumber || 'LTSS-SUNRISE-BIZ-001',
              contactEmail: data.contactEmail || 'contact@sunriseresort.com',
              websiteUrl: data.websiteUrl || 'sunriseresort.com',
              address: data.place?.address || 'Phường Sơn Tây, Hà Nội',
            }))
          }
        }
      })
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [])

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setProfileForm(prev => ({ ...prev, [name]: value }))
  }

  const handleDayToggle = (day) => {
    setProfileForm(prev => {
      const activeDays = prev.activeDays.includes(day)
        ? prev.activeDays.filter(d => d !== day)
        : [...prev.activeDays, day]
      return { ...prev, activeDays }
    })
  }

  const handleSave = (e) => {
    e.preventDefault()
    setSaving(true)
    setSaveSuccess(false)
    setTimeout(() => {
      setSaving(false)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    }, 800)
  }

  const business = state.data

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: '24px', width: '100%' }} aria-labelledby="business-profile-title">
      <header className="page-heading">
        <p className="eyebrow">Không gian doanh nghiệp</p>
        <h1 id="business-profile-title">Hồ sơ doanh nghiệp</h1>
        <p>Thông tin công khai của cơ sở kinh doanh đang được liên kết với tài khoản của bạn.</p>
      </header>

      {state.loading && <p className="form-status">Đang tải hồ sơ doanh nghiệp…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}

      {business && (
        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

          {/* ── Cover & Logo ───────────────────────────────── */}
          <div style={{
            position: 'relative', borderRadius: '16px', overflow: 'visible',
            background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(240,192,64,0.1)',
            height: '240px',
          }}>
            {business.coverUrl ? (
              <img src={business.coverUrl} alt="Cover" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '16px' }} />
            ) : (
              <div style={{
                display: 'grid', placeItems: 'center', width: '100%', height: '100%',
                color: 'rgba(253,246,236,0.2)', fontSize: '3rem', borderRadius: '16px',
                background: 'linear-gradient(135deg,rgba(23,50,40,0.6),rgba(15,115,85,0.3))',
              }}>
                <span>◇ Banner Doanh Nghiệp</span>
              </div>
            )}

            {/* Upload cover btn */}
            <button type="button" onClick={() => {}}
              style={{
                position: 'absolute', top: '16px', right: '16px',
                padding: '8px 16px', borderRadius: '8px',
                border: '1px solid rgba(253,246,236,0.2)',
                background: 'rgba(255,255,255,0.05)', color: C.cream,
                fontSize: '0.85rem', cursor: 'pointer',
              }}>
              Thay ảnh bìa
            </button>

            {/* Logo container */}
            <div style={{
              position: 'absolute', bottom: '-40px', left: '40px',
              width: '120px', height: '120px', borderRadius: '20px',
              background: '#1e1e1e', border: '4px solid #142820',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              overflow: 'hidden', boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
            }}>
              {business.logoUrl ? (
                <img src={business.logoUrl} alt="Logo" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              ) : (
                <span style={{ color: 'rgba(253,246,236,0.3)', fontSize: '2.5rem' }} aria-hidden>🏢</span>
              )}
            </div>

            {/* Upload logo btn */}
            <button type="button" onClick={() => {}}
              style={{
                position: 'absolute', bottom: '-40px', left: '175px',
                padding: '8px 16px', borderRadius: '8px',
                border: '1px solid rgba(253,246,236,0.2)',
                background: 'rgba(255,255,255,0.05)', color: C.cream,
                fontSize: '0.85rem', cursor: 'pointer',
              }}>
              ↑ Tải logo mới
            </button>
          </div>

          {/* ── Main grid ────────────────────────────────── */}
          <div style={{ marginTop: '40px', display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '24px' }}>

            {/* Left card: info form */}
            <div style={{
              background: C.bg, border: '1px solid rgba(240,192,64,0.08)',
              borderRadius: '16px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px',
            }}>
              <h2 style={{
                fontSize: '1.15rem', fontWeight: 700, color: C.cream,
                borderBottom: '1px solid rgba(240,192,64,0.08)', paddingBottom: '12px', margin: 0,
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              }}>
                <span>DOANH NGHIỆP #{business.id}</span>
                <span className={`admin-dashboard__status admin-dashboard__status--${business.status.toLowerCase()}`}>
                  {STATUS_LABELS[business.status] || business.status}
                </span>
              </h2>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                {[
                  { id: 'biz-name', name: 'name', label: 'Tên cơ sở kinh doanh', required: true },
                  { id: 'biz-reg', name: 'registrationNumber', label: 'Mã đăng ký' },
                  { id: 'biz-email', name: 'contactEmail', label: 'Email liên hệ', type: 'email' },
                  { id: 'biz-web', name: 'websiteUrl', label: 'Website' },
                  { id: 'biz-phone', name: 'phone', label: 'Điện thoại' },
                  { id: 'biz-fb', name: 'facebook', label: 'Facebook' },
                ].map(({ id, name, label, type = 'text', required }) => (
                  <Field key={id} htmlFor={id} label={label}>
                    <input
                      id={id} name={name} type={type}
                      value={profileForm[name]} onChange={handleInputChange}
                      required={required}
                      style={inputStyle}
                    />
                  </Field>
                ))}

                <Field htmlFor="biz-type" label="Loại hình">
                  <select id="biz-type" name="type" value={profileForm.type} onChange={handleInputChange} style={inputStyle}>
                    <option value="Khách sạn / Resort">Khách sạn / Resort</option>
                    <option value="Nhà hàng / Quán ăn">Nhà hàng / Quán ăn</option>
                    <option value="Cửa hàng lưu niệm">Cửa hàng lưu niệm</option>
                    <option value="Dịch vụ cho thuê">Dịch vụ cho thuê</option>
                  </select>
                </Field>

                <Field htmlFor="biz-addr" label="Địa chỉ">
                  <input id="biz-addr" name="address" value={profileForm.address} onChange={handleInputChange} style={inputStyle} />
                </Field>
              </div>
            </div>

            {/* Right column: description + hours */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              {/* Description card */}
              <div style={{
                background: C.bg, border: '1px solid rgba(240,192,64,0.08)',
                borderRadius: '16px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px',
              }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: C.cream, margin: 0, borderBottom: '1px solid rgba(240,192,64,0.08)', paddingBottom: '12px' }}>
                  Mô tả doanh nghiệp
                </h3>
                <textarea
                  name="description"
                  value={profileForm.description}
                  onChange={handleInputChange}
                  placeholder="Mô tả tóm tắt về doanh nghiệp của bạn..."
                  style={{ ...inputStyle, minHeight: '100px', resize: 'vertical' }}
                />
              </div>

              {/* Hours card */}
              <div style={{
                background: C.bg, border: '1px solid rgba(240,192,64,0.08)',
                borderRadius: '16px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px',
              }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: C.cream, margin: 0, borderBottom: '1px solid rgba(240,192,64,0.08)', paddingBottom: '12px' }}>
                  Giờ hoạt động
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <Field label="Mở cửa">
                    <input type="time" name="openTime" value={profileForm.openTime} onChange={handleInputChange} style={inputStyle} />
                  </Field>
                  <Field label="Đóng cửa">
                    <input type="time" name="closeTime" value={profileForm.closeTime} onChange={handleInputChange} style={inputStyle} />
                  </Field>
                </div>
                <Field label="Ngày hoạt động trong tuần">
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '4px' }}>
                    {['T2-T6', 'T7-CN', 'Cả tuần'].map(day => {
                      const active = profileForm.activeDays.includes(day)
                      return (
                        <button
                          key={day} type="button"
                          onClick={() => handleDayToggle(day)}
                          style={{
                            padding: '6px 14px', borderRadius: '20px', fontSize: '0.85rem',
                            cursor: 'pointer', transition: 'all 0.2s',
                            background: active ? 'rgba(240,192,64,0.15)' : 'rgba(255,255,255,0.04)',
                            border: active ? '1px solid rgba(240,192,64,0.4)' : '1px solid rgba(253,246,236,0.1)',
                            color: active ? C.cream : C.creamFaint,
                            fontWeight: active ? 600 : 400,
                          }}
                        >
                          {day}
                        </button>
                      )
                    })}
                  </div>
                </Field>
              </div>
            </div>
          </div>

          {/* ── Footer ───────────────────────────────────── */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', alignItems: 'center' }}>
            {saveSuccess && (
              <span style={{ color: '#26c6da', marginRight: '4px' }}>✓ Lưu thông tin thành công!</span>
            )}
            <button type="submit" className="button button--primary" disabled={saving} style={{ background: C.red, borderColor: C.red }}>
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </div>

        </form>
      )}
    </section>
  )
}

export default BusinessOwnerProfilePage
