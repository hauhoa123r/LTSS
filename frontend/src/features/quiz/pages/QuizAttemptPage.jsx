import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { quizApi } from '../api/quizApi.js'

function QuizAttemptPage() {
  const { attemptId } = useParams()
  const [state, setState] = useState({ attempt: null, loading: true, error: null })
  const [selected, setSelected] = useState({})
  const [remaining, setRemaining] = useState(0)
  const submitting = useRef(false)

  const load = useCallback(() => {
    quizApi.attempt(attemptId)
      .then((attempt) => {
        setState({ attempt, loading: false, error: null })
        setRemaining(Math.max(0, Math.ceil((new Date(attempt.expiresAt) - Date.now()) / 1000)))
      })
      .catch((error) => setState({ attempt: null, loading: false, error }))
  }, [attemptId])

  useEffect(() => { load() }, [load])

  const submit = useCallback(async (answers = null) => {
    if (submitting.current) return
    submitting.current = true
    setState((current) => ({ ...current, error: null }))
    try {
      const payload = answers ?? Object.entries(selected).map(([questionOrder, selectedAnswerId]) => ({ questionOrder: Number(questionOrder), selectedAnswerId }))
      const attempt = await quizApi.submit(attemptId, { answers: payload })
      setState({ attempt, loading: false, error: null })
    } catch (error) {
      setState((current) => ({ ...current, error }))
    } finally {
      submitting.current = false
    }
  }, [attemptId, selected])

  useEffect(() => {
    if (state.attempt?.status !== 'IN_PROGRESS') return undefined
    const timer = window.setInterval(() => {
      const next = Math.max(0, Math.ceil((new Date(state.attempt.expiresAt) - Date.now()) / 1000))
      setRemaining(next)
      if (next === 0) submit([])
    }, 500)
    return () => window.clearInterval(timer)
  }, [state.attempt, submit])

  if (state.loading) return <p className="form-status">Đang tải lượt chơi…</p>
  if (!state.attempt) return <section className="quiz-page"><FormMessage error={state.error} /></section>
  const { attempt } = state
  const terminal = attempt.status !== 'IN_PROGRESS'

  return (
    <section className="quiz-player">
      <header className="page-heading">
        <p className="eyebrow">{terminal ? 'Kết quả quiz' : `Còn ${remaining} giây`}</p>
        <h1>{attempt.quizTitle}</h1>
        {terminal && <p className={attempt.passed ? 'quiz-result quiz-result--passed' : 'quiz-result'}>{attempt.scorePercent}% · {attempt.passed ? 'Đạt' : 'Chưa đạt'} · {attempt.status === 'AUTO_SUBMITTED' ? 'Tự động nộp khi hết giờ' : 'Đã nộp'}</p>}
      </header>
      <FormMessage error={state.error} />
      <div className="quiz-questions">
        {attempt.questions.map((question) => (
          <fieldset className="quiz-question" key={question.questionOrder} disabled={terminal}>
            <legend>Câu {question.questionOrder}. {question.question}</legend>
            {question.choices.map((choice) => <label key={choice.id}><input type="radio" name={`q-${question.questionOrder}`} checked={selected[question.questionOrder] === choice.id} onChange={() => setSelected({ ...selected, [question.questionOrder]: choice.id })} /> {choice.content}</label>)}
            {terminal && <div className={question.correct ? 'quiz-answer quiz-answer--correct' : 'quiz-answer'}><strong>{question.correct ? 'Chính xác' : 'Chưa chính xác'}</strong><p>Bạn chọn: {question.selectedAnswer || 'Chưa trả lời'}</p><p>Đáp án đúng: {question.correctAnswer}</p>{question.explanation && <p>{question.explanation}</p>}</div>}
          </fieldset>
        ))}
      </div>
      {!terminal && <button className="button button--primary" type="button" disabled={submitting.current} onClick={() => submit()}>Nộp bài</button>}
      {terminal && <div className="quiz-result-actions"><Link className="button button--secondary" to="/quizzes">Quiz khác</Link><Link className="button button--primary" to="/quiz-progress">Lịch sử &amp; huy hiệu</Link></div>}
      {attempt.awardedBadges?.length > 0 && <aside className="badge-award"><h2>Huy hiệu mới</h2>{attempt.awardedBadges.map((badge) => <p key={badge.id}>🏅 <strong>{badge.name}</strong> — {badge.description}</p>)}</aside>}
    </section>
  )
}

export default QuizAttemptPage
