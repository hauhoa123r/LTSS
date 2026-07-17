import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { contentApi } from '../api/contentApi.js'
import ContentMedia from '../components/ContentMedia.jsx'
import { formatDate } from '../components/ContentCard.jsx'
import ReviewSection from '../../community/components/ReviewSection.jsx'
import { useTrackView } from '../../analytics/hooks/useTrackView.js'

const SETTINGS = {
  article: { load: contentApi.article, back: '/articles', backLabel: 'Bài viết' },
  event: { load: contentApi.event, back: '/events', backLabel: 'Sự kiện' },
  post: { load: contentApi.post, back: '/businesses', backLabel: 'Doanh nghiệp' },
  promotion: { load: contentApi.promotion, back: '/businesses', backLabel: 'Doanh nghiệp' },
}

function discountLabel(item) {
  if (item.discountValue == null) return null
  if (item.discountType === 'PERCENTAGE') return `${Number(item.discountValue).toLocaleString('vi-VN')}%`
  if (item.discountType === 'FIXED_AMOUNT') return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(item.discountValue)
  return String(item.discountValue)
}

function ContentDetailPage({ type }) {
  const params = useParams()
  const key = type === 'promotion' ? params.id : params.slug
  const setting = SETTINGS[type]
  const [state, setState] = useState({ item: null, loading: true, error: null })
  const targetType = { article: 'ARTICLE', event: 'EVENT', post: 'BUSINESS_POST', promotion: 'PROMOTION' }[type]
  useTrackView(targetType, state.item?.id)

  useEffect(() => {
    let active = true
    setState({ item: null, loading: true, error: null })
    setting.load(key)
      .then((item) => active && setState({ item, loading: false, error: null }))
      .catch((error) => active && setState({ item: null, loading: false, error }))
    return () => { active = false }
  }, [setting, key])

  if (state.loading) return <p className="form-status">Đang tải nội dung…</p>
  if (state.error) return <section className="discovery-error"><FormMessage error={state.error} /><Link to={setting.back}>Quay lại</Link></section>

  const item = state.item
  const place = type === 'post' ? item.business?.place : item.place
  const eyebrow = type === 'article' ? item.category?.name : type === 'event' ? 'Sự kiện' : type === 'post' ? item.business?.place?.name : 'Ưu đãi đang diễn ra'
  const body = type === 'article' || type === 'post' ? item.content : item.description

  return (
    <article className="content-detail">
      <div className="content-detail__breadcrumb"><Link to={setting.back}>{setting.backLabel}</Link><span>/</span><span>{eyebrow}</span></div>
      <header className="content-detail__header">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{item.title}</h1>
        {item.summary && <p>{item.summary}</p>}
        {type === 'promotion' && discountLabel(item) && <strong className="promotion-value">{discountLabel(item)}</strong>}
      </header>
      <ContentMedia media={item.media} />
      <div className="content-detail__columns">
        <div className="content-detail__body">
          {body && <section><p className="preserve-lines">{body}</p></section>}
          {type === 'event' && <section><h2>Thời gian và địa điểm</h2><p>Từ {formatDate(item.startAt, true)} đến {formatDate(item.endAt, true)}</p><p>{item.locationNote || place?.name || 'Địa điểm đang được cập nhật.'}</p></section>}
          {type === 'promotion' && <section><h2>Thời hạn áp dụng</h2><p>Từ {formatDate(item.startAt, true)} đến {formatDate(item.endAt, true)}</p>{item.promoCode && <p>Mã ưu đãi: <strong className="promo-code">{item.promoCode}</strong></p>}</section>}
          {type === 'post' && item.tags?.length > 0 && <div className="content-tags">{item.tags.map((tag) => <span key={tag.slug}>#{tag.name}</span>)}</div>}
        </div>
        <aside className="content-facts">
          <h2>Thông tin liên quan</h2>
          <dl>
            {type === 'article' && <div><dt>Xuất bản</dt><dd>{formatDate(item.publishedAt)}</dd></div>}
            {type === 'post' && <div><dt>Xuất bản</dt><dd>{formatDate(item.publishedAt)}</dd></div>}
            {type === 'promotion' && <div><dt>Đơn vị</dt><dd>{item.businessName}</dd></div>}
            {place && <div><dt>Địa điểm</dt><dd>{place.name}</dd></div>}
          </dl>
          {place && <Link className="button button--secondary" to={`/places/${place.slug}`}>Xem địa điểm</Link>}
          {type === 'post' && item.business && <Link className="button button--secondary" to={`/businesses/${item.business.id}`}>Xem doanh nghiệp</Link>}
          {type === 'promotion' && <Link className="button button--secondary" to={`/businesses/${item.businessId}`}>Xem doanh nghiệp</Link>}
        </aside>
      </div>
      {type === 'article' && <ReviewSection targetType="ARTICLE" targetId={item.id} />}
    </article>
  )
}

export default ContentDetailPage
