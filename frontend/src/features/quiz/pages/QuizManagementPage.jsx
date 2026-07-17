import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { moderationApi } from '../../moderation/api/moderationApi.js'
import { quizApi } from '../api/quizApi.js'

const PAGE_SIZE = 5
const EDITABLE_STATUSES = ['DRAFT', 'REJECTED']
const STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ kiểm duyệt',
  PUBLISHED: 'Đã xuất bản',
  REJECTED: 'Bị từ chối',
  DELETED: 'Đã xóa',
}

function QuizManagementPage() {
  const [page, setPage] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [action, setAction] = useState({ id: null, error: null })
  const [confirmingQuiz, setConfirmingQuiz] = useState(null)

  const load = useCallback(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    quizApi.mine({ page, size: PAGE_SIZE })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [page])

  useEffect(() => load(), [load])

  async function submit(quiz) {
    setAction({ id: quiz.id, error: null })
    try {
      await moderationApi.submit('QUIZ', quiz.id, { targetVersion: quiz.version, note: 'Quiz đã sẵn sàng để kiểm duyệt.' })
      setAction({ id: null, error: null })
      load()
    } catch (error) {
      setAction({ id: null, error })
    }
  }

  async function removeConfirmed(event) {
    event.preventDefault()
    const quiz = confirmingQuiz
    setAction({ id: quiz.id, error: null })
    try {
      await quizApi.delete(quiz.id, quiz.version)
      setConfirmingQuiz(null)
      setAction({ id: null, error: null })
      if (state.data?.content.length === 1 && page > 0) setPage((current) => current - 1)
      else load()
    } catch (error) {
      setAction({ id: null, error })
    }
  }

  const quizzes = state.data?.content ?? []

  return <section className="quiz-page relic-quiz-page">
    <header className="page-heading page-heading--actions">
      <div><p className="eyebrow">Quản lý di tích</p><h1>Danh sách quiz</h1><p>Quản lý quiz do bạn biên soạn. Mỗi trang hiển thị 5 nội dung.</p></div>
      <Link className="button button--primary" to="/relic-manager/quizzes/new">Tạo quiz mới</Link>
    </header>

    <FormMessage error={state.error || (!confirmingQuiz && action.error)} />
    {state.loading && <p className="form-status">Đang tải danh sách quiz…</p>}
    {!state.loading && !quizzes.length && <div className="discovery-empty"><h2>Chưa có quiz</h2><p>Hãy tạo quiz đầu tiên cho một địa điểm đã xuất bản.</p></div>}

    {!state.loading && quizzes.length > 0 && <div className="admin-table-wrap relic-quiz-table">
      <table>
        <thead><tr><th>Quiz</th><th>Địa điểm</th><th>Cấu hình</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>{quizzes.map((quiz) => {
          const editable = EDITABLE_STATUSES.includes(quiz.status)
          return <tr key={quiz.id}>
            <td data-label="Quiz"><div className="relic-quiz-table__identity"><strong>{quiz.title}</strong><small>ID quiz: #{quiz.id}</small></div></td>
            <td data-label="Địa điểm"><div className="relic-quiz-table__place"><strong>{quiz.placeName}</strong><small>ID địa điểm: #{quiz.placeId}</small></div></td>
            <td data-label="Cấu hình"><div className="relic-quiz-table__config"><span>{quiz.questionCount} câu · {quiz.timeLimitSeconds} giây</span><small>Điểm đạt: {quiz.passingScorePercent}%</small></div></td>
            <td data-label="Trạng thái"><span className={`admin-dashboard__status admin-dashboard__status--${quiz.status.toLowerCase()}`}>{STATUS_LABELS[quiz.status] || quiz.status}</span></td>
            <td data-label="Thao tác"><div className="relic-quiz-table__actions">{editable ? <>
              <Link className="button button--secondary" to={`/relic-manager/quizzes/${quiz.id}/edit`}>Chỉnh sửa</Link>
              <button className="button button--primary" type="button" disabled={action.id === quiz.id} onClick={() => submit(quiz)}>Gửi duyệt</button>
              <button className="button button--danger" type="button" disabled={action.id === quiz.id} onClick={() => { setAction({ id: null, error: null }); setConfirmingQuiz(quiz) }}>Xóa</button>
            </> : <span className="relic-quiz-table__locked">{quiz.status === 'PENDING' ? 'Đang chờ xử lý' : 'Không thể chỉnh sửa'}</span>}</div></td>
          </tr>
        })}</tbody>
      </table>
    </div>}

    {state.data?.totalPages > 1 && <nav className="pagination" aria-label="Phân trang quiz"><button type="button" disabled={state.data.first} onClick={() => setPage((current) => current - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages} · {state.data.totalElements} quiz</span><button type="button" disabled={state.data.last} onClick={() => setPage((current) => current + 1)}>Trang sau</button></nav>}

    {confirmingQuiz && <div className="admin-action-modal__backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && action.id === null) setConfirmingQuiz(null) }}>
      <section className="admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="delete-quiz-title">
        <header><div><p className="eyebrow">Xác nhận xóa</p><h2 id="delete-quiz-title">Xóa quiz</h2></div><button type="button" aria-label="Đóng cửa sổ" disabled={action.id !== null} onClick={() => setConfirmingQuiz(null)}>×</button></header>
        <p>Quiz “{confirmingQuiz.title}” sẽ bị xóa khỏi danh sách quản lý. Thao tác này chỉ áp dụng cho bản nháp hoặc nội dung bị từ chối.</p>
        <form onSubmit={removeConfirmed}><FormMessage error={action.error} /><footer><button className="button button--secondary" type="button" disabled={action.id !== null} onClick={() => setConfirmingQuiz(null)}>Hủy</button><button className="button button--danger" type="submit" disabled={action.id !== null}>{action.id !== null ? 'Đang xóa…' : 'Xác nhận xóa'}</button></footer></form>
      </section>
    </div>}
  </section>
}

export default QuizManagementPage
