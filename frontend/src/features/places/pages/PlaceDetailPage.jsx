import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { placeApi } from '../api/placeApi.js'
import FavoriteButton from '../components/FavoriteButton.jsx'
import MediaGallery from '../components/MediaGallery.jsx'
import { ReviewSection } from '../../community/index.js'
import { useTrackView } from '../../analytics/hooks/useTrackView.js'

function formatFee(value) {
  const amount = Number(value)
  if (!amount) return 'Miễn phí'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount)
}

function PlaceDetailPage() {
  const { slug } = useParams()
  const [state, setState] = useState({ place: null, loading: true, error: null })
  useTrackView('PLACE', state.place?.id)

  useEffect(() => {
    let active = true
    setState({ place: null, loading: true, error: null })
    placeApi.detail(slug)
      .then((place) => active && setState({ place, loading: false, error: null }))
      .catch((error) => active && setState({ place: null, loading: false, error }))
    return () => { active = false }
  }, [slug])

  if (state.loading) return <p className="form-status">Đang tải địa điểm…</p>
  if (state.error) return <section className="discovery-error"><FormMessage error={state.error} /><Link to="/places">Quay lại khám phá</Link></section>

  const place = state.place
  return (
    <article className="place-detail">
      <div className="place-detail__breadcrumb"><Link to="/places">Khám phá</Link><span>/</span><span>{place.category.name}</span></div>
      <header className="place-detail__header">
        <div><p className="eyebrow">{place.category.name}</p><h1>{place.name}</h1><p>{place.summary}</p></div>
        <FavoriteButton placeId={place.id} favorite={place.favorite} onChange={(favorite) => setState({ ...state, place: { ...place, favorite } })} />
      </header>
      <MediaGallery media={place.media} />
      <div className="place-detail__grid">
        <div className="place-detail__content">
          <section><h2>Giới thiệu</h2><p className="preserve-lines">{place.description || 'Nội dung đang được cập nhật.'}</p></section>
          {place.relicDetail && <><section><h2>Lịch sử</h2><p className="preserve-lines">{place.relicDetail.history || place.relicDetail.historicalPeriod}</p></section><section><h2>Kiến trúc</h2><p className="preserve-lines">{place.relicDetail.architecture || 'Thông tin đang được cập nhật.'}</p></section></>}
        </div>
        <aside className="place-facts">
          <h2>Thông tin tham quan</h2>
          <dl>
            <div><dt>Địa chỉ</dt><dd>{place.address || 'Đang cập nhật'}</dd></div>
            <div><dt>Giờ mở cửa</dt><dd>{place.openingHours || 'Liên hệ địa điểm'}</dd></div>
            <div><dt>Vé vào cửa</dt><dd>{formatFee(place.entranceFee)}</dd></div>
            {place.contactPhone && <div><dt>Điện thoại</dt><dd><a href={`tel:${place.contactPhone}`}>{place.contactPhone}</a></dd></div>}
            {place.latitude != null && <div><dt>Tọa độ</dt><dd>{Number(place.latitude).toFixed(5)}, {Number(place.longitude).toFixed(5)}</dd></div>}
            {place.relicDetail?.recognitionLevel && <div><dt>Xếp hạng</dt><dd>{place.relicDetail.recognitionLevel}</dd></div>}
          </dl>
        </aside>
      </div>
      <div className="quiz-result-actions">
        <Link className="button button--secondary" to={`/quizzes?placeId=${place.id}`}>Quiz tại địa điểm này</Link>
      </div>
      <ReviewSection targetType="PLACE" targetId={place.id} />
    </article>
  )
}

export default PlaceDetailPage
