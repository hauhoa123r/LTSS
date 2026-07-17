import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { businessOwnerApi } from '../api/businessOwnerApi.js'

const STATUS_LABELS = {
  PENDING: 'Chờ phê duyệt',
  ACTIVE: 'Đang hoạt động',
  REJECTED: 'Bị từ chối',
  SUSPENDED: 'Tạm ngưng',
  INACTIVE: 'Ngừng hoạt động',
}

const PLACE_STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ phê duyệt',
  PUBLISHED: 'Đã xuất bản',
  REJECTED: 'Bị từ chối',
  ARCHIVED: 'Đã lưu trữ',
  DELETED: 'Đã xóa',
}

function BusinessOwnerProfilePage() {
  const [state, setState] = useState({ data: null, loading: true, error: null })

  useEffect(() => {
    let active = true
    businessOwnerApi.profile()
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [])

  const business = state.data
  return <section className="business-owner-page" aria-labelledby="business-profile-title">
    <header className="page-heading"><p className="eyebrow">Không gian doanh nghiệp</p><h1 id="business-profile-title">Hồ sơ doanh nghiệp</h1><p>Thông tin công khai của cơ sở kinh doanh đang được liên kết với tài khoản của bạn.</p></header>
    {state.loading && <p className="form-status">Đang tải hồ sơ doanh nghiệp…</p>}
    {!state.loading && state.error && <FormMessage error={state.error} />}
    {business && <div className="business-owner-profile">
      <div className="business-owner-profile__cover">{business.coverUrl ? <img src={business.coverUrl} alt="" /> : <span aria-hidden="true">◇</span>}</div>
      <section>
        <div><p className="eyebrow">Doanh nghiệp #{business.id}</p><h2>{business.place.name}</h2><span className={`admin-dashboard__status admin-dashboard__status--${business.status.toLowerCase()}`}>{STATUS_LABELS[business.status] || business.status}</span></div>
        <dl>
          <div><dt>Mã đăng ký</dt><dd>{business.registrationNumber || 'Chưa cập nhật'}</dd></div>
          <div><dt>Email liên hệ</dt><dd>{business.contactEmail || 'Chưa cập nhật'}</dd></div>
          <div><dt>Website</dt><dd>{business.websiteUrl ? <a href={business.websiteUrl} target="_blank" rel="noreferrer">{business.websiteUrl}</a> : 'Chưa cập nhật'}</dd></div>
          <div><dt>Địa chỉ</dt><dd>{business.place.address || 'Chưa cập nhật'}</dd></div>
          <div><dt>Đường dẫn địa điểm</dt><dd>{business.place.slug}</dd></div>
          <div><dt>ID địa điểm</dt><dd>#{business.place.id}</dd></div>
          <div><dt>Trạng thái địa điểm</dt><dd>{PLACE_STATUS_LABELS[business.placeStatus] || business.placeStatus}</dd></div>
        </dl>
        <footer><Link className="button button--primary" to="/business-owner/posts">Xem bài đăng</Link><Link className="button button--secondary" to="/business-owner/overview">Xem thống kê</Link></footer>
      </section>
    </div>}
  </section>
}

export default BusinessOwnerProfilePage
