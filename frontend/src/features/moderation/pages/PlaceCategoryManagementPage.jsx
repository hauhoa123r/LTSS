import { useCallback, useEffect, useState } from 'react'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../api/moderationApi.js'

const PAGE_SIZE = 5
const EMPTY_FORM = { name: '', description: '', active: true }

function formatDate(value) {
  if (!value) return 'Chưa cập nhật'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function PlaceCategoryManagementPage() {
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [dialog, setDialog] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [action, setAction] = useState({ loading: false, error: null })

  const load = useCallback(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    moderationApi.placeCategories({ page, size: PAGE_SIZE })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [page])

  useEffect(() => load(), [load])

  function openCreate() {
    setForm(EMPTY_FORM)
    setAction({ loading: false, error: null })
    setDialog({ type: 'create' })
  }

  function openEdit(category) {
    setForm({ name: category.name, description: category.description || '', active: category.active })
    setAction({ loading: false, error: null })
    setDialog({ type: 'edit', category })
  }

  function openDelete(category) {
    setAction({ loading: false, error: null })
    setDialog({ type: 'delete', category })
  }

  function openDetails(category) {
    setAction({ loading: true, error: null })
    setDialog({ type: 'details', category })
    moderationApi.getPlaceCategory(category.id)
      .then((data) => {
        setDialog({ type: 'details', category: data })
        setAction({ loading: false, error: null })
      })
      .catch((error) => {
        setAction({ loading: false, error })
      })
  }

  async function save(event) {
    event.preventDefault()
    setAction({ loading: true, error: null })
    const payload = { name: form.name.trim(), description: form.description.trim() || null, active: form.active }
    try {
      if (dialog.type === 'edit') await moderationApi.updatePlaceCategory(dialog.category.id, payload)
      else await moderationApi.createPlaceCategory(payload)
      setDialog(null)
      load()
    } catch (error) {
      setAction({ loading: false, error })
    }
  }

  async function deactivate() {
    setAction({ loading: true, error: null })
    try {
      await moderationApi.deletePlaceCategory(dialog.category.id)
      setDialog(null)
      load()
    } catch (error) {
      setAction({ loading: false, error })
    }
  }

  const categories = state.data?.content ?? []

  return <section className="article-category-page" aria-labelledby="place-category-title">
    <header className="page-heading page-heading--actions">
      <div><p className="eyebrow">Không gian kiểm duyệt</p><h1 id="place-category-title">Danh mục địa điểm</h1><p>Tổ chức các nhóm nội dung được người quản lý di tích sử dụng khi phân loại địa điểm. Mỗi trang hiển thị 5 danh mục.</p></div>
      <button className="button button--primary" type="button" onClick={openCreate}>Thêm danh mục</button>
    </header>

    {state.loading && <p className="form-status">Đang tải danh mục…</p>}
    {!state.loading && state.error && <FormMessage error={state.error} />}
    {!state.loading && !state.error && !categories.length && <div className="discovery-empty"><h2>Chưa có danh mục</h2><p>Hãy tạo danh mục đầu tiên để phân loại địa điểm.</p></div>}
    {!state.loading && !state.error && categories.length > 0 && <div className="admin-table-wrap article-category-table">
      <table>
        <thead><tr><th>Danh mục</th><th>Đường dẫn</th><th>Trạng thái</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
        <tbody>{categories.map((category) => <tr key={category.id}>
          <td data-label="Danh mục"><div className="article-category-table__identity"><strong>{category.name}</strong><small>ID danh mục: #{category.id}</small>{category.description && <p>{category.description}</p>}</div></td>
          <td data-label="Đường dẫn"><code>{category.slug}</code></td>
          <td data-label="Trạng thái"><span className={`admin-dashboard__status admin-dashboard__status--${category.active ? 'active' : 'disabled'}`}>{category.active ? 'Đang sử dụng' : 'Đã vô hiệu hóa'}</span></td>
          <td data-label="Cập nhật"><time dateTime={category.updatedAt}>{formatDate(category.updatedAt)}</time></td>
          <td data-label="Thao tác"><div className="article-category-table__actions"><button className="button button--secondary" type="button" onClick={() => openDetails(category)}>Chi tiết</button><button className="button button--secondary" type="button" onClick={() => openEdit(category)}>Chỉnh sửa</button>{category.active && <button className="button button--danger" type="button" onClick={() => openDelete(category)}>Xóa</button>}</div></td>
        </tr>)}</tbody>
      </table>
    </div>}

    {state.data?.totalPages > 1 && <nav className="pagination" aria-label="Phân trang danh mục địa điểm"><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} danh mục</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}

    {dialog && <div className="admin-action-modal__backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !action.loading) setDialog(null) }}>
      <section className="admin-action-modal article-category-modal" role="dialog" aria-modal="true" aria-labelledby="place-category-dialog-title">
        <header><div><p className="eyebrow">Quản lý danh mục địa điểm</p><h2 id="place-category-dialog-title">{dialog.type === 'create' ? 'Thêm danh mục' : dialog.type === 'edit' ? 'Chỉnh sửa danh mục' : dialog.type === 'details' ? 'Chi tiết danh mục' : 'Xóa danh mục'}</h2></div><button type="button" aria-label="Đóng cửa sổ" disabled={action.loading} onClick={() => setDialog(null)}>×</button></header>
        {dialog.type === 'delete' ? <><p>Danh mục “{dialog.category.name}” sẽ bị vô hiệu hóa và không còn xuất hiện khi tạo địa điểm mới. Các địa điểm hiện có vẫn được giữ nguyên.</p><div className="article-category-modal__delete"><FormMessage error={action.error} /><footer><button className="button button--secondary" type="button" disabled={action.loading} onClick={() => setDialog(null)}>Hủy</button><button className="button button--danger" type="button" disabled={action.loading} onClick={deactivate}>{action.loading ? 'Đang xử lý…' : 'Xác nhận xóa'}</button></footer></div></> : dialog.type === 'details' ? <div className="article-category-modal__details">
          {action.loading ? <p className="form-status">Đang tải...</p> : action.error ? <FormMessage error={action.error} /> : <div className="admin-details-list">
            <p><strong>ID:</strong> #{dialog.category.id}</p>
            <p><strong>Tên danh mục:</strong> {dialog.category.name}</p>
            <p><strong>Đường dẫn:</strong> <code>{dialog.category.slug}</code></p>
            <p><strong>Mô tả:</strong> {dialog.category.description || 'Không có'}</p>
            <p><strong>Trạng thái:</strong> {dialog.category.active ? 'Đang sử dụng' : 'Đã vô hiệu hóa'}</p>
            <p><strong>Ngày tạo:</strong> {formatDate(dialog.category.createdAt)}</p>
            <p><strong>Cập nhật lần cuối:</strong> {formatDate(dialog.category.updatedAt)}</p>
          </div>}
          <footer><button className="button button--secondary" type="button" onClick={() => setDialog(null)}>Đóng</button></footer>
        </div> : <form onSubmit={save}>
          <label htmlFor="category-name">Tên danh mục <span>*</span></label><input id="category-name" autoFocus required maxLength="100" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <div className="admin-action-modal__counter">{form.name.length}/100</div>
          <label htmlFor="category-description">Mô tả</label><textarea id="category-description" maxLength="500" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <div className="admin-action-modal__counter">{form.description.length}/500</div>
          <label className="article-category-modal__switch"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /><span>Cho phép sử dụng danh mục này</span></label>
          <FormMessage error={action.error} />
          <footer><button className="button button--secondary" type="button" disabled={action.loading} onClick={() => setDialog(null)}>Hủy</button><button className="button button--primary" disabled={action.loading}>{action.loading ? 'Đang lưu…' : 'Lưu danh mục'}</button></footer>
        </form>}
      </section>
    </div>}
  </section>
}

export default PlaceCategoryManagementPage
