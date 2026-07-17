import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { quizApi } from '../api/quizApi.js'

function QuizCatalogPage() {
  const [searchParams] = useSearchParams()
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [starting, setStarting] = useState({ id: null, error: null })
  const { user } = useAuth()
  const navigate = useNavigate()
  const placeId = searchParams.get('placeId') || undefined

  useEffect(() => {
    let active = true
    quizApi.published({ placeId, page: 0, size: 50 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [placeId])

  function start(quizId) {
    if (!user) { navigate('/login', { state: { from: '/quizzes' } }); return }
    if (!navigator.geolocation) {
      setStarting({ id: null, error: new Error('Trình duyệt không hỗ trợ xác định vị trí.') })
      return
    }
    setStarting({ id: quizId, error: null })
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => quizApi.start(quizId, { latitude: coords.latitude, longitude: coords.longitude })
        .then((attempt) => navigate(`/quiz-attempts/${attempt.id}`))
        .catch((error) => setStarting({ id: null, error })),
      () => setStarting({ id: null, error: new Error('Không thể xác minh vị trí. Hãy cấp quyền GPS và thử lại.') }),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 },
    )
  }

  return (
    <section className="quiz-page">
      <header className="content-hero">
        <p className="eyebrow">Quiz &amp; Gamification</p>
        <h1>Thử thách kiến thức ngay tại điểm đến.</h1>
        <p>Bạn cần ở trong bán kính 200 m của địa điểm để bắt đầu. Đồng hồ sẽ chạy liên tục sau khi xác minh.</p>
        {user && <p><Link to="/quiz-progress">Xem lịch sử và huy hiệu của tôi</Link></p>}
      </header>
      <FormMessage error={starting.error} />
      {state.loading && <p className="form-status">Đang tải quiz…</p>}
      {state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>Chưa có quiz được xuất bản</h2><p>Quiz đã qua kiểm duyệt sẽ xuất hiện tại đây.</p></div>}
      <div className="quiz-grid">
        {state.data?.content.map((quiz) => (
          <article className="quiz-card" key={quiz.id}>
            <p className="content-card__label">{quiz.placeName}</p>
            <h2>{quiz.title}</h2>
            <p>{quiz.description || 'Khám phá câu chuyện của điểm đến qua các câu hỏi ngắn.'}</p>
            <dl><div><dt>Câu hỏi</dt><dd>{quiz.questionCount}</dd></div><div><dt>Thời gian</dt><dd>{quiz.timeLimitSeconds} giây</dd></div><div><dt>Điểm đạt</dt><dd>{quiz.passingScorePercent}%</dd></div></dl>
            <button className="button button--primary" type="button" disabled={starting.id === quiz.id} onClick={() => start(quiz.id)}>{starting.id === quiz.id ? 'Đang xác minh GPS…' : 'Bắt đầu quiz'}</button>
          </article>
        ))}
      </div>
    </section>
  )
}

export default QuizCatalogPage
