import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { ReviewSection } from '../../community/index.js'
import { tourApi } from '../api/tourApi.js'
import { useTrackView } from '../../analytics/hooks/useTrackView.js'

function TourDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState({ tour: null, loading: true, error: null, actionError: null })
  useTrackView('TOUR', state.tour?.id)
  useEffect(() => { let active = true; tourApi.detail(id).then((tour) => active && setState({ tour, loading: false, error: null, actionError: null })).catch((error) => active && setState({ tour: null, loading: false, error, actionError: null })); return () => { active = false } }, [id])
  async function copy() { try { const tour = await tourApi.copy(id); navigate(`/my-tours/${tour.id}/edit`) } catch (error) { setState({ ...state, actionError: error }) } }
  async function visibility(value) { try { const tour = await tourApi.visibility(id, { visibility: value, version: state.tour.version }); setState({ ...state, tour, actionError: null }) } catch (error) { setState({ ...state, actionError: error }) } }
  if (state.loading) return <p className="form-status">Đang tải lịch trình…</p>
  if (state.error) return <div className="discovery-error"><FormMessage error={state.error} /><Link to="/tours">Quay lại</Link></div>
  const tour = state.tour
  return <article className="tour-detail"><div className="content-detail__breadcrumb"><Link to="/tours">Lịch trình</Link><span>/</span><span>{tour.region || 'Sơn Tây'}</span></div><header className="content-detail__header"><p className="eyebrow">{tour.items.length} điểm đến · {tour.ownerDisplayName}</p><h1>{tour.title}</h1><p>{tour.description}</p><div className="hero__actions">{user && !tour.ownedByCurrentUser && <button className="button button--primary" onClick={copy}>Sao chép lịch trình</button>}{tour.ownedByCurrentUser && <Link className="button button--primary" to={`/my-tours/${tour.id}/edit`}>Chỉnh sửa</Link>}{tour.ownedByCurrentUser && tour.status === 'PUBLISHED' && <select className="tour-visibility" value={tour.visibility} onChange={(event) => visibility(event.target.value)}><option value="PRIVATE">Riêng tư</option><option value="UNLISTED">Có liên kết</option><option value="PUBLIC">Công khai</option></select>}</div><FormMessage error={state.actionError} /></header><ol className="tour-stops">{tour.items.map((item) => <li key={item.id}><span>{item.visitOrder}</span><div><h2>{item.placeSlug ? <Link to={`/places/${item.placeSlug}`}>{item.placeName}</Link> : item.placeName}</h2><p>{item.note || item.address}</p><small>{item.durationMinutes ? `${item.durationMinutes} phút` : 'Thời gian linh hoạt'}{item.transportMethod ? ` · ${item.transportMethod}` : ''}</small></div></li>)}</ol>{tour.status === 'PUBLISHED' && <ReviewSection targetType="TOUR" targetId={tour.id} />}</article>
}

export default TourDetailPage
