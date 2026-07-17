import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../api/moderationApi.js'

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function NotificationsPage({ workspace = false }) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [page, setPage] = useState(0)

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    moderationApi.notifications({ page, size: 20 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [page])

  async function markRead(item) {
    if (item.read) return
    const updated = await moderationApi.markNotificationRead(item.id)
    setState((current) => ({ ...current, data: { ...current.data, content: current.data.content.map((entry) => entry.id === item.id ? updated : entry) } }))
  }

  const workspaceCopy = workspace === 'moderation'
    ? 'Theo dõi nội dung mới được gửi và các cập nhật trong quy trình kiểm duyệt.'
    : workspace === 'relic-manager'
      ? 'Theo dõi kết quả kiểm duyệt và các cập nhật liên quan đến quiz bạn quản lý.'
      : 'Kết quả kiểm duyệt và các cập nhật liên quan đến tài khoản của bạn.'

  return (
    <section className="notification-page" aria-labelledby="notifications-title">
      <header className="page-heading"><p className="eyebrow">{workspace === 'moderation' ? 'Không gian kiểm duyệt' : workspace === 'relic-manager' ? 'Quản lý di tích' : 'Tài khoản'}</p><h1 id="notifications-title">Thông báo</h1><p>{workspaceCopy}</p></header>
      {state.loading && <p className="form-status">Đang tải thông báo…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>Chưa có thông báo</h2><p>Các cập nhật mới sẽ xuất hiện tại đây.</p></div>}
      {state.data?.content.length > 0 && <div className="notification-list">{state.data.content.map((item) => <article key={item.id} className={`notification-item ${item.read ? '' : 'notification-item--unread'}`}><div><h2>{item.title}</h2><p>{item.message}</p><time>{formatDate(item.createdAt)}</time></div><div>{item.actionUrl && <Link className="button button--secondary" to={item.actionUrl}>Xem</Link>}{!item.read && <button className="button button--secondary" type="button" onClick={() => markRead(item)}>Đã đọc</button>}</div></article>)}</div>}
      {state.data?.totalPages > 1 && <nav className="pagination"><button disabled={state.data.first} onClick={() => setPage(page - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages}</span><button disabled={state.data.last} onClick={() => setPage(page + 1)}>Trang sau</button></nav>}
    </section>
  )
}

export default NotificationsPage
