function AuditEntityIdentity({ log, entityTypeLabels }) {
  const typeLabel = entityTypeLabels[log.entityType] || log.entityTypeLabel || log.entityType || 'Đối tượng'

  if (!log.entityId) return <strong>{log.entityDisplayName || typeLabel}</strong>

  const idLabel = log.entityType === 'USER' ? 'tài khoản' : typeLabel.toLocaleLowerCase('vi-VN')
  return <div className="audit-table__actor">
    <strong>{log.entityDisplayName || `Không tìm thấy ${typeLabel.toLocaleLowerCase('vi-VN')}`}</strong>
    <small>ID {idLabel}: #{log.entityId}</small>
  </div>
}

export default AuditEntityIdentity
