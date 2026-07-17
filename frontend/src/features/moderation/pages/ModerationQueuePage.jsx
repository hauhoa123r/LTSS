import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../api/moderationApi.js'
import { moderationConfigFor } from '../moderationConfig.js'

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function ModerationQueuePage({ targetType }) {
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const config = moderationConfigFor(targetType)

  const load = useCallback(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    moderationApi.queue({ targetType, page, size: 5 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [targetType, page])

  useEffect(() => load(), [load])

  return (
    <section className="moderation-page" aria-labelledby="moderation-title">
      <header className="page-heading">
        <p className="eyebrow">Không gian kiểm duyệt</p>
        <h1 id="moderation-title">{config.title}</h1>
        <p>{config.description} Mỗi trang hiển thị 5 nội dung.</p>
      </header>

      <div className="moderation-toolbar">
        <div className="moderation-toolbar__context"><span>Hàng đợi hiện tại</span><strong>{config.label}</strong></div>
        <button className="button button--secondary" type="button" onClick={load}>Làm mới</button>
      </div>

      {state.loading && <p className="form-status">Đang tải hàng đợi…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>Không còn nội dung chờ duyệt</h2><p>{config.emptyMessage}</p></div>}
      {!state.loading && state.data?.content.length > 0 && (
        <div className="admin-table-wrap moderation-queue-table">
          <table>
            <thead><tr><th>Nội dung</th><th>Người gửi</th><th>Thời gian gửi</th><th>Thao tác</th></tr></thead>
            <tbody>{state.data.content.map((item) => (
              <tr key={item.id}>
                <td data-label="Nội dung"><div className="moderation-queue-table__content"><span>{config.label}</span><strong>{item.targetTitle}</strong><small>ID đối tượng: #{item.targetId} · Mã kiểm duyệt: #{item.id}</small></div></td>
                <td data-label="Người gửi"><div className="moderation-queue-table__submitter"><strong>{item.submittedByDisplayName || 'Không tìm thấy tên tài khoản'}</strong><small>ID tài khoản: #{item.submittedByUserId}</small></div></td>
                <td data-label="Thời gian gửi"><time dateTime={item.submittedAt}>{formatDate(item.submittedAt)}</time></td>
                <td data-label="Thao tác"><Link className="button button--secondary moderation-queue-table__detail" to={`${config.path}/${item.id}`}>Xem chi tiết</Link></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
      {state.data?.totalPages > 1 && <nav className="pagination" aria-label="Phân trang hàng đợi kiểm duyệt"><button type="button" disabled={state.data.first} onClick={() => setPage(page - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} nội dung</span><button type="button" disabled={state.data.last} onClick={() => setPage(page + 1)}>Trang sau</button></nav>}
    </section>
  )
}

export default ModerationQueuePage
