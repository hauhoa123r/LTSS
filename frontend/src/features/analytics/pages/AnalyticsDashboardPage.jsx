import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { administrationApi } from '../../administration/api/administrationApi.js'
import { INTERNAL_AUDIT_ACTION_CODES } from '../../administration/auditLabels.js'
import { roleLabel } from '../../administration/roleLabels.js'
import { moderationApi } from '../../moderation/api/moderationApi.js'
import { analyticsApi } from '../api/analyticsApi.js'

const DAY = 86400000
const today = new Date().toISOString().slice(0, 10)
const monthAgo = new Date(Date.now() - 29 * DAY).toISOString().slice(0, 10)

const USER_STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  PENDING_VERIFICATION: 'Chờ xác minh',
  DEACTIVATED: 'Đã vô hiệu hóa',
  SUSPENDED: 'Tạm ngưng',
  DELETED: 'Đã xóa',
}

const MODERATION_LABELS = {
  ARTICLE: 'Bài viết',
  EVENT: 'Sự kiện',
  BUSINESS_POST: 'Bài đăng doanh nghiệp',
  PROMOTION: 'Khuyến mãi',
  REVIEW: 'Đánh giá cộng đồng',
  QUIZ: 'Quiz điểm đến',
}

const EVENT_TYPE_LABELS = {
  PLACE_VIEW: 'Xem địa điểm',
  ARTICLE_VIEW: 'Xem bài viết',
  PROMOTION_CLICK: 'Bấm khuyến mãi',
  TOUR_VIEW: 'Xem lịch trình',
  BUSINESS_VIEW: 'Xem doanh nghiệp',
}

const USER_SORTS = {
  name: (a, b) => a.displayName.localeCompare(b.displayName, 'vi'),
  status: (a, b) => a.status.localeCompare(b.status),
  login: (a, b) => new Date(b.lastLoginAt || 0) - new Date(a.lastLoginAt || 0),
}

function toDateInput(date) {
  return date.toISOString().slice(0, 10)
}

