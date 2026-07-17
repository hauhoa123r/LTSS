import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../features/auth/context/AuthContext.jsx'

function navLinkClassName({ isActive }) {
  return isActive ? 'site-nav__link site-nav__link--active' : 'site-nav__link'
}

function MainLayout() {
  const { user, isReady, logout } = useAuth()

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Chuyển đến nội dung chính
      </a>

      <header className="site-header">
        <div className="site-header__inner page-container">
          <NavLink className="brand" to="/" aria-label="LTSS - Trang chủ">
            <span className="brand__mark" aria-hidden="true">
              LT
            </span>
            <span>
              <strong>LTSS</strong>
              <small>Local Tourism Support System</small>
            </span>
          </NavLink>

          <nav className="site-nav" aria-label="Điều hướng chính">
            <NavLink className={navLinkClassName} to="/" end>
              Trang chủ
            </NavLink>
            <NavLink className={navLinkClassName} to="/system-status">
              Trạng thái hệ thống
            </NavLink>
            <NavLink className={navLinkClassName} to="/places">
              Khám phá
            </NavLink>
            <NavLink className={navLinkClassName} to="/businesses">
              Doanh nghiệp
            </NavLink>
            <NavLink className={navLinkClassName} to="/articles">
              Bài viết
            </NavLink>
            <NavLink className={navLinkClassName} to="/events">
              Sự kiện
            </NavLink>
            <NavLink className={navLinkClassName} to="/tours">
              Lịch trình
            </NavLink>
            <NavLink className={navLinkClassName} to="/quizzes">
              Quiz
            </NavLink>
            {isReady && user ? (
              <>
                <NavLink className={navLinkClassName} to="/profile">
                  {user.displayName}
                </NavLink>
                <NavLink className={navLinkClassName} to="/favorites">
                  Yêu thích
                </NavLink>
                <NavLink className={navLinkClassName} to="/notifications">
                  Thông báo
                </NavLink>
                <NavLink className={navLinkClassName} to="/my-tours">
                  Tour của tôi
                </NavLink>
                <NavLink className={navLinkClassName} to="/quiz-progress">
                  Thành tích
                </NavLink>
                {user.roles.includes('RELIC_MANAGER') && (
                  <NavLink className={navLinkClassName} to="/manage/quizzes">
                    Quản lý quiz
                  </NavLink>
                )}
                {user.roles.includes('BUSINESS_OWNER') && (
                  <NavLink className={navLinkClassName} to="/business-analytics">
                    Phân tích kinh doanh
                  </NavLink>
                )}
                {user.roles.includes('ADMINISTRATOR') && (
                  <>
                    <NavLink className={navLinkClassName} to="/admin/dashboard">
                      Dashboard
                    </NavLink>
                    <NavLink className={navLinkClassName} to="/admin/users">
                      Người dùng
                    </NavLink>
                    <NavLink className={navLinkClassName} to="/admin/audit-logs">
                      Audit
                    </NavLink>
                  </>
                )}
                {user.roles.some((role) => role === 'MODERATOR' || role === 'ADMINISTRATOR') && (
                  <NavLink className={navLinkClassName} to="/moderation">
                    Kiểm duyệt
                  </NavLink>
                )}
                <button className="site-nav__button" type="button" onClick={logout}>
                  Đăng xuất
                </button>
              </>
            ) : isReady ? (
              <>
                <NavLink className={navLinkClassName} to="/login">
                  Đăng nhập
                </NavLink>
                <NavLink className="button button--primary site-nav__register" to="/register">
                  Đăng ký
                </NavLink>
              </>
            ) : null}
          </nav>
        </div>
      </header>

      <main id="main-content" className="site-main page-container">
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="page-container">
          <p>LTSS foundation · Sơn Tây, Hà Nội</p>
        </div>
      </footer>
    </div>
  )
}

export default MainLayout
