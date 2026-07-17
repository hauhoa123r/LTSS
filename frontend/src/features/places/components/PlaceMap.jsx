import { Link } from 'react-router-dom'

function PlaceMap({ places }) {
  const located = places.filter((place) => place.latitude != null && place.longitude != null)
  if (!located.length) return <div className="place-map place-map--empty">Chưa có tọa độ để hiển thị bản đồ.</div>

  const latitudes = located.map((place) => Number(place.latitude))
  const longitudes = located.map((place) => Number(place.longitude))
  const minLat = Math.min(...latitudes)
  const maxLat = Math.max(...latitudes)
  const minLng = Math.min(...longitudes)
  const maxLng = Math.max(...longitudes)
  const latSpan = Math.max(maxLat - minLat, 0.01)
  const lngSpan = Math.max(maxLng - minLng, 0.01)

  return (
    <div className="place-map" role="img" aria-label={`Bản đồ tọa độ của ${located.length} địa điểm`}>
      <div className="place-map__grid" aria-hidden="true" />
      {located.map((place, index) => {
        const left = minLng === maxLng ? 50 : 8 + ((Number(place.longitude) - minLng) / lngSpan) * 84
        const top = minLat === maxLat ? 50 : 8 + ((maxLat - Number(place.latitude)) / latSpan) * 84
        return (
          <Link
            key={place.id}
            className="place-map__marker"
            to={`/places/${place.slug}`}
            style={{ left: `${left}%`, top: `${top}%` }}
            title={place.name}
          >
            <span>{index + 1}</span>
          </Link>
        )
      })}
      <span className="place-map__north" aria-hidden="true">Bắc ↑</span>
    </div>
  )
}

export default PlaceMap
