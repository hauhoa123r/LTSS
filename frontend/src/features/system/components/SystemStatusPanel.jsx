const STATUS_CONTENT = {
  loading: {
    label: 'Đang kiểm tra',
    title: 'Đang kết nối tới backend…',
  },
  success: {
    label: 'Hoạt động',
    title: 'Backend đang sẵn sàng',
  },
  unavailable: {
    label: 'Không khả dụng',
    title: 'Backend hiện không sẵn sàng',
  },
  error: {
    label: 'Có lỗi',
    title: 'Không thể kiểm tra trạng thái',
  },
}

function SystemStatusPanel({ kind, health = null, error = null, onRetry }) {
  const content = STATUS_CONTENT[kind]

  return (
    <section
      className={`status-panel status-panel--${kind}`}
      aria-live="polite"
      aria-busy={kind === 'loading'}
    >
      <div className="status-panel__heading">
        <span className="status-panel__indicator" aria-hidden="true" />
        <div>
          <p className="status-panel__label">{content.label}</p>
          <h2>{content.title}</h2>
        </div>
      </div>

      {kind === 'loading' && <p>Đang gửi yêu cầu GET /health…</p>}

      {health && (
        <dl className="status-details">
          <div>
            <dt>Trạng thái</dt>
            <dd>{health.status}</dd>
          </div>
          {health.application && (
            <div>
              <dt>Ứng dụng</dt>
              <dd>{health.application}</dd>
            </div>
          )}
          {health.requestId && (
            <div>
              <dt>Request ID</dt>
              <dd className="status-details__request-id">{health.requestId}</dd>
            </div>
          )}
        </dl>
      )}

      {error && <p className="status-panel__message">{error.message}</p>}

      {kind !== 'loading' && (
        <button className="button button--secondary" type="button" onClick={onRetry}>
          Kiểm tra lại
        </button>
      )}
    </section>
  )
}

export default SystemStatusPanel
