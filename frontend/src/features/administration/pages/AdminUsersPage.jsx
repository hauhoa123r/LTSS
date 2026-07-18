import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { administrationApi } from '../api/administrationApi.js'
import { roleLabel } from '../roleLabels.js'

const STATUS_LABELS = {
  PENDING_VERIFICATION: 'Chờ xác minh',
  ACTIVE: 'Đang hoạt động',
  DEACTIVATED: 'Đã vô hiệu hóa',
  SUSPENDED: 'Tạm ngưng',
  DELETED: 'Đã xóa',
}

function formatDate(value) {
  if (!value) return 'Chưa đăng nhập'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}

function AdminUsersPage() {
  const [filter, setFilter] = useState({ q: '', status: '' })
  const [query, setQuery] = useState(filter)
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const load = useCallback(() => { setState({ data: null, loading: true, error: null }); administrationApi.users({ q: query.q || undefined, status: query.status || undefined, page, size: 5 }).then((data) => setState({ data, loading: false, error: null })).catch((error) => setState({ data: null, loading: false, error })) }, [page, query])
  useEffect(() => load(), [load])

  return <section className="admin-page"><header className="page-heading"><p className="eyebrow">Quản trị hệ thống</p><h1>Người dùng và vai trò</h1><p>Mỗi trang hiển thị 5 người dùng. Mở chi tiết để xem thông tin và thực hiện thay đổi có kiểm soát.</p></header><form className="admin-filters" onSubmit={(event) => { event.preventDefault(); setPage(0); setQuery(filter) }}><label>Tìm kiếm<input value={filter.q} maxLength="200" placeholder="Tên hoặc email" onChange={(event) => setFilter({ ...filter, q: event.target.value })} /></label><label>Trạng thái<select value={filter.status} onChange={(event) => setFilter({ ...filter, status: event.target.value })}><option value="">Tất cả</option>{['PENDING_VERIFICATION', 'ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'DELETED'].map((status) => <option key={status}>{STATUS_LABELS[status]}</option>)}</select></label><button className="button button--primary">Lọc</button></form><FormMessage error={state.error} />{state.loading && <p className="form-status">Đang tải người dùng…</p>}{state.data && <><div className="admin-table-wrap admin-users-table"><table><thead><tr><th>Người dùng</th><th>Trạng thái</th><th>Đăng nhập gần nhất</th><th>Vai trò</th><th>Thao tác</th></tr></thead><tbody>{state.data.content.map((account) => <tr key={account.id}><td className="admin-users-table__identity"><strong>{account.displayName}</strong><a href={`mailto:${account.email}`}>{account.email}</a><small>#{account.id} · v{account.version}</small></td><td><span className={`admin-dashboard__status admin-dashboard__status--${account.status.toLowerCase()}`}>{STATUS_LABELS[account.status] || account.status}</span></td><td>{formatDate(account.lastLoginAt)}</td><td>{account.effectiveRoles.map((role) => roleLabel(account, role)).join(', ')}</td><td className="admin-users-table__action-cell"><Link className="button button--secondary admin-users-table__detail-link" to={`/admin/users/${account.id}`}>Xem chi tiết</Link></td></tr>)}</tbody></table>{!state.data.content.length && <p>Không tìm thấy người dùng phù hợp.</p>}</div>{state.data.totalPages > 1 && <nav className="pagination" aria-label="Phân trang người dùng"><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} người dùng</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}</>}</section>
}

export default AdminUsersPage
