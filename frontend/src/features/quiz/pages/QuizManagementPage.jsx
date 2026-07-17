import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { moderationApi } from '../../moderation/api/moderationApi.js'
import { quizApi } from '../api/quizApi.js'

function QuizManagementPage() {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [action, setAction] = useState({ id: null, error: null })
  const load = useCallback(() => {
    setState({ data: null, loading: true, error: null })
    quizApi.mine({ page: 0, size: 50 }).then((data) => setState({ data, loading: false, error: null })).catch((error) => setState({ data: null, loading: false, error }))
  }, [])
  useEffect(() => load(), [load])

  async function submit(quiz) {
    setAction({ id: quiz.id, error: null })
    try { await moderationApi.submit('QUIZ', quiz.id, { targetVersion: quiz.version, note: 'Quiz đã sẵn sàng để kiểm duyệt.' }); load() }
    catch (error) { setAction({ id: null, error }) }
  }
  async function remove(quiz) {
    if (!window.confirm(`Xóa quiz “${quiz.title}”?`)) return
    setAction({ id: quiz.id, error: null })
    try { await quizApi.delete(quiz.id, quiz.version); load() }
    catch (error) { setAction({ id: null, error }) }
  }

  return <section className="quiz-page"><header className="page-heading page-heading--actions"><div><p className="eyebrow">Relic Manager</p><h1>Quản lý quiz</h1><p>Tạo bộ câu hỏi, gửi kiểm duyệt và theo dõi trạng thái xuất bản.</p></div><Link className="button button--primary" to="/manage/quizzes/new">Tạo quiz</Link></header><FormMessage error={state.error || action.error} />{state.loading && <p className="form-status">Đang tải quiz…</p>}{!state.loading && !state.data?.content.length && <div className="discovery-empty"><h2>Chưa có quiz</h2><p>Hãy tạo quiz đầu tiên cho một địa điểm đã xuất bản.</p></div>}<div className="management-list">{state.data?.content.map((quiz) => <article key={quiz.id}><div><p className="content-card__label">{quiz.placeName} · {quiz.status}</p><h2>{quiz.title}</h2><p>{quiz.questionCount} câu · {quiz.timeLimitSeconds} giây · đạt {quiz.passingScorePercent}%</p></div><div className="management-list__actions">{['DRAFT', 'REJECTED'].includes(quiz.status) && <><Link className="button button--secondary" to={`/manage/quizzes/${quiz.id}/edit`}>Chỉnh sửa</Link><button className="button button--primary" disabled={action.id === quiz.id} onClick={() => submit(quiz)}>Gửi duyệt</button><button className="button button--danger" disabled={action.id === quiz.id} onClick={() => remove(quiz)}>Xóa</button></>}</div></article>)}</div></section>
}

export default QuizManagementPage
