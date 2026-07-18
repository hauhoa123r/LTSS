import { useEffect, useRef, useState } from 'react'
import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom'

function navLinkClassName({ isActive }) {
  return isActive ? 'site-nav__link site-nav__link--active' : 'site-nav__link'
}

function navChildLinkClassName({ isActive }) {
  return isActive ? 'site-nav__link site-nav__link--child site-nav__link--active' : 'site-nav__link site-nav__link--child'
}

function drawerLinkClassName({ isActive }) {
  return isActive ? 'mobile-nav__link mobile-nav__link--active' : 'mobile-nav__link'
}

function workspaceLinkClassName({ isActive }) {
  return isActive ? 'workspace-nav__link workspace-nav__link--active' : 'workspace-nav__link'
}

function workspaceChildLinkClassName({ isActive }) {
  return isActive ? 'workspace-nav__link workspace-nav__link--child workspace-nav__link--active' : 'workspace-nav__link workspace-nav__link--child'
}

function MainLayout({
  user,
  isReady,
  logout,
  isAdministrator,
  isModerator,
  isRelicManager,
  isBusinessOwner,
  isStaff,
  isModerationPath,
  isRelicManagerPath,
  isBusinessOwnerPath,
  displayedRole,
  workspaceLinks,
  accountLinks,
  primaryLinks,
  mobileLinks,
  homePath,
  brandSubtitle,
  showWorkspace,
  redirectTo,
}) {
  const location = useLocation()
  const userMenuRef = useRef(null)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userOpen, setUserOpen] = useState(false)
  const [openGroups, setOpenGroups] = useState({})

  useEffect(() => {
    setMobileOpen(false)
    setUserOpen(false)
  }, [location.pathname])

  useEffect(() => {
    setOpenGroups((current) => {
      const next = { ...current }
      workspaceLinks.forEach((link) => {
        if (link.children?.some((child) => isPathActive(child, location.pathname))) {
          next[groupKey(link)] = true
        }
      })
      return next
    })
  }, [location.pathname, workspaceLinks])

  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    function onKeyDown(event) {
      if (event.key === 'Escape') {
        setMobileOpen(false)
        setUserOpen(false)
      }
    }
    function onPointerDown(event) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) setUserOpen(false)
    }
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('pointerdown', onPointerDown)
    return () => {
      document.body.style.overflow = ''
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('pointerdown', onPointerDown)
    }
  }, [mobileOpen])

  function handleLogout() {
    setUserOpen(false)
    setMobileOpen(false)
    logout()
  }

  function groupKey(link) {
    return link.id || link.label
  }

  function isPathActive(link, pathname) {
    if (!link.to) return false
    return link.end ? pathname === link.to : pathname === link.to || pathname.startsWith(`${link.to}/`)
  }

  function renderPrimaryItem(link) {
    if (!link.children?.length) {
      return (
        <NavLink key={link.to} className={navLinkClassName} to={link.to} end={link.end}>
          {link.label}
        </NavLink>
      )
    }

    const key = groupKey(link)
    const isOpen = Boolean(openGroups[key])
    const isActive = link.children.some((child) => isPathActive(child, location.pathname))

    return (
      <div key={key} className={`site-nav__group${isOpen ? ' is-open' : ''}${isActive ? ' is-active' : ''}`}>
        <button
          className={`site-nav__link site-nav__group-trigger${isActive ? ' site-nav__link--active' : ''}`}
          type="button"
          aria-expanded={isOpen}
          aria-controls={`site-nav-group-${key}`}
          onClick={() => setOpenGroups((current) => ({ ...current, [key]: !current[key] }))}
        >
          <span>{link.label}</span>
          <span aria-hidden="true">▾</span>
        </button>
        <div id={`site-nav-group-${key}`} className="site-nav__group-panel">
          {link.children.map((child) => (
            <NavLink key={child.to} className={navChildLinkClassName} to={child.to} end={child.end}>
              <span aria-hidden="true">{child.icon}</span>
              <span>{child.label}</span>
            </NavLink>
          ))}
        </div>
      </div>
    )
  }

  function renderWorkspaceItem(link) {
    if (!link.children?.length) {
      return (
        <NavLink key={link.to} className={workspaceLinkClassName} to={link.to} end={link.end}>
          <span aria-hidden="true">{link.icon}</span>
          <span className="workspace-nav__label">{link.label}</span>
        </NavLink>
      )
    }

    const key = groupKey(link)
    const isOpen = Boolean(openGroups[key])
    const isActive = link.children.some((child) => isPathActive(child, location.pathname))

    return (
      <div key={key} className={`workspace-nav__group${isOpen ? ' is-open' : ''}${isActive ? ' is-active' : ''}`}>
        <button
          className={`workspace-nav__link workspace-nav__group-trigger${isActive ? ' workspace-nav__link--active' : ''}`}
          type="button"
          aria-expanded={isOpen}
          aria-controls={`workspace-group-${key}`}
          onClick={() => setOpenGroups((current) => ({ ...current, [key]: !current[key] }))}
        >
          <span aria-hidden="true">{link.icon}</span>
          <span className="workspace-nav__label">{link.label}</span>
          <span className="workspace-nav__chevron" aria-hidden="true">▾</span>
        </button>
        <div id={`workspace-group-${key}`} className="workspace-nav__group-panel">
          {link.children.map((child) => (
            <NavLink key={child.to} className={workspaceChildLinkClassName} to={child.to} end={child.end}>
              <span aria-hidden="true">{child.icon}</span>
              <span className="workspace-nav__label">{child.label}</span>
            </NavLink>
          ))}
        </div>
      </div>
    )
  }

  function renderMobileItem(link) {
    if (!link.children?.length) {
      return <NavLink key={link.to} className={drawerLinkClassName} to={link.to} end={link.end}>{link.label}</NavLink>
    }

    const key = groupKey(link)
    const isOpen = Boolean(openGroups[key])
    const isActive = link.children.some((child) => isPathActive(child, location.pathname))

    return (
      <div key={key} className={`mobile-nav__group${isOpen ? ' is-open' : ''}${isActive ? ' is-active' : ''}`}>
        <button
          className={`mobile-nav__link mobile-nav__group-trigger${isActive ? ' mobile-nav__link--active' : ''}`}
          type="button"
          aria-expanded={isOpen}
          aria-controls={`mobile-group-${key}`}
          onClick={() => setOpenGroups((current) => ({ ...current, [key]: !current[key] }))}
        >
          <span>{link.label}</span>
          <span aria-hidden="true">▾</span>
        </button>
        <div id={`mobile-group-${key}`} className="mobile-nav__group-panel">
          {link.children.map((child) => (
            <NavLink key={child.to} className={({ isActive }) => `${drawerLinkClassName({ isActive })} mobile-nav__link--child`} to={child.to} end={child.end}>
              {child.label}
            </NavLink>
          ))}
        </div>
      </div>
    )
  }

  if (redirectTo) return <Navigate to={redirectTo} replace />

  return (
    <div className={`app-shell${showWorkspace ? ' app-shell--workspace' : ''}`}>
      <a className="skip-link" href="#main-content">
        Chuyển đến nội dung chính
      </a>

      <header className="site-header">
        <div className="site-header__inner page-container">
          <NavLink className="brand" to={homePath} aria-label={`LTSS - ${brandSubtitle}`}>
            <span className="brand__mark" aria-hidden="true">
              <span>LT</span>
              <i>✦</i>
            </span>
            <span className="brand__copy">
              <strong>LTSS</strong>
              <small>{brandSubtitle}</small>
            </span>
          </NavLink>

          <nav className="site-nav" aria-label="Điều hướng chính">
            {primaryLinks.map(renderPrimaryItem)}
          </nav>

          <div className="site-header__actions">
            {isReady && user ? (
              <div className="user-menu" ref={userMenuRef}>
                <button
                  className="user-menu__trigger"
                  type="button"
                  onClick={() => setUserOpen((current) => !current)}
                  aria-haspopup="menu"
                  aria-expanded={userOpen}
                >
                  <span className="user-menu__avatar" aria-hidden="true">
                    {user.displayName?.[0]?.toUpperCase() || 'U'}
                  </span>
                  <span className="user-menu__name">{user.displayName}</span>
                  <span className="user-menu__chevron" aria-hidden="true">{userOpen ? '▴' : '▾'}</span>
                </button>

                {userOpen && (
                  <div className="user-menu__panel" role="menu">
                    <div className="user-menu__summary">
                      <strong>{user.displayName}</strong>
                      <span>{displayedRole || 'Thành viên'}</span>
                    </div>
                    {!isStaff && workspaceLinks.map((link) => (
                      <NavLink key={link.to} to={link.to} role="menuitem">
                        <span aria-hidden="true">{link.icon}</span>{link.label}
                      </NavLink>
                    ))}
                    {!isStaff && workspaceLinks.length > 0 && <div className="user-menu__separator" />}
                    {accountLinks.map((link) => (
                      <NavLink key={link.to} to={link.to} role="menuitem">
                        <span aria-hidden="true">{link.icon}</span>{link.label}
                      </NavLink>
                    ))}
                    <button type="button" onClick={handleLogout} role="menuitem">
                      <span aria-hidden="true">↪</span>Đăng xuất
                    </button>
                  </div>
                )}
              </div>
            ) : isReady ? (
              <div className="site-header__auth">
                <NavLink className="header-login" to="/login">Đăng nhập</NavLink>
                <NavLink className="button button--gold header-register" to="/register">Đăng ký</NavLink>
              </div>
            ) : null}

            <button
              className="mobile-menu-button"
              type="button"
              onClick={() => setMobileOpen(true)}
              aria-label="Mở menu"
              aria-expanded={mobileOpen}
              aria-controls="mobile-navigation"
            >
              <span />
              <span />
              <span />
            </button>
          </div>
        </div>
      </header>

      {mobileOpen && (
        <>
          <button className="mobile-nav__backdrop" type="button" aria-label="Đóng menu" onClick={() => setMobileOpen(false)} />
          <aside id="mobile-navigation" className="mobile-nav" aria-label="Menu di động">
            <div className="mobile-nav__header">
              <NavLink className="brand" to={homePath}>
                <span className="brand__mark" aria-hidden="true"><span>LT</span><i>✦</i></span>
                <span className="brand__copy"><strong>LTSS</strong><small>{brandSubtitle}</small></span>
              </NavLink>
              <button type="button" onClick={() => setMobileOpen(false)} aria-label="Đóng menu">×</button>
            </div>
            <nav className="mobile-nav__body">
              <p>{isAdministrator ? 'Quản trị' : isModerator ? 'Kiểm duyệt' : isRelicManager ? 'Quản lý di tích' : isBusinessOwner ? 'Doanh nghiệp' : 'Khám phá'}</p>
              {mobileLinks.map(renderMobileItem)}
              {user && !isStaff && (
                <>
                  <p>Tài khoản</p>
                  {workspaceLinks.map(renderMobileItem)}
                  {accountLinks.map((link) => (
                    <NavLink key={link.to} className={drawerLinkClassName} to={link.to}>{link.label}</NavLink>
                  ))}
                </>
              )}
            </nav>
            <div className="mobile-nav__footer">
              {user ? (
                <button className="button button--secondary" type="button" onClick={handleLogout}>Đăng xuất</button>
              ) : (
                <>
                  <NavLink className="button button--secondary" to="/login">Đăng nhập</NavLink>
                  <NavLink className="button button--gold" to="/register">Đăng ký</NavLink>
                </>
              )}
            </div>
          </aside>
        </>
      )}

      {showWorkspace ? (
        <div className="workspace-shell page-container">
          <aside className="workspace-nav" aria-label="Điều hướng khu vực quản lý">
            <div className="workspace-nav__heading">
              <span>Không gian làm việc</span>
              <strong>{isModerationPath ? 'Kiểm duyệt LTSS' : isRelicManagerPath ? 'Quản lý di tích' : isBusinessOwnerPath ? 'Chủ doanh nghiệp' : isAdministrator ? 'Quản trị LTSS' : 'Quản lý LTSS'}</strong>
            </div>
            {workspaceLinks.map(renderWorkspaceItem)}
            <div className="workspace-nav__spacer" />
            {isAdministrator && isModerationPath && (
              <NavLink className="workspace-nav__link" to="/admin/dashboard">
                <span aria-hidden="true">←</span>
                <span className="workspace-nav__label">Quay lại quản trị</span>
              </NavLink>
            )}
            {!isStaff && (
              <NavLink className="workspace-nav__link" to="/">
                <span aria-hidden="true">←</span>
                <span className="workspace-nav__label">Về trang công khai</span>
              </NavLink>
            )}
          </aside>
          <main id="main-content" className="site-main site-main--workspace">
            <Outlet />
          </main>
        </div>
      ) : (
        <main id="main-content" className="site-main page-container">
          <Outlet />
        </main>
      )}

      {!showWorkspace && (
        <footer className="site-footer">
          <div className="site-footer__grid page-container">
            <div className="site-footer__brand">
              <NavLink className="brand" to={homePath}>
                <span className="brand__mark" aria-hidden="true"><span>LT</span><i>✦</i></span>
                <span className="brand__copy"><strong>LTSS</strong><small>Local Tourism Support System</small></span>
              </NavLink>
              <p>Đồng hành cùng bạn khám phá di sản, văn hóa và những trải nghiệm địa phương đáng nhớ tại Sơn Tây.</p>
            </div>
            <div>
              <h2>Khám phá</h2>
              <NavLink to="/places">Địa điểm</NavLink>
              <NavLink to="/events">Sự kiện</NavLink>
              <NavLink to="/tours">Lịch trình</NavLink>
            </div>
            <div>
              <h2>Trải nghiệm</h2>
              <NavLink to="/businesses">Doanh nghiệp</NavLink>
              <NavLink to="/articles">Bài viết</NavLink>
              <NavLink to="/quizzes">Quiz tại điểm đến</NavLink>
            </div>
            <div>
              <h2>Hệ thống</h2>
              <NavLink to="/system-status">Trạng thái dịch vụ</NavLink>
              <NavLink to="/login">Đăng nhập</NavLink>
              <NavLink to="/register">Tạo tài khoản</NavLink>
            </div>
          </div>
          <div className="site-footer__bottom page-container">
            <p>© 2026 LTSS · Sơn Tây, Hà Nội</p>
            <span>Du lịch địa phương · Kết nối cộng đồng</span>
          </div>
        </footer>
      )}
    </div>
  )
}

export default MainLayout
