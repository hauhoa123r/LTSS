import { Link } from 'react-router-dom'

function AuthCard({ eyebrow = 'Tài khoản LTSS', title, description, children, footer }) {
  return (
    <section className="auth-page" aria-labelledby="auth-title">
      <aside className="auth-showcase" aria-label="Giới thiệu LTSS">
        <div className="auth-showcase__seal" aria-hidden="true"><span>LT</span><i>✦</i></div>
        <p className="section-eyebrow">Local Tourism Support System</p>
        <h2>Mỗi hành trình bắt đầu từ một câu chuyện.</h2>
        <p>Đăng nhập để lưu điểm đến, xây dựng lịch trình và tiếp tục khám phá Sơn Tây theo cách của bạn.</p>
        <div className="auth-showcase__benefits">
          <span><i>✓</i> Lưu địa điểm yêu thích</span>
          <span><i>✓</i> Tạo lịch trình cá nhân</span>
          <span><i>✓</i> Theo dõi thành tích trải nghiệm</span>
        </div>
        <div className="auth-showcase__landmark" aria-hidden="true"><span /><span /><span /></div>
      </aside>

      <div className="auth-card-column">
        <div className="auth-card">
          <p className="eyebrow">{eyebrow}</p>
          <h1 id="auth-title">{title}</h1>
          {description && <p className="auth-card__description">{description}</p>}
          {children}
          {footer && <div className="auth-card__footer">{footer}</div>}
        </div>
        <Link className="auth-page__back" to="/">
          <span aria-hidden="true">←</span> Quay về trang chủ
        </Link>
      </div>
    </section>
  )
}

export default AuthCard
