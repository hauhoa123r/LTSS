import { Link } from 'react-router-dom'
import FavoriteButton from './FavoriteButton.jsx'
import bepLangDuongLamImage from '../../../assets/places/demo-bep-lang-duong-lam.jpg'
import chuaMiaImage from '../../../assets/places/demo-chua-mia.jpg'
import thanhCoSonTayImage from '../../../assets/places/demo-thanh-co-son-tay.jpg'

const DEMO_PLACE_IMAGES = {
  'demo-bep-lang-duong-lam': bepLangDuongLamImage,
  'demo-chua-mia': chuaMiaImage,
  'demo-thanh-co-son-tay': thanhCoSonTayImage,
}

function formatFee(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount) || amount === 0) return 'Miễn phí'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount)
}

function PlaceCard({ place, onFavoriteChange }) {
  const coverUrl = DEMO_PLACE_IMAGES[place.slug] || place.coverUrl

  return (
    <article className="place-card">
      <Link className="place-card__media" to={`/places/${place.slug}`}>
        {coverUrl ? (
          <img src={coverUrl} alt={place.name} loading="lazy" />
        ) : (
          <span className="place-card__placeholder" aria-hidden="true">LT</span>
        )}
        <span className="place-card__category">{place.category?.name || 'Địa điểm'}</span>
      </Link>
      <div className="place-card__body">
        <div className="place-card__title-row">
          <h2><Link to={`/places/${place.slug}`}>{place.name}</Link></h2>
          <FavoriteButton
            compact
            placeId={place.id}
            favorite={place.favorite}
            onChange={(favorite) => onFavoriteChange?.(place.id, favorite)}
          />
        </div>
        <p>{place.summary || place.address || 'Thông tin địa điểm đang được cập nhật.'}</p>
        <dl className="place-card__meta">
          <div><dt>Vé</dt><dd>{formatFee(place.entranceFee)}</dd></div>
          {place.distanceKm != null && <div><dt>Khoảng cách</dt><dd>{place.distanceKm.toFixed(2)} km</dd></div>}
        </dl>
      </div>
    </article>
  )
}

export default PlaceCard
