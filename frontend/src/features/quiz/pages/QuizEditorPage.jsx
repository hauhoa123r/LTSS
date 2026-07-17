import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { placeApi } from '../../places/api/placeApi.js'
import { quizApi } from '../api/quizApi.js'

const newQuestion = () => ({ content: '', explanation: '', points: 1, answers: [{ content: '', correct: true }, { content: '', correct: false }] })
const initialForm = { placeId: '', title: '', description: '', timeLimitSeconds: 300, passingScorePercent: 60, questions: [newQuestion()], version: null }

function QuizEditorPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [places, setPlaces] = useState([])
  const [form, setForm] = useState(initialForm)
  const [state, setState] = useState({ loading: Boolean(id), saving: false, error: null })

  useEffect(() => {
    placeApi.search({ page: 0, size: 50 }).then((data) => setPlaces(data.content)).catch((error) => setState((current) => ({ ...current, error })))
    if (id) quizApi.managementDetail(id).then((quiz) => setForm({ placeId: quiz.placeId, title: quiz.title, description: quiz.description || '', timeLimitSeconds: quiz.timeLimitSeconds, passingScorePercent: quiz.passingScorePercent, version: quiz.version, questions: quiz.questions.map((question) => ({ content: question.content, explanation: question.explanation || '', points: question.points, answers: question.answers.map((answer) => ({ content: answer.content, correct: answer.correct })) })) })).catch((error) => setState((current) => ({ ...current, error }))).finally(() => setState((current) => ({ ...current, loading: false })))
  }, [id])

  function updateQuestion(index, field, value) { const questions = [...form.questions]; questions[index] = { ...questions[index], [field]: value }; setForm({ ...form, questions }) }
  function updateAnswer(questionIndex, answerIndex, field, value) { const questions = [...form.questions]; const answers = [...questions[questionIndex].answers]; answers[answerIndex] = { ...answers[answerIndex], [field]: value }; questions[questionIndex] = { ...questions[questionIndex], answers }; setForm({ ...form, questions }) }
  function markCorrect(questionIndex, answerIndex) { const questions = [...form.questions]; questions[questionIndex] = { ...questions[questionIndex], answers: questions[questionIndex].answers.map((answer, index) => ({ ...answer, correct: index === answerIndex })) }; setForm({ ...form, questions }) }
  function removeQuestion(index) { setForm({ ...form, questions: form.questions.filter((_, itemIndex) => itemIndex !== index) }) }
  function addAnswer(questionIndex) { const questions = [...form.questions]; questions[questionIndex] = { ...questions[questionIndex], answers: [...questions[questionIndex].answers, { content: '', correct: false }] }; setForm({ ...form, questions }) }
  function removeAnswer(questionIndex, answerIndex) { const questions = [...form.questions]; let answers = questions[questionIndex].answers.filter((_, index) => index !== answerIndex); if (!answers.some((answer) => answer.correct)) answers = answers.map((answer, index) => ({ ...answer, correct: index === 0 })); questions[questionIndex] = { ...questions[questionIndex], answers }; setForm({ ...form, questions }) }

  async function save(event) {
    event.preventDefault(); setState({ ...state, saving: true, error: null })
    const payload = { ...form, placeId: Number(form.placeId), timeLimitSeconds: Number(form.timeLimitSeconds), passingScorePercent: Number(form.passingScorePercent), questions: form.questions.map((question) => ({ ...question, points: Number(question.points) })) }
    try { if (id) await quizApi.update(id, payload); else await quizApi.create({ ...payload, version: null }); navigate('/manage/quizzes') }
    catch (error) { setState({ ...state, saving: false, error }) }
  }

  if (state.loading) return <p className="form-status">Đang tải quiz…</p>
  return <section className="quiz-editor"><header className="page-heading"><p className="eyebrow">Quiz authoring</p><h1>{id ? 'Chỉnh sửa quiz' : 'Tạo quiz mới'}</h1><p>Mỗi câu phải có 2–4 lựa chọn và đúng một đáp án chính xác.</p></header><form onSubmit={save}><FormMessage error={state.error} /><div className="quiz-editor__meta"><label>Địa điểm<select required disabled={Boolean(id)} value={form.placeId} onChange={(event) => setForm({ ...form, placeId: event.target.value })}><option value="">Chọn địa điểm</option>{places.map((place) => <option key={place.id} value={place.id}>{place.name}</option>)}</select></label><label>Tiêu đề<input required maxLength="250" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label><label>Mô tả<textarea maxLength="10000" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><label>Thời gian (giây)<input required type="number" min="1" max="600" value={form.timeLimitSeconds} onChange={(event) => setForm({ ...form, timeLimitSeconds: event.target.value })} /></label><label>Điểm đạt (%)<input required type="number" min="0" max="100" step="0.01" value={form.passingScorePercent} onChange={(event) => setForm({ ...form, passingScorePercent: event.target.value })} /></label></div><div className="quiz-editor__questions">{form.questions.map((question, questionIndex) => <fieldset key={questionIndex} className="quiz-editor__question"><legend>Câu {questionIndex + 1}</legend><label>Nội dung<input required maxLength="250" value={question.content} onChange={(event) => updateQuestion(questionIndex, 'content', event.target.value)} /></label><label>Giải thích<textarea maxLength="5000" value={question.explanation} onChange={(event) => updateQuestion(questionIndex, 'explanation', event.target.value)} /></label><label>Điểm<input required type="number" min="0.01" step="0.01" value={question.points} onChange={(event) => updateQuestion(questionIndex, 'points', event.target.value)} /></label><div className="quiz-editor__answers">{question.answers.map((answer, answerIndex) => <div key={answerIndex}><input type="radio" name={`correct-${questionIndex}`} checked={answer.correct} onChange={() => markCorrect(questionIndex, answerIndex)} aria-label={`Đáp án đúng ${answerIndex + 1}`} /><input required maxLength="100" value={answer.content} onChange={(event) => updateAnswer(questionIndex, answerIndex, 'content', event.target.value)} placeholder={`Đáp án ${answerIndex + 1}`} />{question.answers.length > 2 && <button type="button" onClick={() => removeAnswer(questionIndex, answerIndex)}>Xóa</button>}</div>)}</div>{question.answers.length < 4 && <button className="button button--secondary" type="button" onClick={() => addAnswer(questionIndex)}>Thêm đáp án</button>}{form.questions.length > 1 && <button className="button button--danger" type="button" onClick={() => removeQuestion(questionIndex)}>Xóa câu hỏi</button>}</fieldset>)}</div>{form.questions.length < 100 && <button className="button button--secondary" type="button" onClick={() => setForm({ ...form, questions: [...form.questions, newQuestion()] })}>Thêm câu hỏi</button>}<button className="button button--primary" disabled={state.saving}>{state.saving ? 'Đang lưu…' : 'Lưu quiz'}</button></form></section>
}

export default QuizEditorPage
