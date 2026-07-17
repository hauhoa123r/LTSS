import { Link } from 'react-router-dom'

export function formatDate(value, includeTime = false) {
  if (!value) return 'Đang cập nhật'
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    ...(includeTime ? { timeStyle: 'short' } : {}),
  }).format(new Date(value))
}

function ContentCard({ item, type }) {
  const config = {
    business: {
      to: `/businesses/${item.id}`,
      title: item.place.name,
      label: 'Doanh nghiệp địa phương',
      summary: item.place.summary || item.place.address,
      cover: item.coverUrl,
      meta: item.websiteUrl ? 'Có website chính thức' : item.contactEmail,
    },
    article: {
      to: `/articles/${item.slug}`,
      title: item.title,
      label: item.category?.name || 'Bài viết',
      summary: item.summary,
      cover: item.coverUrl,
      meta: formatDate(item.publishedAt),
    },
    event: {
      to: `/events/${item.slug}`,
      title: item.title,
      label: 'Sự kiện sắp tới',
      summary: item.locationNote || item.place?.name,
      cover: item.coverUrl,
      meta: formatDate(item.startAt, true),
    },
    post: {
      to: `/business-posts/${item.slug}`,
      title: item.title,
      label: item.businessName || 'Tin doanh nghiệp',
      summary: item.summary,
      cover: item.coverUrl,
      meta: formatDate(item.publishedAt),
    },
    promotion: {
      to: `/promotions/${item.id}`,
      title: item.title,
      label: 'Ưu đãi đang diễn ra',
      summary: item.description,
      cover: item.coverUrl,
      meta: `Đến ${formatDate(item.endAt)}`,
    },
  }[type]

  return (
    <article className="content-card">
      <Link className="content-card__media" to={config.to}>
        {config.cover ? <img src={config.cover} alt="" loading="lazy" /> : <span aria-hidden="true">LT</span>}
      </Link>
      <div className="content-card__body">
        <p className="content-card__label">{config.label}</p>
        <h2><Link to={config.to}>{config.title}</Link></h2>
        <p className="content-card__summary">{config.summary || 'Nội dung đang được cập nhật.'}</p>
        {config.meta && <p className="content-card__meta">{config.meta}</p>}
      </div>
    </article>
  )
}

export default ContentCard
