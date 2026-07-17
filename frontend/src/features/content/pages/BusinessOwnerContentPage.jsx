import { useEffect, useState } from 'react'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { businessOwnerApi } from '../api/businessOwnerApi.js'

const PAGE_SIZE = 5
const STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ xử lý',
  PUBLISHED: 'Đã xuất bản',
  ACTIVE: 'Đang hoạt động',
  REJECTED: 'Bị từ chối',
  EXPIRED: 'Đã hết hạn',
  ARCHIVED: 'Đã lưu trữ',
}

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function discountLabel(item) {
  if (!item.discountType || item.discountValue == null) return 'Ưu đãi đặc biệt'
  if (item.discountType === 'PERCENTAGE') return `Giảm ${Number(item.discountValue).toLocaleString('vi-VN')}%`
  if (item.discountType === 'FIXED_AMOUNT') return `Giảm ${Number(item.discountValue).toLocaleString('vi-VN')} đ`
  return Number(item.discountValue).toLocaleString('vi-VN')
}

function BusinessOwnerContentPage({ mode }) {
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [detail, setDetail] = useState({ item: null, loading: false, error: null })
  const isPosts = mode === 'posts'

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    const request = isPosts
      ? businessOwnerApi.posts({ page, size: PAGE_SIZE })
      : businessOwnerApi.promotions({ page, size: PAGE_SIZE })
    request
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [isPosts, page])

  const items = state.data?.content ?? []

  function openDetail(item) {
    setDetail({ item, loading: false, error: null })
  }
  return <section className="business-owner-page" aria-labelledby="business-content-title">
    <header className="page-heading"><p className="eyebrow">Không gian doanh nghiệp</p><h1 id="business-content-title">{isPosts ? 'Bài đăng' : 'Khuyến mãi'}</h1><p>{isPosts ? 'Theo dõi toàn bộ bài đăng thuộc doanh nghiệp của bạn.' : 'Theo dõi toàn bộ chương trình ưu đãi thuộc doanh nghiệp của bạn.'} Mỗi trang hiển thị 5 dòng.</p></header>
    {state.loading && <p className="form-status">Đang tải nội dung…</p>}
    {!state.loading && state.error && <FormMessage error={state.error} />}
    {!state.loading && !state.error && !items.length && <div className="discovery-empty"><h2>{isPosts ? 'Chưa có bài đăng' : 'Chưa có khuyến mãi'}</h2><p>Nội dung thuộc doanh nghiệp sẽ xuất hiện tại đây.</p></div>}
    {!state.loading && !state.error && items.length > 0 && <div className="admin-table-wrap business-owner-content-table">
      <table>
        <thead><tr>{isPosts ? <><th>Bài đăng</th><th>Trạng thái</th><th>Cập nhật</th></> : <><th>Khuyến mãi</th><th>Ưu đãi</th><th>Trạng thái</th><th>Thời gian áp dụng</th></>}<th>Thao tác</th></tr></thead>
        <tbody>{items.map((item) => <tr key={item.id}>
          <td data-label={isPosts ? 'Bài đăng' : 'Khuyến mãi'}><div className="business-owner-content-table__identity"><strong>{item.title}</strong><small>ID: #{item.id}</small><p>{isPosts ? item.summary : item.description}</p></div></td>
          {isPosts ? <><td data-label="Trạng thái"><span className={`admin-dashboard__status admin-dashboard__status--${item.status.toLowerCase()}`}>{STATUS_LABELS[item.status] || item.status}</span></td><td data-label="Cập nhật"><time dateTime={item.updatedAt}>{formatDate(item.updatedAt)}</time></td></> : <><td data-label="Ưu đãi"><div className="business-owner-content-table__offer"><strong>{discountLabel(item)}</strong><small>{item.promoCode ? `Mã: ${item.promoCode}` : 'Không yêu cầu mã'}</small></div></td><td data-label="Trạng thái"><span className={`admin-dashboard__status admin-dashboard__status--${item.status.toLowerCase()}`}>{STATUS_LABELS[item.status] || item.status}</span></td><td data-label="Thời gian áp dụng"><div className="business-owner-content-table__period"><time dateTime={item.startAt}>{formatDate(item.startAt)}</time><span>đến</span><time dateTime={item.endAt}>{formatDate(item.endAt)}</time></div></td></>}
          <td data-label="Thao tác"><button className="button button--secondary" type="button" onClick={() => openDetail(item)}>Xem chi tiết</button></td>
        </tr>)}</tbody>
      </table>
    </div>}
    {state.data?.totalPages > 1 && <nav className="pagination" aria-label={`Phân trang ${isPosts ? 'bài đăng' : 'khuyến mãi'}`}><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} nội dung</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}
    {detail.item && <div className="admin-action-modal__backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setDetail({ item: null, loading: false, error: null }) }}>
      <article className="admin-action-modal business-owner-detail-modal" role="dialog" aria-modal="true" aria-labelledby="business-content-detail-title">
        <header><div><p className="eyebrow">{isPosts ? 'Chi tiết bài đăng' : 'Chi tiết khuyến mãi'}</p><h2 id="business-content-detail-title">{detail.item.title}</h2></div><button type="button" aria-label="Đóng cửa sổ" onClick={() => setDetail({ item: null, loading: false, error: null })}>×</button></header>
        <div className="business-owner-detail-modal__body">
          {detail.loading && <p className="form-status">Đang tải nội dung…</p>}
          <FormMessage error={detail.error} />
          {!detail.loading && !detail.error && isPosts && <><p className="business-owner-detail-modal__summary">{detail.item.summary || 'Không có phần tóm tắt.'}</p><div className="business-owner-detail-modal__content">{detail.item.content}</div></>}
          {!detail.loading && !detail.error && !isPosts && <><div className="business-owner-detail-modal__offer"><strong>{discountLabel(detail.item)}</strong><span>{detail.item.promoCode ? `Mã ưu đãi: ${detail.item.promoCode}` : 'Không yêu cầu mã ưu đãi'}</span></div><div className="business-owner-detail-modal__content">{detail.item.description}</div><dl><div><dt>Bắt đầu</dt><dd>{formatDate(detail.item.startAt)}</dd></div><div><dt>Kết thúc</dt><dd>{formatDate(detail.item.endAt)}</dd></div></dl></>}
        </div>
      </article>
    </div>}
  </section>
}

export default BusinessOwnerContentPage
