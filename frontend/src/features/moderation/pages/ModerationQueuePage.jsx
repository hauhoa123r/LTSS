import { useCallback, useEffect, useState } from 'react'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { moderationApi } from '../api/moderationApi.js'

const TARGET_LABELS = {
  ARTICLE: 'Bài viết',
  EVENT: 'Sự kiện',
  BUSINESS_POST: 'Bài đăng doanh nghiệp',
  PROMOTION: 'Khuyến mãi',
  REVIEW: 'Đánh giá cộng đồng',
  QUIZ: 'Quiz điểm đến',
}

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function ModerationQueuePage() {
  const [targetType, setTargetType] = useState('')
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [decision, setDecision] = useState({ caseId: null, mode: null, reason: '', loading: false, error: null })

  const load = useCallback(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    moderationApi.queue({ targetType: targetType || undefined, page, size: 20 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [targetType, page])

  useEffect(() => load(), [load])

  function openDecision(caseId, mode) {
    setDecision({ caseId, mode, reason: '', loading: false, error: null })
  }

  async function resolve(item) {
    setDecision((current) => ({ ...current, loading: true, error: null }))
    try {
      const payload = { targetVersion: item.targetVersion, reason: decision.reason.trim() || null }
      if (decision.mode === 'approve') await moderationApi.approve(item.id, payload)
      else await moderationApi.reject(item.id, payload)
      setDecision({ caseId: null, mode: null, reason: '', loading: false, error: null })
      load()
    } catch (error) {
      setDecision((current) => ({ ...current, loading: false, error }))
    }
  }

  return (
    <section className="moderation-page" aria-labelledby="moderation-title">
      <header className="page-heading">
        <p className="eyebrow">Phase 5</p>
        <h1 id="moderation-title">Hàng đợi kiểm duyệt</h1>
        <p>Duyệt hoặc từ chối nội dung đang chờ. Mỗi quyết định được lưu cùng notification và audit log.</p>
      </header>

      <div className="moderation-toolbar">
        <label>Loại nội dung
          <select value={targetType} onChange={(event) => { setTargetType(event.target.value); setPage(0) }}>
            <option value="">Tất cả</option>
            {Object.entries(TARGET_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <button className="button button--secondary" type="button" onClick={load}>Làm mới</button>
      </div>

      {state.loading && <p className="form-status">Đang tải hàng đợi…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>Không còn nội dung chờ duyệt</h2><p>Hàng đợi hiện đã được xử lý hết.</p></div>}
      {!state.loading && state.data?.content.length > 0 && (
        <div className="moderation-list">
          {state.data.content.map((item) => (
            <article className="moderation-card" key={item.id}>
              <div className="moderation-card__heading">
                <div><p>{TARGET_LABELS[item.targetType]} · #{item.targetId}</p><h2>{item.targetTitle}</h2></div>
                <span>v{item.targetVersion}</span>
              </div>
              <dl><div><dt>Case</dt><dd>#{item.id}</dd></div><div><dt>Người gửi</dt><dd>#{item.submittedByUserId}</dd></div><div><dt>Thời gian</dt><dd>{formatDate(item.submittedAt)}</dd></div></dl>
              {item.submissionNote && <p className="moderation-card__note">“{item.submissionNote}”</p>}
              <div className="moderation-card__actions">
                <button className="button button--primary" type="button" onClick={() => openDecision(item.id, 'approve')}>Duyệt</button>
                <button className="button button--danger" type="button" onClick={() => openDecision(item.id, 'reject')}>Từ chối</button>
              </div>
              {decision.caseId === item.id && (
                <div className="moderation-decision">
                  <label>{decision.mode === 'reject' ? 'Lý do từ chối (bắt buộc)' : 'Ghi chú quyết định'}
                    <textarea required={decision.mode === 'reject'} maxLength="1000" value={decision.reason} onChange={(event) => setDecision({ ...decision, reason: event.target.value })} />
                  </label>
                  <FormMessage error={decision.error} />
                  <div><button className="button button--primary" type="button" disabled={decision.loading || (decision.mode === 'reject' && !decision.reason.trim())} onClick={() => resolve(item)}>{decision.loading ? 'Đang xử lý…' : 'Xác nhận'}</button><button className="button button--secondary" type="button" onClick={() => setDecision({ caseId: null, mode: null, reason: '', loading: false, error: null })}>Hủy</button></div>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
      {state.data?.totalPages > 1 && <nav className="pagination"><button disabled={state.data.first} onClick={() => setPage(page - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages}</span><button disabled={state.data.last} onClick={() => setPage(page + 1)}>Trang sau</button></nav>}
    </section>
  )
}

export default ModerationQueuePage
