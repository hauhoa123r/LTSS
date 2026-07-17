import { useEffect, useState } from 'react'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { administrationApi } from '../api/administrationApi.js'

const today = new Date().toISOString().slice(0, 10)
const monthAgo = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10)
const formatDate = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value))

function AuditLogsPage() {
  const [filter, setFilter] = useState({ from: monthAgo, to: today, action: '', entityType: '', actorId: '' })
  const [query, setQuery] = useState(filter)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  useEffect(() => { let active = true; setState({ data: null, loading: true, error: null }); administrationApi.auditLogs({ ...query, action: query.action || undefined, entityType: query.entityType || undefined, actorId: query.actorId || undefined, page: 0, size: 50 }).then((data) => active && setState({ data, loading: false, error: null })).catch((error) => active && setState({ data: null, loading: false, error })); return () => { active = false } }, [query])
  return <section className="admin-page"><header className="page-heading"><p className="eyebrow">Append-only audit</p><h1>Nhật ký kiểm toán</h1><p>API chỉ đọc; change detail được allowlist và loại bỏ khóa nhạy cảm trước khi trả về.</p></header><form className="admin-filters" onSubmit={(event) => { event.preventDefault(); setQuery(filter) }}><label>Từ<input type="date" value={filter.from} onChange={(event) => setFilter({ ...filter, from: event.target.value })} /></label><label>Đến<input type="date" value={filter.to} onChange={(event) => setFilter({ ...filter, to: event.target.value })} /></label><label>Action<input value={filter.action} maxLength="100" onChange={(event) => setFilter({ ...filter, action: event.target.value })} /></label><label>Entity<input value={filter.entityType} maxLength="100" onChange={(event) => setFilter({ ...filter, entityType: event.target.value })} /></label><label>Actor ID<input type="number" min="1" value={filter.actorId} onChange={(event) => setFilter({ ...filter, actorId: event.target.value })} /></label><button className="button button--primary">Lọc</button></form>{state.loading && <p className="form-status">Đang tải audit…</p>}<FormMessage error={state.error} />{state.data && <div className="admin-table-wrap"><table><thead><tr><th>Thời gian</th><th>Actor</th><th>Action</th><th>Target</th><th>Thay đổi an toàn</th><th>Request</th></tr></thead><tbody>{state.data.content.map((log) => <tr key={log.id}><td>{formatDate(log.createdAt)}</td><td>{log.actorUserId ? `#${log.actorUserId}` : 'SYSTEM'}</td><td>{log.actionCode}</td><td>{log.entityType} {log.entityId ? `#${log.entityId}` : ''}</td><td><code>{Object.keys(log.oldValues || {}).length || Object.keys(log.newValues || {}).length ? `${JSON.stringify(log.oldValues)} → ${JSON.stringify(log.newValues)}` : '—'}</code></td><td><code>{log.requestId || '—'}</code></td></tr>)}</tbody></table>{!state.data.content.length && <p>Không có audit log phù hợp.</p>}</div>}</section>
}

export default AuditLogsPage
