import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { contentApi } from '../api/contentApi.js'
import ContentCard from '../components/ContentCard.jsx'
import { ReviewSection } from '../../community/index.js'
import { useTrackView } from '../../analytics/hooks/useTrackView.js'

function BusinessDetailPage() {
  const { id } = useParams()
  const [state, setState] = useState({ business: null, posts: [], promotions: [], loading: true, error: null })
  useTrackView('BUSINESS', state.business?.id)

  useEffect(() => {
    let active = true
    setState({ business: null, posts: [], promotions: [], loading: true, error: null })
    Promise.all([
      contentApi.business(id),
      contentApi.posts({ businessId: id, size: 6 }),
      contentApi.promotions({ businessId: id, size: 6 }),
    ])
      .then(([business, posts, promotions]) => active && setState({ business, posts: posts.content, promotions: promotions.content, loading: false, error: null }))
      .catch((error) => active && setState({ business: null, posts: [], promotions: [], loading: false, error }))
    return () => { active = false }
  }, [id])

  if (state.loading) return <p className="form-status">Đang tải doanh nghiệp…</p>
  if (state.error) return <section className="discovery-error"><FormMessage error={state.error} /><Link to="/businesses">Quay lại danh sách</Link></section>

  const { business } = state
  return (
    <article className="content-detail">
      <div className="content-detail__breadcrumb"><Link to="/businesses">Doanh nghiệp</Link><span>/</span><span>{business.place.name}</span></div>
      <header className="business-profile">
        {business.coverUrl && <img src={business.coverUrl} alt="" />}
        <div><p className="eyebrow">Đơn vị đã xác thực</p><h1>{business.place.name}</h1><p>{business.place.summary || business.place.address}</p></div>
      </header>
      <div className="content-detail__columns">
        <div className="content-detail__body">
          <section><h2>Tin từ doanh nghiệp</h2>{state.posts.length ? <div className="content-grid content-grid--two">{state.posts.map((item) => <ContentCard key={item.id} item={item} type="post" />)}</div> : <p className="content-muted">Chưa có bài đăng đã xuất bản.</p>}</section>
          <section><h2>Ưu đãi đang diễn ra</h2>{state.promotions.length ? <div className="content-grid content-grid--two">{state.promotions.map((item) => <ContentCard key={item.id} item={item} type="promotion" />)}</div> : <p className="content-muted">Hiện chưa có ưu đãi còn hiệu lực.</p>}</section>
        </div>
        <aside className="content-facts">
          <h2>Thông tin liên hệ</h2>
          <dl>
            <div><dt>Địa chỉ</dt><dd>{business.place.address || 'Đang cập nhật'}</dd></div>
            {business.contactEmail && <div><dt>Email</dt><dd><a href={`mailto:${business.contactEmail}`}>{business.contactEmail}</a></dd></div>}
            {business.websiteUrl && <div><dt>Website</dt><dd><a href={business.websiteUrl} target="_blank" rel="noreferrer">Mở website</a></dd></div>}
            <div><dt>Mã đăng ký</dt><dd>{business.registrationNumber}</dd></div>
          </dl>
          <Link className="button button--secondary" to={`/places/${business.place.slug}`}>Xem địa điểm</Link>
        </aside>
      </div>
      <ReviewSection targetType="BUSINESS" targetId={business.id} />
    </article>
  )
}

export default BusinessDetailPage
