import { useEffect, useState } from 'react'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { placeApi } from '../api/placeApi.js'
import PlaceCard from '../components/PlaceCard.jsx'

function FavoritesPage() {
  const [state, setState] = useState({ data: null, loading: true, error: null })

  useEffect(() => {
    placeApi.favorites({ page: 0, size: 50 })
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((error) => setState({ data: null, loading: false, error }))
  }, [])

  function favoriteChanged(placeId, favorite) {
    if (!favorite) {
      setState((current) => ({ ...current, data: { ...current.data, content: current.data.content.filter((place) => place.id !== placeId), totalElements: current.data.totalElements - 1 } }))
    }
  }

  return (
    <section className="favorites-page">
      <header className="page-heading"><p className="eyebrow">Bộ sưu tập</p><h1>Địa điểm yêu thích</h1><p>Lưu những điểm đến bạn muốn ghé thăm trong hành trình.</p></header>
      {state.loading && <p className="form-status">Đang tải danh sách…</p>}
      {state.error && <FormMessage error={state.error} />}
      {!state.loading && !state.error && state.data.content.length === 0 && <div className="discovery-empty"><h2>Chưa có địa điểm yêu thích</h2><p>Khám phá và nhấn biểu tượng trái tim để lưu địa điểm.</p></div>}
      {state.data?.content.length > 0 && <div className="place-grid">{state.data.content.map((place) => <PlaceCard key={place.id} place={place} onFavoriteChange={favoriteChanged} />)}</div>}
    </section>
  )
}

export default FavoritesPage
