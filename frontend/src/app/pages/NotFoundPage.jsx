import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <section className="empty-page" aria-labelledby="not-found-title">
      <p className="eyebrow">404</p>
      <h1 id="not-found-title">Không tìm thấy trang</h1>
      <p>Đường dẫn này chưa tồn tại hoặc đã được thay đổi.</p>
      <Link className="button button--primary" to="/">
        Về trang chủ
      </Link>
    </section>
  )
}

export default NotFoundPage