function previousRange(range) {
  const from = new Date(`${range.from}T00:00:00Z`)
  const to = new Date(`${range.to}T00:00:00Z`)
  const duration = Math.round((to - from) / DAY) + 1
  return {
    from: toDateInput(new Date(from.getTime() - duration * DAY)),
    to: toDateInput(new Date(from.getTime() - DAY)),
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(value || 0)
}

function formatDate(value) {
  if (!value) return 'Chưa đăng nhập'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function percentageChange(value, previous) {
  if (!previous) return value ? { value: 100, direction: 'up', label: 'mới trong kỳ' } : { value: 0, direction: 'neutral', label: 'không đổi' }
  const valueChange = Math.round(((value - previous) / previous) * 100)
  return {
    value: Math.abs(valueChange),
    direction: valueChange > 0 ? 'up' : valueChange < 0 ? 'down' : 'neutral',
    label: valueChange > 0 ? 'so với kỳ trước' : valueChange < 0 ? 'so với kỳ trước' : 'không đổi',
  }
}

function KpiCard({ icon, label, value, trend, context }) {
  return (
    <article className="admin-kpi-card">
      <div className="admin-kpi-card__top">
        <span className="admin-kpi-card__icon" aria-hidden="true">{icon}</span>
        <span className={`admin-kpi-card__trend admin-kpi-card__trend--${trend.direction}`}>
          {trend.direction === 'up' ? '↑' : trend.direction === 'down' ? '↓' : '—'} {trend.value}%
        </span>
      </div>
      <p>{label}</p>
      <strong>{formatNumber(value)}</strong>
      <small>{context || trend.label}</small>
    </article>
  )
}

function DashboardSkeleton() {
  return (
    <div className="admin-dashboard__skeleton" aria-label="Đang tải bảng điều khiển" aria-busy="true">
      <div className="admin-dashboard__skeleton-heading" />
      <div className="admin-dashboard__skeleton-kpis">
        {Array.from({ length: 6 }, (_, index) => <i key={index} />)}
      </div>
      <div className="admin-dashboard__skeleton-panels"><i /><i /></div>
    </div>
  )
}

function AdminDashboard() {
  const [draftRange, setDraftRange] = useState({ from: monthAgo, to: today })
  const [range, setRange] = useState(draftRange)
  const [state, setState] = useState({ data: null, previous: null, retention: null, moderation: null, audits: null, loading: true, error: null })
  const [userState, setUserState] = useState({ data: null, loading: true, error: null })
  const [userPage, setUserPage] = useState(0)
  const [userSort, setUserSort] = useState('login')

  const loadDashboard = useCallback(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    const prior = previousRange(range)
    Promise.all([
      analyticsApi.dashboard(range),
      analyticsApi.dashboard(prior),
      analyticsApi.retention(),
      moderationApi.queue({ page: 0, size: 4 }),
      administrationApi.auditLogs({ from: range.from, to: range.to, page: 0, size: 4 }),
    ])
      .then(([data, previous, retention, moderation, audits]) => {
        if (active) setState({ data, previous, retention, moderation, audits, loading: false, error: null })
      })
      .catch((error) => active && setState((current) => ({ ...current, loading: false, error })))
    return () => { active = false }
  }, [range])

  useEffect(() => loadDashboard(), [loadDashboard])

  useEffect(() => {
    let active = true
    setUserState((current) => ({ ...current, loading: true, error: null }))
    administrationApi.users({ page: userPage, size: 5 })
      .then((data) => active && setUserState({ data, loading: false, error: null }))
      .catch((error) => active && setUserState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [userPage])

  const sortedUsers = useMemo(
    () => [...(userState.data?.content || [])].sort(USER_SORTS[userSort]),
    [userState.data, userSort],
  )

  const analytics = state.data?.engagement
  const previousAnalytics = state.previous?.engagement
  const maxDaily = Math.max(1, ...(analytics?.daily.map((item) => item.value) || [1]))
  const totalUsers = Object.values(state.data?.usersByStatus || {}).reduce((total, value) => total + value, 0)
  const kpis = analytics ? [
    { icon: '◎', label: 'Tổng người dùng', value: totalUsers, trend: { value: 0, direction: 'neutral' }, context: 'Ảnh chụp toàn hệ thống' },
    { icon: '●', label: 'Người dùng hoạt động', value: state.data.usersByStatus.ACTIVE || 0, trend: { value: 0, direction: 'neutral' }, context: 'Trạng thái tài khoản hiện tại' },
    { icon: '⌖', label: 'Địa điểm công khai', value: state.data.publishedPlaces, trend: { value: 0, direction: 'neutral' }, context: 'Sẵn sàng hiển thị công khai' },
    { icon: '◇', label: 'Doanh nghiệp hoạt động', value: state.data.activeBusinesses, trend: { value: 0, direction: 'neutral' }, context: 'Trạng thái doanh nghiệp hiện tại' },
    { icon: '↗', label: 'Tổng tương tác', value: analytics.totalEvents, trend: percentageChange(analytics.totalEvents, previousAnalytics?.totalEvents) },
    { icon: '◌', label: 'Phiên hoạt động', value: analytics.uniqueSessions, trend: percentageChange(analytics.uniqueSessions, previousAnalytics?.uniqueSessions) },
  ] : []

  return (
    <section className="admin-dashboard" aria-labelledby="admin-dashboard-title">
      <header className="admin-dashboard__hero">
        <div>
          <p className="eyebrow">Không gian quản trị</p>
          <h1 id="admin-dashboard-title">Chào buổi {new Date().getHours() < 12 ? 'sáng' : new Date().getHours() < 18 ? 'chiều' : 'tối'}, quản trị viên</h1>
          <p>Theo dõi vận hành LTSS và xử lý các việc cần ưu tiên từ một nơi.</p>
        </div>
        <div className="admin-dashboard__hero-actions">
          <time dateTime={today}>{new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date())}</time>
          <div>
            <Link className="button button--primary" to="/admin/users">Quản lý người dùng</Link>
            <Link className="button button--secondary" to="/moderation">Mở hàng đợi</Link>
          </div>
        </div>
      </header>

      <form className="admin-dashboard__range" onSubmit={(event) => { event.preventDefault(); setRange(draftRange) }}>
        <div><span aria-hidden="true">◷</span><p><strong>Phạm vi phân tích</strong><small>Xu hướng được so sánh với kỳ liền trước có cùng độ dài.</small></p></div>
        <label>Từ ngày<input type="date" value={draftRange.from} max={draftRange.to} onChange={(event) => setDraftRange({ ...draftRange, from: event.target.value })} /></label>
        <label>Đến ngày<input type="date" value={draftRange.to} min={draftRange.from} max={today} onChange={(event) => setDraftRange({ ...draftRange, to: event.target.value })} /></label>
        <button className="button button--secondary" type="submit">Cập nhật</button>
      </form>

      {state.loading && !state.data && <DashboardSkeleton />}
      {!state.loading && state.error && !state.data && <div className="discovery-error"><FormMessage error={state.error} /><button className="button button--secondary" type="button" onClick={loadDashboard}>Thử lại</button></div>}
      {state.data && <>
        {state.error && <FormMessage error={state.error} />}
        <div className="admin-dashboard__kpis">{kpis.map((kpi) => <KpiCard key={kpi.label} {...kpi} />)}</div>

        <div className="admin-dashboard__primary-grid">
          <section className="admin-dashboard__panel admin-dashboard__panel--chart">
            <header><div><p className="eyebrow">Tương tác</p><h2>Hoạt động theo ngày</h2></div><span>{analytics.daily.length ? `${formatNumber(analytics.totalEvents)} sự kiện` : 'Chưa có dữ liệu'}</span></header>
            {analytics.daily.length ? <div className="admin-dashboard__chart" role="img" aria-label={`Biểu đồ ${formatNumber(analytics.totalEvents)} tương tác từ ${range.from} đến ${range.to}`}>
              {analytics.daily.map((item) => <div key={item.day} title={`${item.day}: ${item.value} tương tác`}><i style={{ height: `${Math.max(10, item.value / maxDaily * 164)}px` }} /><span>{item.day.slice(5)}</span></div>)}
            </div> : <div className="admin-dashboard__empty"><strong>Chưa có tương tác trong kỳ này</strong><span>Hãy thử mở rộng phạm vi ngày để xem dữ liệu.</span></div>}
          </section>

          <aside className="admin-dashboard__panel admin-dashboard__panel--activity">
            <header><div><p className="eyebrow">Cần chú ý</p><h2>Hàng đợi kiểm duyệt</h2></div><Link to="/moderation">Xem tất cả</Link></header>
            {state.moderation?.content.length ? <ul className="admin-dashboard__activity-list">{state.moderation.content.map((item) => <li key={item.id}><span className="admin-dashboard__activity-icon" aria-hidden="true">!</span><div><strong>{item.targetTitle}</strong><small>{MODERATION_LABELS[item.targetType]} · gửi bởi #{item.submittedByUserId}</small></div></li>)}</ul> : <div className="admin-dashboard__empty"><strong>Hàng đợi đang trống</strong><span>Không có nội dung nào cần phê duyệt.</span></div>}
            <footer><span>{formatNumber(state.moderation?.totalElements)} mục đang chờ</span><Link className="button button--secondary" to="/moderation">Xử lý hàng đợi</Link></footer>
          </aside>
        </div>

        <div className="admin-dashboard__secondary-grid">
          <section className="admin-dashboard__panel admin-dashboard__panel--events">
            <header><div><p className="eyebrow">Phân bổ</p><h2>Loại tương tác</h2></div><span>{formatNumber(analytics.authenticatedUsers)} người đã đăng nhập</span></header>
            {analytics.byType.length ? <ul className="admin-dashboard__event-list">{analytics.byType.slice(0, 5).map((item) => <li key={item.code}><div><span>{EVENT_TYPE_LABELS[item.code] || item.code.replaceAll('_', ' ')}</span><strong>{formatNumber(item.value)}</strong></div><i><b style={{ width: `${Math.max(4, item.value / Math.max(1, analytics.totalEvents) * 100)}%` }} /></i></li>)}</ul> : <div className="admin-dashboard__empty"><strong>Chưa có loại sự kiện</strong><span>Dữ liệu sẽ xuất hiện khi người dùng bắt đầu tương tác.</span></div>}
          </section>

          <section className="admin-dashboard__panel admin-dashboard__panel--audits">
            <header><div><p className="eyebrow">Mới nhất</p><h2>Hoạt động quản trị</h2></div><Link to="/admin/audit-logs">Nhật ký</Link></header>
            {state.audits?.content.some((log) => INTERNAL_AUDIT_ACTION_CODES.has(log.actionCode)) ? <ul className="admin-dashboard__audit-list">{state.audits.content.filter((log) => INTERNAL_AUDIT_ACTION_CODES.has(log.actionCode)).map((log) => <li key={log.id}><span aria-hidden="true">↗</span><div><strong>{log.actionLabel || log.actionCode.replaceAll('_', ' ')}</strong><small>{log.actorDisplayName || (log.actorUserId ? `Người dùng #${log.actorUserId}` : 'Hệ thống')} · {formatDate(log.createdAt)}</small></div></li>)}</ul> : <div className="admin-dashboard__empty"><strong>Chưa có hoạt động quản trị</strong><span>Nhật ký trong phạm vi đã chọn sẽ xuất hiện ở đây.</span></div>}
          </section>
        </div>

        <section className="admin-dashboard__panel admin-dashboard__panel--users">
          <header><div><p className="eyebrow">Người dùng</p><h2>Người dùng mới nhất</h2></div><Link to="/admin/users">Quản lý tất cả</Link></header>
          {userState.loading && !userState.data ? <div className="admin-dashboard__table-loading">Đang tải người dùng…</div> : userState.error ? <div className="admin-dashboard__empty"><strong>Không thể tải người dùng</strong><span>{userState.error.message}</span></div> : <>
            <div className="admin-dashboard__table-wrap"><table><thead><tr><th>ID</th><th><button type="button" onClick={() => setUserSort('name')}>Người dùng {userSort === 'name' ? '↓' : ''}</button></th><th><button type="button" onClick={() => setUserSort('status')}>Trạng thái {userSort === 'status' ? '↓' : ''}</button></th><th><button type="button" onClick={() => setUserSort('login')}>Đăng nhập gần nhất {userSort === 'login' ? '↓' : ''}</button></th><th>Vai trò</th></tr></thead><tbody>{sortedUsers.map((user) => <tr key={user.id}><td>#{user.id}</td><td><strong>{user.displayName}</strong><small>{user.email}</small></td><td><span className={`admin-dashboard__status admin-dashboard__status--${user.status.toLowerCase()}`}>{USER_STATUS_LABELS[user.status] || user.status}</span></td><td>{formatDate(user.lastLoginAt)}</td><td>{user.effectiveRoles.map((role) => roleLabel(user, role)).join(', ')}</td></tr>)}</tbody></table></div>
            {!sortedUsers.length && <div className="admin-dashboard__empty"><strong>Chưa có người dùng</strong><span>Dữ liệu người dùng sẽ xuất hiện tại đây.</span></div>}
            <footer className="admin-dashboard__table-footer"><span>Trang {userPage + 1} / {Math.max(1, userState.data?.totalPages || 1)}</span><div><button className="button button--secondary" type="button" disabled={userState.data?.first} onClick={() => setUserPage((page) => page - 1)}>Trước</button><button className="button button--secondary" type="button" disabled={userState.data?.last} onClick={() => setUserPage((page) => page + 1)}>Sau</button></div></footer>
          </>}
        </section>

        {state.retention && <p className="admin-dashboard__data-note">Dữ liệu lưu giữ: {formatNumber(state.retention.engagementEventCount)} tương tác và {formatNumber(state.retention.auditLogCount)} nhật ký. {state.retention.message}</p>}
      </>}
    </section>
  )
}

function BusinessAnalyticsDashboard({ mode }) {
  const [range, setRange] = useState({ from: monthAgo, to: today })
  const [state, setState] = useState({ data: null, retention: null, loading: true, error: null })

  useEffect(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    const main = mode === 'admin' ? analyticsApi.dashboard(range) : analyticsApi.business(range)
    Promise.all([main, mode === 'admin' ? analyticsApi.retention() : Promise.resolve(null)])
      .then(([data, retention]) => active && setState({ data, retention, loading: false, error: null }))
      .catch((error) => active && setState({ data: null, retention: null, loading: false, error }))
    return () => { active = false }
  }, [mode, range])

  const analytics = mode === 'admin' ? state.data?.engagement : state.data
  const maxDaily = Math.max(1, ...(analytics?.daily.map((item) => item.value) || [1]))
  return <section className="analytics-page"><header className="page-heading"><p className="eyebrow">Phân tích vận hành</p><h1>{mode === 'admin' ? 'Bảng điều khiển hệ thống' : 'Hiệu quả doanh nghiệp'}</h1><p>{mode === 'admin' ? 'Dữ liệu tổng hợp theo UTC, chỉ dành cho quản trị viên.' : 'Chỉ số được giới hạn theo doanh nghiệp, địa điểm, bài đăng và ưu đãi thuộc tài khoản.'}</p></header><div className="analytics-range"><label>Từ ngày<input type="date" value={range.from} max={range.to} onChange={(event) => setRange({ ...range, from: event.target.value })} /></label><label>Đến ngày<input type="date" value={range.to} min={range.from} onChange={(event) => setRange({ ...range, to: event.target.value })} /></label></div>{state.loading && <p className="form-status">Đang tổng hợp dữ liệu…</p>}<FormMessage error={state.error} />{analytics && <><div className="metric-grid"><article><span>Tổng tương tác</span><strong>{analytics.totalEvents}</strong></article><article><span>Phiên duy nhất</span><strong>{analytics.uniqueSessions}</strong></article><article><span>Người dùng đăng nhập</span><strong>{analytics.authenticatedUsers}</strong></article></div><div className="analytics-panels"><section><h2>Theo loại sự kiện</h2>{!analytics.byType.length ? <p>Chưa có dữ liệu.</p> : <table><thead><tr><th>Loại</th><th>Số lượng</th></tr></thead><tbody>{analytics.byType.map((item) => <tr key={item.code}><td>{EVENT_TYPE_LABELS[item.code] || item.code.replaceAll('_', ' ')}</td><td>{item.value}</td></tr>)}</tbody></table>}</section><section><h2>Xu hướng theo ngày</h2>{!analytics.daily.length ? <p>Chưa có dữ liệu.</p> : <div className="daily-chart">{analytics.daily.map((item) => <div key={item.day}><span>{item.day.slice(5)}</span><i style={{ height: `${Math.max(8, item.value / maxDaily * 140)}px` }} /><strong>{item.value}</strong></div>)}</div>}</section></div></>}</section>
}

function AnalyticsDashboardPage({ mode }) {
  return mode === 'admin' ? <AdminDashboard /> : <BusinessAnalyticsDashboard mode={mode} />
}

export default AnalyticsDashboardPage
