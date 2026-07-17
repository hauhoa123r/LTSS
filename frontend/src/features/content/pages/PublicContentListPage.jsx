import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { contentApi } from '../api/contentApi.js'
import ContentCard from '../components/ContentCard.jsx'

const SETTINGS = {
  businesses: {
    eyebrow: 'Dịch vụ bản địa',
    title: 'Doanh nghiệp đồng hành cùng chuyến đi.',
    description: 'Khám phá các đơn vị đã được xác thực và gắn với địa điểm đang hoạt động.',
    empty: 'Chưa có doanh nghiệp phù hợp.',
    type: 'business',
    load: contentApi.businesses,
  },
  articles: {
    eyebrow: 'Cẩm nang Sơn Tây',
    title: 'Câu chuyện, kinh nghiệm và góc nhìn địa phương.',
    description: 'Nội dung biên tập đã xuất bản để bạn chuẩn bị hành trình tốt hơn.',
    empty: 'Chưa có bài viết phù hợp.',
    type: 'article',
    load: contentApi.articles,
  },
  events: {
    eyebrow: 'Lịch địa phương',
    title: 'Sự kiện sắp diễn ra tại Sơn Tây.',
    description: 'Theo dõi thời gian, địa điểm và thông tin cần biết trước khi tham gia.',
    empty: 'Chưa có sự kiện sắp tới.',
    type: 'event',
    load: contentApi.events,
  },
}

function PublicContentListPage({ mode }) {
  const setting = SETTINGS[mode]
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [categories, setCategories] = useState([])
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const rawPage = Number(searchParams.get('page') || 0)
  const page = Number.isInteger(rawPage) && rawPage >= 0 ? rawPage : 0
  const q = searchParams.get('q') || undefined
  const category = searchParams.get('category') || undefined

  useEffect(() => {
    if (mode !== 'articles') return
    contentApi.articleCategories().then(setCategories).catch(() => setCategories([]))
  }, [mode])

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    setting.load({ q, category, page, size: 12 })
      .then((data) => active && setState({ data, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [setting, q, category, page])

  function submit(event) {
    event.preventDefault()
    const next = {}
    if (query.trim()) next.q = query.trim()
    if (category) next.category = category
    setSearchParams(next)
  }

  function changeCategory(event) {
    const next = new URLSearchParams(searchParams)
    if (event.target.value) next.set('category', event.target.value)
    else next.delete('category')
    next.delete('page')
    setSearchParams(next)
  }

  function changePage(nextPage) {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(nextPage))
    setSearchParams(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <section className="content-list-page" aria-labelledby={`${mode}-title`}>
      <header className="content-hero">
        <p className="eyebrow">{setting.eyebrow}</p>
        <h1 id={`${mode}-title`}>{setting.title}</h1>
        <p>{setting.description}</p>
        <form className="content-search" onSubmit={submit}>
          <input value={query} maxLength="255" aria-label="Từ khóa" placeholder="Nhập từ khóa…" onChange={(event) => setQuery(event.target.value)} />
          {mode === 'articles' && (
            <select aria-label="Danh mục bài viết" value={category || ''} onChange={changeCategory}>
              <option value="">Tất cả danh mục</option>
              {categories.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}
            </select>
          )}
          <button className="button button--primary">Tìm kiếm</button>
        </form>
      </header>

      {state.loading && <p className="form-status">Đang tải nội dung…</p>}
      {!state.loading && state.error && <div className="discovery-error"><FormMessage error={state.error} /></div>}
      {!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>{setting.empty}</h2><p>Hãy thử một từ khóa khác hoặc quay lại sau.</p></div>}
      {!state.loading && state.data?.content.length > 0 && <div className="content-grid">{state.data.content.map((item) => <ContentCard key={item.id} item={item} type={setting.type} />)}</div>}
      {state.data?.totalPages > 1 && (
        <nav className="pagination" aria-label="Phân trang nội dung">
          <button disabled={state.data.first} onClick={() => changePage(state.data.page - 1)}>Trang trước</button>
          <span>Trang {state.data.page + 1} / {state.data.totalPages}</span>
          <button disabled={state.data.last} onClick={() => changePage(state.data.page + 1)}>Trang sau</button>
        </nav>
      )}
    </section>
  )
}

export default PublicContentListPage
