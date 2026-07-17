import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../../moderation/api/moderationApi.js'
import { contentApi } from '../api/contentApi.js'

const PAGE_SIZE = 5
const EDITABLE_STATUSES = ['DRAFT', 'REJECTED']
const STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ kiểm duyệt',
  PUBLISHED: 'Đã xuất bản',
  REJECTED: 'Bị từ chối',
  ARCHIVED: 'Đã lưu trữ',
  DELETED: 'Đã xóa',
}

function RelicArticleManagementPage() {
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [action, setAction] = useState({ id: null, error: null })
  const [confirmingArticle, setConfirmingArticle] = useState(null)

  const load = useCallback(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    contentApi.managedArticles({ page, size: PAGE_SIZE })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [page])

  useEffect(() => load(), [load])

  async function submit(article) {
    setAction({ id: article.id, error: null })
    try {
      await moderationApi.submit('ARTICLE', article.id, { targetVersion: article.version, note: 'Bài viết quảng bá đã sẵn sàng để kiểm duyệt.' })
      setAction({ id: null, error: null })
      load()
    } catch (error) {
      setAction({ id: null, error })
    }
  }

  async function removeConfirmed(event) {
    event.preventDefault()
    const article = confirmingArticle
    setAction({ id: article.id, error: null })
    try {
      await contentApi.deleteArticle(article.id, article.version)
      setConfirmingArticle(null)
      setAction({ id: null, error: null })
      if (state.data?.content.length === 1 && page > 0) setPage((current) => current - 1)
      else load()
    } catch (error) {
      setAction({ id: null, error })
    }
  }

  const articles = state.data?.content ?? []

  return <section className="relic-article-page">
    <header className="page-heading page-heading--actions">
      <div><p className="eyebrow">Quản lý di tích</p><h1>Bài viết quảng bá</h1><p>Biên soạn nội dung giới thiệu khu du lịch và gửi kiểm duyệt trước khi xuất bản. Mỗi trang hiển thị 5 bài.</p></div>
      <Link className="button button--primary" to="/relic-manager/articles/new">Viết bài mới</Link>
    </header>

    <FormMessage error={state.error || (!confirmingArticle && action.error)} />
    {state.loading && <p className="form-status">Đang tải bài viết…</p>}
    {!state.loading && !articles.length && <div className="discovery-empty"><h2>Chưa có bài viết</h2><p>Hãy tạo nội dung quảng bá đầu tiên cho một địa điểm đã xuất bản.</p></div>}

    {!state.loading && articles.length > 0 && <div className="admin-table-wrap relic-article-table">
      <table>
        <thead><tr><th>Bài viết</th><th>Khu du lịch</th><th>Chuyên mục</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>{articles.map((article) => {
          const editable = EDITABLE_STATUSES.includes(article.status)
          return <tr key={article.id}>
            <td data-label="Bài viết"><div className="relic-article-table__identity"><strong>{article.title}</strong><small>ID bài viết: #{article.id}</small>{article.summary && <p>{article.summary}</p>}</div></td>
            <td data-label="Khu du lịch"><div className="relic-article-table__place"><strong>{article.placeName}</strong><small>ID địa điểm: #{article.placeId}</small></div></td>
            <td data-label="Chuyên mục">{article.categoryName}</td>
            <td data-label="Trạng thái"><span className={`admin-dashboard__status admin-dashboard__status--${article.status.toLowerCase()}`}>{STATUS_LABELS[article.status] || article.status}</span></td>
            <td data-label="Thao tác"><div className="relic-article-table__actions">{editable ? <>
              <Link className="button button--secondary" to={`/relic-manager/articles/${article.id}/edit`}>Chỉnh sửa</Link>
              <button className="button button--primary" type="button" disabled={action.id === article.id} onClick={() => submit(article)}>Gửi duyệt</button>
              <button className="button button--danger" type="button" disabled={action.id === article.id} onClick={() => { setAction({ id: null, error: null }); setConfirmingArticle(article) }}>Xóa</button>
            </> : article.status === 'PUBLISHED' ? <Link className="button button--secondary" to={`/articles/${article.slug}`}>Xem bài</Link> : <span className="relic-article-table__locked">{article.status === 'PENDING' ? 'Đang chờ xử lý' : 'Không thể chỉnh sửa'}</span>}</div></td>
          </tr>
        })}</tbody>
      </table>
    </div>}

    {state.data?.totalPages > 1 && <nav className="pagination" aria-label="Phân trang bài viết"><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} bài viết</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}

    {confirmingArticle && <div className="admin-action-modal__backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && action.id === null) setConfirmingArticle(null) }}>
      <section className="admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="delete-article-title">
        <header><div><p className="eyebrow">Xác nhận xóa</p><h2 id="delete-article-title">Xóa bài viết</h2></div><button type="button" aria-label="Đóng cửa sổ" disabled={action.id !== null} onClick={() => setConfirmingArticle(null)}>×</button></header>
        <p>Bài viết “{confirmingArticle.title}” sẽ bị xóa khỏi danh sách quản lý. Chỉ bản nháp hoặc nội dung bị từ chối mới có thể xóa.</p>
        <form onSubmit={removeConfirmed}><FormMessage error={action.error} /><footer><button className="button button--secondary" type="button" disabled={action.id !== null} onClick={() => setConfirmingArticle(null)}>Hủy</button><button className="button button--danger" type="submit" disabled={action.id !== null}>{action.id !== null ? 'Đang xóa…' : 'Xác nhận xóa'}</button></footer></form>
      </section>
    </div>}
  </section>
}

export default RelicArticleManagementPage
