import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { placeApi } from '../api/placeApi.js'

function FavoriteButton({ placeId, favorite, onChange, compact = false }) {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function toggle(event) {
    event.preventDefault()
    event.stopPropagation()
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }

    setLoading(true)
    setError('')
    try {
      const state = favorite ? await placeApi.unfavorite(placeId) : await placeApi.favorite(placeId)
      onChange?.(state.favorite)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <span className="favorite-control">
      <button
        className={`favorite-button ${favorite ? 'favorite-button--active' : ''}`}
        type="button"
        onClick={toggle}
        disabled={loading}
        aria-pressed={favorite}
        title={favorite ? 'Bỏ khỏi yêu thích' : 'Thêm vào yêu thích'}
      >
        <span aria-hidden="true">{favorite ? '♥' : '♡'}</span>
        {!compact && (loading ? 'Đang lưu…' : favorite ? 'Đã yêu thích' : 'Yêu thích')}
      </button>
      {error && <small className="favorite-control__error" role="alert">{error}</small>}
    </span>
  )
}

export default FavoriteButton
