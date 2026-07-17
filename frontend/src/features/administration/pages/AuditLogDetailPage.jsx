import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { administrationApi } from '../api/administrationApi.js'
import { DEFAULT_AUDIT_ACTION_LABELS, DEFAULT_AUDIT_ENTITY_LABELS } from '../auditLabels.js'
import AuditEntityIdentity from '../components/AuditEntityIdentity.jsx'

const formatDate = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'full', timeStyle: 'medium' }).format(new Date(value))

function displayValue(value) {
  if (value === undefined || value === null || value === '') return 'Không có'
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  return String(value)
}

function DiffValue({ value }) {
  const text = displayValue(value)
  const [expanded, setExpanded] = useState(false)
  const isLong = text.length > 100 || text.includes('\n')
  return <div className={`audit-diff__value${expanded ? ' audit-diff__value--expanded' : ''}`}><span>{text}</span>{isLong && <button type="button" onClick={() => setExpanded((current) => !current)}>{expanded ? 'Thu gọn' : 'Mở rộng'}</button>}</div>
}

function AuditDiff({ oldValues, newValues }) {
  const [showAll, setShowAll] = useState(false)
  const fields = [...new Set([...Object.keys(oldValues), ...Object.keys(newValues)])]
  const changes = fields
    .map((field) => ({ field, oldValue: oldValues[field], newValue: newValues[field] }))
    .filter((item) => JSON.stringify(item.oldValue) !== JSON.stringify(item.newValue))
  const visibleChanges = showAll ? changes : changes.slice(0, 5)

  if (!changes.length) return <div className="admin-dashboard__empty"><strong>Không có thay đổi dữ liệu</strong><span>Hành động này chỉ ghi nhận sự kiện và không sửa trường dữ liệu nào.</span></div>

  return <div className="audit-diff">
    <div className="audit-diff__summary"><div><strong>{changes.length} trường đã thay đổi</strong><span>Chỉ hiển thị các giá trị khác nhau.</span></div>{changes.length > 5 && <button type="button" onClick={() => setShowAll((current) => !current)}>{showAll ? 'Thu gọn danh sách' : `Xem thêm ${changes.length - 5} thay đổi`}</button>}</div>
    <div className={`audit-diff__table${showAll ? ' audit-diff__table--expanded' : ''}`} role="table" aria-label="So sánh dữ liệu trước và sau thay đổi">
      <div className="audit-diff__head" role="row"><span role="columnheader">Trường thay đổi</span><span role="columnheader">Giá trị cũ</span><span role="columnheader">Giá trị mới</span></div>
      <div className="audit-diff__body">{visibleChanges.map((item) => <div className="audit-diff__row" role="row" key={item.field}><strong role="rowheader">{item.field}</strong><div role="cell" data-label="Giá trị cũ"><DiffValue value={item.oldValue} /></div><div role="cell" data-label="Giá trị mới"><DiffValue value={item.newValue} /></div></div>)}</div>
    </div>
  </div>
}

function RequestTrace({ requestId }) {
  const [copyStatus, setCopyStatus] = useState('idle')

  const copyRequestId = async () => {
    try {
      await navigator.clipboard.writeText(requestId)
      setCopyStatus('copied')
      window.setTimeout(() => setCopyStatus('idle'), 1800)
    } catch {
      setCopyStatus('error')
    }
  }

  if (!requestId) return <span className="audit-trace__empty">Không có mã truy vết</span>

  return <div className="audit-trace">
    <div className="audit-trace__value" title={requestId}><code>{requestId}</code></div>
    <button className="audit-trace__copy" type="button" onClick={copyRequestId}>
      {copyStatus === 'copied' ? 'Đã sao chép' : copyStatus === 'error' ? 'Không thể sao chép' : 'Sao chép'}
    </button>
    <small className="audit-trace__help">Dùng mã này để tra cứu yêu cầu liên quan trong hệ thống.</small>
    <span className="sr-only" aria-live="polite">{copyStatus === 'copied' ? `Đã sao chép mã truy vết ${requestId}` : copyStatus === 'error' ? 'Không thể sao chép mã truy vết' : ''}</span>
  </div>
}

function AuditLogDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigationLog = location.state?.auditLog || null
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [metadata, setMetadata] = useState({ actionLabels: DEFAULT_AUDIT_ACTION_LABELS, entityTypeLabels: DEFAULT_AUDIT_ENTITY_LABELS })

  useEffect(() => {
    let active = true
    setState({ data: navigationLog, loading: true, error: null })
    Promise.all([
      administrationApi.auditLog(id).catch((error) => {
        if (navigationLog && String(navigationLog.id) === String(id)) return navigationLog
        throw error
      }),
      administrationApi.auditMetadata().catch(() => null),
    ])
      .then(async ([data, apiMetadata]) => {
        const missingIds = [...new Set([
          data.actorUserId && !data.actorDisplayName ? data.actorUserId : null,
          data.entityType === 'USER' && data.entityId && !data.entityDisplayName ? data.entityId : null,
        ].filter(Boolean))]
        const accounts = await Promise.all(missingIds.map((accountId) => administrationApi.user(accountId).catch(() => null)))
        const names = new Map(accounts.filter(Boolean).map((account) => [account.id, account.displayName]))
        data = {
          ...data,
          actorDisplayName: data.actorDisplayName || names.get(data.actorUserId) || null,
          entityDisplayName: data.entityDisplayName || (data.entityType === 'USER' ? names.get(data.entityId) : null) || null,
        }
        return [data, apiMetadata]
      })
      .then(([data, apiMetadata]) => {
        if (!active) return
        if (apiMetadata) setMetadata({
          actionLabels: { ...DEFAULT_AUDIT_ACTION_LABELS, ...apiMetadata.actionLabels },
          entityTypeLabels: { ...DEFAULT_AUDIT_ENTITY_LABELS, ...apiMetadata.entityTypeLabels },
        })
        setState({ data, loading: false, error: null })
      })
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [id, navigationLog])

  const log = state.data
  const oldValues = log?.oldValuesDisplay || log?.oldValues || {}
  const newValues = log?.newValuesDisplay || log?.newValues || {}

  return (
    <section className="admin-page audit-detail" aria-labelledby="audit-detail-title">
      <header className="page-heading page-heading--actions">
        <div><p className="eyebrow">Nhật ký kiểm toán</p><h1 id="audit-detail-title">Chi tiết bản ghi #{id}</h1><p>Thông tin chỉ đọc phục vụ kiểm tra và đối soát hoạt động hệ thống.</p></div>
        <Link className="button button--secondary" to="/admin/audit-logs">← Danh sách nhật ký</Link>
      </header>

      {state.loading && !state.data && <p className="form-status">Đang tải chi tiết nhật ký…</p>}
      <FormMessage error={state.error} />
      {!state.error && log && <>
        <section className="audit-detail__overview">
          <div><p className="eyebrow">Hành động</p><h2>{metadata.actionLabels[log.actionCode] || log.actionLabel || log.actionCode}</h2><time dateTime={log.createdAt}>{formatDate(log.createdAt)}</time></div>
          <dl>
            <div><dt>Người thực hiện</dt><dd>{log.actorUserId ? <><strong>{log.actorDisplayName || 'Không tìm thấy tên tài khoản'}</strong><small>ID tài khoản: #{log.actorUserId}</small></> : 'Hệ thống'}</dd></div>
            <div><dt>Đối tượng</dt><dd><AuditEntityIdentity log={log} entityTypeLabels={metadata.entityTypeLabels} /></dd></div>
          </dl>
        </section>

        <section className="admin-user-detail__panel audit-detail__changes">
          <header><div><p className="eyebrow">Dữ liệu</p><h2>Chi tiết thay đổi</h2></div></header>
          <AuditDiff oldValues={oldValues} newValues={newValues} />
        </section>

        <section className="admin-user-detail__panel audit-detail__technical">
          <details>
            <summary>
              <div><p className="eyebrow">Đối soát</p><h2>Thông tin kỹ thuật</h2><span>Mở để xem mã phục vụ tra cứu nhật ký.</span></div>
              <span className="audit-detail__technical-toggle" aria-hidden="true">⌄</span>
            </summary>
            <dl>
              <div><dt>Mã bản ghi</dt><dd>#{log.id}</dd></div>
              <div className="audit-detail__trace-field"><dt>Mã truy vết</dt><dd><RequestTrace requestId={log.requestId} /></dd></div>
              <div><dt>Thời điểm ghi nhận</dt><dd>{formatDate(log.createdAt)}</dd></div>
            </dl>
          </details>
        </section>
      </>}
    </section>
  )
}

export default AuditLogDetailPage
