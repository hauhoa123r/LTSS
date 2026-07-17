import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { tourApi } from '../api/tourApi.js'

function PublicToursPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const q = searchParams.get('q') || undefined
  const page = Math.max(0, Number(searchParams.get('page') || 0))

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    tourApi.publicTours({ q, page, size: 12 }).then((data) => active && setState({ data, loading: false, error: null })).catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [q, page])

  function submit(event) { event.preventDefault(); setSearchParams(query.trim() ? { q: query.trim() } : {}) }
  function changePage(value) { const next = new URLSearchParams(searchParams); next.set('page', value); setSearchParams(next) }

  return <section className="tour-page"><header className="content-hero"><p className="eyebrow">Lịch trình cộng đồng</p><h1>Khám phá Sơn Tây qua hành trình được chia sẻ.</h1><p>Sao chép một lịch trình phù hợp rồi tùy chỉnh thành chuyến đi của riêng bạn.</p><form className="content-search" onSubmit={submit}><input value={query} maxLength="200" placeholder="Tìm tên lịch trình…" onChange={(event) => setQuery(event.target.value)} /><button className="button button--primary">Tìm kiếm</button></form></header>{state.loading && <p className="form-status">Đang tải lịch trình…</p>}{state.error && <FormMessage error={state.error} />}{!state.loading && !state.error && !state.data?.content.length && <div className="discovery-empty"><h2>Chưa có lịch trình công khai</h2><p>Các tour đã được xuất bản sẽ xuất hiện tại đây.</p></div>}{state.data?.content.length > 0 && <div className="tour-grid">{state.data.content.map((tour) => <article className="tour-card" key={tour.id}><p className="content-card__label">{tour.region || 'Sơn Tây'} · {tour.stopCount} điểm</p><h2><Link to={`/tours/${tour.id}`}>{tour.title}</Link></h2><p>{tour.description || 'Lịch trình du lịch được chia sẻ bởi cộng đồng LTSS.'}</p><dl><div><dt>Thời lượng</dt><dd>{tour.estimatedDurationMinutes ? `${tour.estimatedDurationMinutes} phút` : 'Linh hoạt'}</dd></div><div><dt>Độ khó</dt><dd>{tour.difficultyLevel || 'Phù hợp mọi người'}</dd></div></dl></article>)}</div>}{state.data?.totalPages > 1 && <nav className="pagination"><button disabled={state.data.first} onClick={() => changePage(page - 1)}>Trang trước</button><span>Trang {page + 1} / {state.data.totalPages}</span><button disabled={state.data.last} onClick={() => changePage(page + 1)}>Trang sau</button></nav>}</section>
}

export default PublicToursPage
