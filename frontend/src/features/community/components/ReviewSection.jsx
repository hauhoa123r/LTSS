import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { communityApi } from '../api/communityApi.js'

function Stars({ rating }) {
  return <span className="review-stars" aria-label={`${rating} trên 5 sao`}>{'★'.repeat(rating)}{'☆'.repeat(5 - rating)}</span>
}

function ReviewSection({ targetType, targetId }) {
  const { user } = useAuth()
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [form, setForm] = useState({ rating: 5, comment: '' })
  const [submitState, setSubmitState] = useState({ loading: false, error: null, success: '' })
  const [replying, setReplying] = useState({ id: null, content: '', loading: false, error: null })

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    communityApi.reviews({ targetType, targetId, size: 10 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [targetType, targetId])

  async function submit(event) {
    event.preventDefault()
    setSubmitState({ loading: true, error: null, success: '' })
    try {
      await communityApi.createReview(targetType, targetId, { ...form, mediaAssetIds: [] })
      setForm({ rating: 5, comment: '' })
      setSubmitState({ loading: false, error: null, success: 'Đánh giá đã được gửi và đang chờ kiểm duyệt.' })
    } catch (error) {
      setSubmitState({ loading: false, error, success: '' })
    }
  }

  async function sendReply(review) {
    setReplying((current) => ({ ...current, loading: true, error: null }))
    try {
      const updated = await communityApi.reply(review.id, { content: replying.content })
      setState((current) => ({ ...current, data: { ...current.data, content: current.data.content.map((item) => item.id === review.id ? updated : item) } }))
      setReplying({ id: null, content: '', loading: false, error: null })
    } catch (error) {
      setReplying((current) => ({ ...current, loading: false, error }))
    }
  }

  const canAttemptReply = targetType === 'BUSINESS' && user?.roles?.includes('BUSINESS_OWNER')

  return (
    <section className="review-section" aria-labelledby={`reviews-${targetType}-${targetId}`}>
      <header><div><p className="eyebrow">Cộng đồng</p><h2 id={`reviews-${targetType}-${targetId}`}>Đánh giá</h2></div><span>{state.data?.totalElements || 0} đánh giá</span></header>
      {user ? <form className="review-form" onSubmit={submit}><label>Chấm điểm<select value={form.rating} onChange={(event) => setForm({ ...form, rating: Number(event.target.value) })}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} sao</option>)}</select></label><label>Chia sẻ trải nghiệm<textarea required minLength="20" maxLength="5000" value={form.comment} onChange={(event) => setForm({ ...form, comment: event.target.value })} placeholder="Viết ít nhất 20 ký tự…" /></label><FormMessage error={submitState.error} success={submitState.success} /><button className="button button--primary" disabled={submitState.loading}>{submitState.loading ? 'Đang gửi…' : 'Gửi đánh giá'}</button></form> : <p className="review-login"><Link to="/login">Đăng nhập</Link> để viết đánh giá.</p>}
      {state.loading && <p className="form-status">Đang tải đánh giá…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && !state.data?.content.length && <p className="content-muted">Chưa có đánh giá đã được duyệt.</p>}
      {state.data?.content.length > 0 && <div className="review-list">{state.data.content.map((review) => <article className="review-item" key={review.id}><div className="review-item__author"><span>{review.reviewerAvatarUrl ? <img src={review.reviewerAvatarUrl} alt="" /> : review.reviewerName.slice(0, 1)}</span><div><strong>{review.reviewerName}</strong><Stars rating={review.rating} /></div></div><p>{review.comment}</p>{review.media?.length > 0 && <div className="review-media">{review.media.map((item) => <img key={item.id} src={item.thumbnailUrl || item.mediaUrl} alt="Ảnh đánh giá" loading="lazy" />)}</div>}{review.reply && <div className="review-reply"><strong>Phản hồi từ {review.reply.repliedByName}</strong><p>{review.reply.content}</p></div>}{canAttemptReply && !review.reply && (replying.id === review.id ? <div className="review-reply-form"><textarea maxLength="5000" value={replying.content} onChange={(event) => setReplying({ ...replying, content: event.target.value })} /><FormMessage error={replying.error} /><div><button className="button button--primary" type="button" disabled={replying.loading || !replying.content.trim()} onClick={() => sendReply(review)}>Gửi phản hồi</button><button className="button button--secondary" type="button" onClick={() => setReplying({ id: null, content: '', loading: false, error: null })}>Hủy</button></div></div> : <button className="review-reply-action" type="button" onClick={() => setReplying({ id: review.id, content: '', loading: false, error: null })}>Phản hồi chính thức</button>)}</article>)}</div>}
    </section>
  )
}

export default ReviewSection
