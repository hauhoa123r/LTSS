import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { administrationApi } from '../api/administrationApi.js'
import { DEFAULT_INTERNAL_AUDIT_ACTION_LABELS, DEFAULT_INTERNAL_AUDIT_ENTITY_LABELS } from '../auditLabels.js'
import AuditEntityIdentity from '../components/AuditEntityIdentity.jsx'

const today = new Date().toISOString().slice(0, 10)
const monthAgo = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10)
const formatDate = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value))

async function enrichAccountNames(data) {
  const missingIds = [...new Set(data.content.flatMap((log) => [
    log.actorUserId && !log.actorDisplayName ? log.actorUserId : null,
    log.entityType === 'USER' && log.entityId && !log.entityDisplayName ? log.entityId : null,
  ]).filter(Boolean))]
  if (!missingIds.length) return data
  const accounts = await Promise.all(missingIds.map((id) => administrationApi.user(id).catch(() => null)))
  const names = new Map(accounts.filter(Boolean).map((account) => [account.id, account.displayName]))
  return {
    ...data,
    content: data.content.map((log) => ({
      ...log,
      actorDisplayName: log.actorDisplayName || names.get(log.actorUserId) || null,
      entityDisplayName: log.entityDisplayName || (log.entityType === 'USER' ? names.get(log.entityId) : null) || null,
    })),
  }
}

function AuditLogsPage() {
  const [filter, setFilter] = useState({ from: monthAgo, to: today, action: '', entityType: '', actorId: '' })
  const [query, setQuery] = useState(filter)
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [metadata, setMetadata] = useState({ actionLabels: DEFAULT_INTERNAL_AUDIT_ACTION_LABELS, entityTypeLabels: DEFAULT_INTERNAL_AUDIT_ENTITY_LABELS })

  useEffect(() => {
    let active = true
    administrationApi.auditMetadata()
      .then((data) => active && setMetadata({
        actionLabels: { ...DEFAULT_INTERNAL_AUDIT_ACTION_LABELS, ...data.actionLabels },
        entityTypeLabels: { ...DEFAULT_INTERNAL_AUDIT_ENTITY_LABELS, ...data.entityTypeLabels },
      }))
      .catch(() => {})
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    administrationApi.auditLogs({
      ...query,
      action: query.action || undefined,
      entityType: query.entityType || undefined,
      actorId: query.actorId || undefined,
      page,
      size: 5,
    })
      .then(enrichAccountNames)
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [page, query])

  return (
    <section className="admin-page">
      <header className="page-heading">
        <p className="eyebrow">Nhật ký bất biến</p>
        <h1>Nhật ký kiểm toán</h1>
        <p>Theo dõi thay đổi quản trị người dùng và hoạt động kiểm duyệt nội bộ. Dữ liệu chỉ đọc và thông tin nhạy cảm đã được loại bỏ.</p>
      </header>

      <form className="admin-filters audit-filters audit-filter-card" aria-label="Bộ lọc nhật ký kiểm toán" onSubmit={(event) => { event.preventDefault(); setPage(0); setQuery(filter) }}>
        <header className="audit-filter-card__header">
          <div>
            <p className="eyebrow">Tìm kiếm nâng cao</p>
            <h2>Bộ lọc nhật ký</h2>
          </div>
          <p>Thu hẹp kết quả theo thời gian, hành động, đối tượng hoặc người thực hiện.</p>
        </header>

        <div className="audit-filter-grid">
          <label className="audit-filter-field">
            <span>Từ ngày</span>
            <input type="date" value={filter.from} max={filter.to} onChange={(event) => setFilter({ ...filter, from: event.target.value })} />
          </label>
          <label className="audit-filter-field">
            <span>Đến ngày</span>
            <input type="date" value={filter.to} min={filter.from} max={today} onChange={(event) => setFilter({ ...filter, to: event.target.value })} />
          </label>
          <label className="audit-filter-field">
            <span>Hành động</span>
            <select value={filter.action} onChange={(event) => setFilter({ ...filter, action: event.target.value })}>
              <option value="">Chọn hành động</option>
              {Object.entries(metadata.actionLabels).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label className="audit-filter-field">
            <span>Đối tượng</span>
            <select value={filter.entityType} onChange={(event) => setFilter({ ...filter, entityType: event.target.value })}>
              <option value="">Chọn đối tượng</option>
              {Object.entries(metadata.entityTypeLabels).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label className="audit-filter-field audit-filter-field--actor">
            <span>ID người thực hiện</span>
            <input type="number" min="1" value={filter.actorId} placeholder="ID người dùng" onChange={(event) => setFilter({ ...filter, actorId: event.target.value })} />
          </label>
          <div className="audit-filter-actions">
            <button className="button button--primary audit-filter-actions__apply" disabled={state.loading} aria-busy={state.loading}>
              {state.loading && <span className="audit-filter-actions__spinner" aria-hidden="true" />}
              {state.loading ? 'Đang áp dụng…' : 'Áp dụng bộ lọc'}
            </button>
          </div>
        </div>
      </form>

      {state.loading && <p className="form-status">Đang tải nhật ký kiểm toán…</p>}
      <FormMessage error={state.error} />
      {state.data && <>
        <div className="admin-table-wrap audit-table audit-table--summary">
          <table>
            <thead><tr><th>Thời gian</th><th>Người thực hiện</th><th>Hành động</th><th>Đối tượng</th><th>Thao tác</th></tr></thead>
            <tbody>{state.data.content.map((log) => <tr key={log.id}>
              <td>{formatDate(log.createdAt)}</td>
              <td>{log.actorUserId ? <div className="audit-table__actor"><strong>{log.actorDisplayName || 'Không tìm thấy tên tài khoản'}</strong><small>ID tài khoản: #{log.actorUserId}</small></div> : <strong className="audit-table__system">Hệ thống</strong>}</td>
              <td><strong className="audit-table__action">{metadata.actionLabels[log.actionCode] || log.actionLabel || log.actionCode}</strong></td>
              <td><AuditEntityIdentity log={log} entityTypeLabels={metadata.entityTypeLabels} /></td>
              <td><Link className="button button--secondary audit-table__detail-link" to={`/admin/audit-logs/${log.id}`} state={{ auditLog: log }}>Xem chi tiết</Link></td>
            </tr>)}</tbody>
          </table>
          {!state.data.content.length && <p>Không có nhật ký kiểm toán phù hợp với bộ lọc.</p>}
        </div>
        {state.data.totalPages > 1 && <nav className="pagination" aria-label="Phân trang nhật ký kiểm toán"><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} bản ghi</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}
      </>}
    </section>
  )
}

export default AuditLogsPage
