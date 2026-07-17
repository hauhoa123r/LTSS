import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../api/moderationApi.js'
import { moderationConfigFor, moderationPathFor } from '../moderationConfig.js'
import ModerationContentDetail from '../components/ModerationContentDetail.jsx'

const formatDate = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

function ModerationDetailPage() {
  const { caseId } = useParams()
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [decision, setDecision] = useState({ mode: null, reason: '', loading: false, error: null, success: null })

  useEffect(() => {
    let active = true
    moderationApi.detail(caseId)
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [caseId])

  async function submitDecision(event) {
    event.preventDefault()
    const item = state.data
    setDecision((current) => ({ ...current, loading: true, error: null, success: null }))
    try {
      const payload = { targetVersion: item.targetVersion, reason: decision.reason.trim() || null }
      const updated = decision.mode === 'approve'
        ? await moderationApi.approve(item.id, payload)
        : await moderationApi.reject(item.id, payload)
      setState((current) => ({ ...current, data: { ...current.data, ...updated, targetContent: current.data.targetContent } }))
      setDecision({ mode: null, reason: '', loading: false, error: null, success: decision.mode === 'approve' ? 'Nội dung đã được duyệt.' : 'Nội dung đã bị từ chối.' })
    } catch (error) {
      setDecision((current) => ({ ...current, loading: false, error }))
    }
  }

  const item = state.data
  const content = item?.targetContent
  const recordPending = item?.status === 'PENDING'
  const stale = recordPending && item?.actionable === false
  const pending = recordPending && !stale
  const targetConfig = moderationConfigFor(item?.targetType)
  const queuePath = moderationPathFor(item?.targetType)

  return (
    <section className="moderation-page moderation-detail" aria-labelledby="moderation-detail-title">
      <header className="page-heading page-heading--actions">
        <div><p className="eyebrow">Kiểm duyệt nội dung</p><h1 id="moderation-detail-title">Chi tiết yêu cầu #{caseId}</h1><p>Đọc toàn bộ nội dung trước khi đưa ra quyết định.</p></div>
        <Link className="button button--secondary" to={queuePath}>← {targetConfig.label}</Link>
      </header>

      {state.loading && <p className="form-status">Đang tải nội dung kiểm duyệt…</p>}
      <FormMessage error={state.error} />

      {!state.loading && !state.error && item && <>
        <section className="moderation-detail__overview">
          <div><p>{targetConfig.label}</p><h2>{item.targetTitle}</h2><span>ID đối tượng: #{item.targetId}</span></div>
          <dl>
            <div><dt>Người gửi</dt><dd><span className="moderation-queue-table__submitter"><strong>{item.submittedByDisplayName || 'Không tìm thấy tên tài khoản'}</strong><small>ID tài khoản: #{item.submittedByUserId}</small></span></dd></div>
            <div><dt>Thời gian gửi</dt><dd>{formatDate(item.submittedAt)}</dd></div>
            <div><dt>Trạng thái</dt><dd><span className={`moderation-status moderation-status--${stale ? 'stale' : item.status.toLowerCase()}`}>{stale ? 'Không còn chờ duyệt' : pending ? 'Chờ xử lý' : 'Đã xử lý'}</span></dd></div>
          </dl>
        </section>

        {item.submissionNote && <aside className="moderation-detail__submission-note"><strong>Ghi chú của người gửi</strong><p>{item.submissionNote}</p></aside>}

        <div className="moderation-detail__layout">
          <main className="moderation-detail__content">
            <ModerationContentDetail targetType={item.targetType} content={content} />
          </main>

          <aside className="moderation-detail__decision">
            <header><p className="eyebrow">Quyết định</p><h2>{pending ? 'Xử lý nội dung' : 'Kết quả kiểm duyệt'}</h2></header>
            {decision.success && <p className="moderation-detail__success" role="status">{decision.success}</p>}
            {stale && <div className="moderation-detail__resolved moderation-detail__resolved--stale"><strong>Không thể tiếp tục xử lý</strong><p>Nội dung đã thay đổi trạng thái và không còn nằm trong quy trình chờ duyệt. Hồ sơ này được giữ lại để đối soát.</p></div>}
            {!pending && !stale && <div className="moderation-detail__resolved"><strong>{item.decision === 'APPROVED' ? 'Đã duyệt' : item.decision === 'REJECTED' ? 'Đã từ chối' : 'Đã xử lý'}</strong>{item.decisionReason && <p>{item.decisionReason}</p>}{item.resolvedAt && <small>{formatDate(item.resolvedAt)}</small>}</div>}
            {pending && !decision.mode && <div className="moderation-detail__decision-actions"><button className="button button--primary" type="button" onClick={() => setDecision({ mode: 'approve', reason: '', loading: false, error: null, success: null })}>Duyệt nội dung</button><button className="button button--danger" type="button" onClick={() => setDecision({ mode: 'reject', reason: '', loading: false, error: null, success: null })}>Từ chối</button></div>}
            {pending && decision.mode && <form className="moderation-decision" onSubmit={submitDecision}>
              <label>{decision.mode === 'reject' ? 'Lý do từ chối (bắt buộc)' : 'Ghi chú quyết định'}<textarea autoFocus required={decision.mode === 'reject'} maxLength="1000" value={decision.reason} placeholder={decision.mode === 'reject' ? 'Nêu rõ nội dung cần chỉnh sửa…' : 'Ghi chú thêm nếu cần…'} onChange={(event) => setDecision({ ...decision, reason: event.target.value })} /></label>
              <FormMessage error={decision.error} />
              <div><button className={`button ${decision.mode === 'reject' ? 'button--danger' : 'button--primary'}`} disabled={decision.loading || (decision.mode === 'reject' && !decision.reason.trim())}>{decision.loading ? 'Đang xử lý…' : decision.mode === 'reject' ? 'Xác nhận từ chối' : 'Xác nhận duyệt'}</button><button className="button button--secondary" type="button" disabled={decision.loading} onClick={() => setDecision({ mode: null, reason: '', loading: false, error: null, success: null })}>Hủy</button></div>
            </form>}
          </aside>
        </div>
      </>}
    </section>
  )
}

export default ModerationDetailPage
