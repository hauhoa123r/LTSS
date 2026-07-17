import { Link } from 'react-router-dom'

function AuthCard({ eyebrow = 'Tài khoản LTSS', title, description, children, footer }) {
  return (
    <section className="auth-page" aria-labelledby="auth-title">
      <div className="auth-card">
        <p className="eyebrow">{eyebrow}</p>
        <h1 id="auth-title">{title}</h1>
        {description && <p className="auth-card__description">{description}</p>}
        {children}
        {footer && <div className="auth-card__footer">{footer}</div>}
      </div>
      <Link className="auth-page__back" to="/">
        Quay về trang chủ
      </Link>
    </section>
  )
}

export default AuthCard
