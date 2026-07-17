import { Link } from 'react-router-dom'

function HomePage() {
  return (
    <section className="hero" aria-labelledby="home-title">
      <div className="hero__content">
        <p className="eyebrow">Local Tourism Support System</p>
        <h1 id="home-title">Khám phá Sơn Tây theo cách của bạn.</h1>
        <p className="hero__summary">
          Khám phá địa điểm, doanh nghiệp địa phương, ưu đãi, bài viết và sự kiện
          đã được LTSS kiểm duyệt cho hành trình của bạn.
        </p>
        <div className="hero__actions">
          <Link className="button button--primary" to="/places">
            Khám phá địa điểm
          </Link>
          <Link className="button button--secondary" to="/quizzes">
            Trải nghiệm quiz
          </Link>
        </div>
      </div>

      <aside className="foundation-card" aria-label="Phạm vi foundation">
        <p className="foundation-card__label">Phase 8</p>
        <h2>Analytics &amp; Administration</h2>
        <ul>
          <li>Tương tác append-only và dedup lượt xem</li>
          <li>Báo cáo riêng theo phạm vi doanh nghiệp</li>
          <li>Dashboard quản trị theo khoảng ngày</li>
          <li>Quản lý tài khoản, vai trò và audit trail</li>
        </ul>
      </aside>
    </section>
  )
}

export default HomePage
