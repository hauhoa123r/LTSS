import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { quizApi } from '../api/quizApi.js'

const formatDate = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

function QuizProgressPage() {
  const [state, setState] = useState({ history: null, badges: null, loading: true, error: null })
  useEffect(() => {
    Promise.all([quizApi.history({ page: 0, size: 50 }), quizApi.badges({ page: 0, size: 50 })])
      .then(([history, badges]) => setState({ history, badges, loading: false, error: null }))
      .catch((error) => setState({ history: null, badges: null, loading: false, error }))
  }, [])
  return <section className="quiz-page"><header className="page-heading"><p className="eyebrow">Thành tích cá nhân</p><h1>Lịch sử quiz và huy hiệu</h1></header>{state.loading && <p className="form-status">Đang tải thành tích…</p>}<FormMessage error={state.error} />{!state.loading && !state.error && <><h2>Huy hiệu</h2>{!state.badges.content.length ? <p>Bạn chưa nhận huy hiệu nào.</p> : <div className="badge-grid">{state.badges.content.map((badge) => <article className="badge-card" key={`${badge.id}-${badge.awardedAt}`}><span>{badge.iconUrl ? <img src={badge.iconUrl} alt="" /> : '🏅'}</span><h3>{badge.name}</h3><p>{badge.description}</p><small>{formatDate(badge.awardedAt)}</small></article>)}</div>}<h2>Lượt chơi</h2>{!state.history.content.length ? <p>Bạn chưa chơi quiz nào.</p> : <div className="attempt-history">{state.history.content.map((attempt) => <article key={attempt.id}><div><strong>{attempt.quizTitle}</strong><p>{formatDate(attempt.startedAt)} · {attempt.status}</p></div><span>{attempt.scorePercent}%</span><Link to={`/quiz-attempts/${attempt.id}`}>Xem</Link></article>)}</div>}</>}</section>
}

export default QuizProgressPage
