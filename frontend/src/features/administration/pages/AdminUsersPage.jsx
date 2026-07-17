import { useCallback, useEffect, useState } from 'react'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { administrationApi } from '../api/administrationApi.js'

const ROLES = ['TOURIST', 'BUSINESS_OWNER', 'RELIC_MANAGER', 'MODERATOR', 'ADMINISTRATOR']

function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const [filter, setFilter] = useState({ q: '', status: '' })
  const [query, setQuery] = useState(filter)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [actionError, setActionError] = useState(null)
  const load = useCallback(() => { setState({ data: null, loading: true, error: null }); administrationApi.users({ q: query.q || undefined, status: query.status || undefined, page: 0, size: 50 }).then((data) => setState({ data, loading: false, error: null })).catch((error) => setState({ data: null, loading: false, error })) }, [query])
  useEffect(() => load(), [load])

  async function changeStatus(account, status) {
    const reason = window.prompt(`Lý do chuyển tài khoản sang ${status}:`)
    if (!reason?.trim()) return
    try { await administrationApi.status(account.id, { status, reason: reason.trim(), version: account.version }); setActionError(null); load() } catch (error) { setActionError(error) }
  }
  async function toggleRole(account, role, assigned) {
    const reason = window.prompt(`Lý do ${assigned ? 'thu hồi' : 'gán'} role ${role}:`)
    if (!reason?.trim()) return
    try { if (assigned) await administrationApi.revokeRole(account.id, role, { reason: reason.trim() }); else await administrationApi.assignRole(account.id, role, { reason: reason.trim() }); setActionError(null); load() } catch (error) { setActionError(error) }
  }

  return <section className="admin-page"><header className="page-heading"><p className="eyebrow">Administration</p><h1>Người dùng và vai trò</h1><p>Mọi thay đổi đều yêu cầu lý do, revoke refresh token và ghi audit.</p></header><form className="admin-filters" onSubmit={(event) => { event.preventDefault(); setQuery(filter) }}><label>Tìm kiếm<input value={filter.q} maxLength="200" placeholder="Tên hoặc email" onChange={(event) => setFilter({ ...filter, q: event.target.value })} /></label><label>Trạng thái<select value={filter.status} onChange={(event) => setFilter({ ...filter, status: event.target.value })}><option value="">Tất cả</option>{['PENDING_VERIFICATION', 'ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'DELETED'].map((status) => <option key={status}>{status}</option>)}</select></label><button className="button button--primary">Lọc</button></form><FormMessage error={state.error || actionError} />{state.loading && <p className="form-status">Đang tải người dùng…</p>}<div className="admin-user-list">{state.data?.content.map((account) => { const self = account.id === currentUser?.id; return <article key={account.id}><header><div><p>#{account.id} · {account.status}</p><h2>{account.displayName}</h2><a href={`mailto:${account.email}`}>{account.email}</a></div><span>v{account.version}</span></header><div className="admin-user-actions"><strong>Trạng thái</strong>{account.status === 'ACTIVE' && <><button disabled={self} onClick={() => changeStatus(account, 'SUSPENDED')}>Tạm ngưng</button><button disabled={self} onClick={() => changeStatus(account, 'DEACTIVATED')}>Vô hiệu hóa</button></>}{['SUSPENDED', 'DEACTIVATED'].includes(account.status) && <button disabled={self} onClick={() => changeStatus(account, 'ACTIVE')}>Kích hoạt lại</button>}</div><div className="admin-role-grid">{ROLES.map((role) => { const assigned = account.directRoles.includes(role); return <label key={role}><input type="checkbox" checked={assigned} disabled={self} onChange={() => toggleRole(account, role, assigned)} /> {role}{!assigned && account.effectiveRoles.includes(role) ? ' (kế thừa)' : ''}</label> })}</div></article> })}</div></section>
}

export default AdminUsersPage
