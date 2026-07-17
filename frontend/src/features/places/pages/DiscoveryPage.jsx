import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { placeApi } from '../api/placeApi.js'
import PlaceCard from '../components/PlaceCard.jsx'
import PlaceMap from '../components/PlaceMap.jsx'
import { usePlaceSearch } from '../hooks/usePlaceSearch.js'

function DiscoveryPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [form, setForm] = useState({ q: searchParams.get('q') || '', category: searchParams.get('category') || '' })
  const [categories, setCategories] = useState([])
  const [history, setHistory] = useState([])
  const [nearby, setNearby] = useState(null)
  const [locationError, setLocationError] = useState('')
  const [view, setView] = useState('grid')

  const rawPage = Number(searchParams.get('page') || 0)
  const page = Number.isInteger(rawPage) && rawPage >= 0 ? rawPage : 0
  const request = useMemo(() => {
    if (nearby) {
      return { mode: 'nearby', query: { ...nearby, category: form.category || undefined, page, size: 12 } }
    }
    return {
      mode: 'search',
      query: {
        q: searchParams.get('q') || undefined,
        category: searchParams.get('category') || undefined,
        page,
        size: 12,
      },
    }
  }, [nearby, form.category, searchParams, page])
  const { data, loading, error, reload, setData } = usePlaceSearch(request)

  useEffect(() => {
    placeApi.categories().then(setCategories).catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    if (!user) {
      setHistory([])
      return
    }
    placeApi.searchHistory().then(setHistory).catch(() => setHistory([]))
  }, [user, data])

  function submitSearch(event) {
    event.preventDefault()
    setNearby(null)
    const next = {}
    if (form.q.trim()) next.q = form.q.trim()
    if (form.category) next.category = form.category
    setSearchParams(next)
  }

  function searchHistory(keyword) {
    setForm((current) => ({ ...current, q: keyword }))
    setNearby(null)
    setSearchParams({ q: keyword, ...(form.category ? { category: form.category } : {}) })
  }

  function findNearby() {
    setLocationError('')
    if (!navigator.geolocation) {
      setLocationError('Trình duyệt không hỗ trợ định vị.')
      return
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setNearby({ latitude: coords.latitude, longitude: coords.longitude, radiusKm: 5 })
        setSearchParams(form.category ? { category: form.category } : {})
      },
      () => setLocationError('Không thể lấy vị trí. Hãy kiểm tra quyền định vị của trình duyệt.'),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
    )
  }

  function changePage(nextPage) {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(nextPage))
    setSearchParams(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function updateFavorite(placeId, favorite) {
    if (!data) return
    setData({ ...data, content: data.content.map((place) => place.id === placeId ? { ...place, favorite } : place) })
  }

  async function clearHistory() {
    await placeApi.clearSearchHistory()
    setHistory([])
  }

  return (
    <section className="discovery-page" aria-labelledby="discovery-title">
      <header className="discovery-hero">
        <p className="eyebrow">Khám phá Sơn Tây</p>
        <h1 id="discovery-title">Tìm điểm đến cho hành trình tiếp theo.</h1>
        <form className="discovery-search" onSubmit={submitSearch}>
          <label className="discovery-search__query">
            <span className="sr-only">Tên hoặc từ khóa địa điểm</span>
            <input value={form.q} maxLength="255" placeholder="Tìm di tích, làng nghề, nhà hàng…" onChange={(event) => setForm({ ...form, q: event.target.value })} />
          </label>
          <label>
            <span className="sr-only">Danh mục</span>
            <select value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => <option key={category.id} value={category.slug}>{category.name}</option>)}
            </select>
          </label>
          <button className="button button--primary">Tìm kiếm</button>
          <button className="button button--secondary" type="button" onClick={findNearby}>Gần tôi</button>
        </form>
        {locationError && <p className="location-error" role="alert">{locationError}</p>}
        {history.length > 0 && (
          <div className="search-history">
            <span>Tìm gần đây:</span>
            {history.map((item) => <button key={item.id} type="button" onClick={() => searchHistory(item.keyword)}>{item.keyword}</button>)}
            <button className="search-history__clear" type="button" onClick={clearHistory}>Xóa</button>
          </div>
        )}
      </header>

      <div className="discovery-toolbar">
        <p>{nearby ? 'Địa điểm trong bán kính 5 km' : data ? `${data.totalElements} địa điểm` : 'Địa điểm'}</p>
        <div><button type="button" className={view === 'grid' ? 'is-active' : ''} onClick={() => setView('grid')}>Danh sách</button><button type="button" className={view === 'map' ? 'is-active' : ''} onClick={() => setView('map')}>Bản đồ</button></div>
      </div>

      {loading && <div className="place-grid" aria-label="Đang tải"><div className="place-card place-card--skeleton" /><div className="place-card place-card--skeleton" /><div className="place-card place-card--skeleton" /></div>}
      {!loading && error && <div className="discovery-error"><FormMessage error={error} /><button className="button button--secondary" onClick={reload}>Thử lại</button></div>}
      {!loading && !error && data?.content.length === 0 && <div className="discovery-empty"><h2>Chưa tìm thấy địa điểm</h2><p>Thử đổi từ khóa, danh mục hoặc phạm vi tìm kiếm.</p></div>}
      {!loading && !error && data?.content.length > 0 && (
        view === 'map'
          ? <PlaceMap places={data.content} />
          : <div className="place-grid">{data.content.map((place) => <PlaceCard key={place.id} place={place} onFavoriteChange={updateFavorite} />)}</div>
      )}
      {data && data.totalPages > 1 && (
        <nav className="pagination" aria-label="Phân trang địa điểm">
          <button disabled={data.first} onClick={() => changePage(data.page - 1)}>Trang trước</button>
          <span>Trang {data.page + 1} / {data.totalPages}</span>
          <button disabled={data.last} onClick={() => changePage(data.page + 1)}>Trang sau</button>
        </nav>
      )}
    </section>
  )
}

export default DiscoveryPage
