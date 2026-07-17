import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { placeApi } from '../../places/api/placeApi.js'
import { contentApi } from '../api/contentApi.js'

const initialForm = { categoryId: '', placeId: '', title: '', summary: '', content: '', version: null }

function RelicArticleEditorPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [categories, setCategories] = useState([])
  const [places, setPlaces] = useState([])
  const [form, setForm] = useState(initialForm)
  const [state, setState] = useState({ loading: true, saving: false, error: null })

  useEffect(() => {
    let active = true
    const articleRequest = id ? contentApi.managedArticle(id) : Promise.resolve(null)
    Promise.all([contentApi.articleCategories(), placeApi.search({ page: 0, size: 50 }), articleRequest])
      .then(([categoryData, placeData, article]) => {
        if (!active) return
        setCategories(categoryData)
        setPlaces(placeData.content)
        if (article) setForm({ categoryId: article.categoryId, placeId: article.placeId, title: article.title, summary: article.summary || '', content: article.content, version: article.version })
        setState({ loading: false, saving: false, error: null })
      })
      .catch((error) => active && setState({ loading: false, saving: false, error }))
    return () => { active = false }
  }, [id])

  async function save(event) {
    event.preventDefault()
    setState((current) => ({ ...current, saving: true, error: null }))
    const payload = {
      categoryId: Number(form.categoryId),
      placeId: Number(form.placeId),
      title: form.title.trim(),
      summary: form.summary.trim() || null,
      content: form.content.trim(),
      version: form.version,
    }
    try {
      if (id) await contentApi.updateArticle(id, payload)
      else await contentApi.createArticle(payload)
      navigate('/relic-manager/articles')
    } catch (error) {
      setState((current) => ({ ...current, saving: false, error }))
    }
  }

  if (state.loading) return <p className="form-status">Đang tải trình biên soạn…</p>

  return <section className="relic-article-editor">
    <header className="page-heading page-heading--actions"><div><p className="eyebrow">Bài viết quảng bá</p><h1>{id ? 'Chỉnh sửa bài viết' : 'Viết bài mới'}</h1><p>Nội dung phải gắn với một khu du lịch đã xuất bản và sẽ được Moderator kiểm duyệt.</p></div><Link className="button button--secondary" to="/relic-manager/articles">← Danh sách bài viết</Link></header>
    <form onSubmit={save}>
      <FormMessage error={state.error} />
      <section className="relic-article-editor__meta">
        <label>Khu du lịch <span>*</span><select required value={form.placeId} onChange={(event) => setForm({ ...form, placeId: event.target.value })}><option value="">Chọn khu du lịch</option>{places.map((place) => <option key={place.id} value={place.id}>{place.name}</option>)}</select></label>
        <label>Chuyên mục <span>*</span><select required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Chọn chuyên mục</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
      </section>
      <label>Tiêu đề bài viết <span>*</span><input required maxLength="250" value={form.title} placeholder="Nhập tiêu đề giới thiệu khu du lịch" onChange={(event) => setForm({ ...form, title: event.target.value })} /><small>{form.title.length}/250</small></label>
      <label>Tóm tắt<input maxLength="700" value={form.summary} placeholder="Tóm tắt ngắn nội dung nổi bật" onChange={(event) => setForm({ ...form, summary: event.target.value })} /><small>{form.summary.length}/700</small></label>
      <label>Nội dung bài viết <span>*</span><textarea required maxLength="50000" rows="16" value={form.content} placeholder="Giới thiệu lịch sử, văn hóa, trải nghiệm và lưu ý dành cho du khách…" onChange={(event) => setForm({ ...form, content: event.target.value })} /><small>{form.content.length}/50000</small></label>
      <footer><Link className="button button--secondary" to="/relic-manager/articles">Hủy</Link><button className="button button--primary" disabled={state.saving}>{state.saving ? 'Đang lưu…' : id ? 'Lưu thay đổi' : 'Lưu bản nháp'}</button></footer>
    </form>
  </section>
}

export default RelicArticleEditorPage
