import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { analyticsApi } from '../api/analyticsApi.js'

const PAGE_SIZE = 8
const PERIOD_LABELS = {
  HISTORICAL: 'Đã diễn ra',
  ACTIVE: 'Đang diễn ra',
  UPCOMING: 'Sắp diễn ra',
}

function currentMonthInput() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function formatNumber(value, digits = 0) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: digits }).format(value || 0)
}

function formatDateTime(value) {
  if (!value) return 'Chưa cập nhật'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function trendClass(value) {
  return value > 0 ? 'up' : value < 0 ? 'down' : 'neutral'
}

function KpiCard({ icon, label, value, trend = 0, meta, active, onClick }) {
  return (
    <button className={`monument-kpi ${active ? 'is-active' : ''}`} type="button" onClick={onClick}>
      <span className="monument-kpi__icon" aria-hidden="true">{icon}</span>
      <span className={`monument-kpi__trend monument-kpi__trend--${trendClass(trend)}`}>
        {trend > 0 ? '+' : ''}{formatNumber(trend, 1)}%
      </span>
      <strong>{value}</strong>
      <small>{label}</small>
      <em>{meta}</em>
      <svg viewBox="0 0 88 28" aria-hidden="true"><path d="M 0 22 C 20 20 25 9 42 14 S 62 5 88 9" /></svg>
    </button>
  )
}

function DailyLineChart({ data }) {
  const width = 840
  const height = 240
  const padding = { top: 18, right: 24, bottom: 34, left: 42 }
  const max = Math.max(1, ...data.map((item) => item.value))
  const chartWidth = width - padding.left - padding.right
  const chartHeight = height - padding.top - padding.bottom
  const points = data.map((item, index) => {
    const x = padding.left + (data.length === 1 ? chartWidth : index / (data.length - 1) * chartWidth)
    const y = padding.top + chartHeight - (item.value / max * chartHeight)
    return { ...item, x, y }
  })
  const path = points.map((point, index) => `${index ? 'L' : 'M'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(' ')
  const area = points.length ? `${path} L ${points.at(-1).x.toFixed(1)} ${height - padding.bottom} L ${points[0].x.toFixed(1)} ${height - padding.bottom} Z` : ''

  return (
    <div className="monument-chart monument-line-chart">
      {!points.length ? <div className="monument-empty-mini">Chưa có tương tác sự kiện trong tháng này.</div> : <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Đăng ký tham gia theo ngày">
        {[0, 0.25, 0.5, 0.75, 1].map((line) => <g key={line}><line x1={padding.left} x2={width - padding.right} y1={padding.top + chartHeight * line} y2={padding.top + chartHeight * line} /><text x="8" y={padding.top + chartHeight * line + 4}>{formatNumber(max * (1 - line))}</text></g>)}
        <path className="monument-line-chart__area" d={area} />
        <path className="monument-line-chart__line" d={path} />
        {points.map((point) => <circle key={point.day} className="monument-chart-point" cx={point.x} cy={point.y} r="5"><title>{`${point.day}: ${formatNumber(point.value)} phiên quan tâm`}</title></circle>)}
        {points.filter((_, index) => index % Math.max(1, Math.ceil(points.length / 8)) === 0 || index === points.length - 1).map((point) => <text key={point.day} className="monument-line-chart__label" x={point.x} y={height - 9}>{point.day.slice(5)}</text>)}
      </svg>}
    </div>
  )
}

function MonthlyEventStatisticsPage() {
  const [monthInput, setMonthInput] = useState(currentMonthInput())
  const [appliedMonth, setAppliedMonth] = useState(null)
  const [state, setState] = useState({ data: null, loading: true, error: null, updatedAt: null })
  const [selectedPeriod, setSelectedPeriod] = useState(null)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState({ key: 'startAt', direction: 'asc' })
  const [page, setPage] = useState(0)

  const params = useMemo(() => {
    if (!appliedMonth) return {}
    const [year, month] = appliedMonth.split('-').map(Number)
    return { year, month }
  }, [appliedMonth])

  const load = useCallback(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    analyticsApi.monthlyEventStatistics(params)
      .then((data) => {
        if (!active) return
        setState({ data, loading: false, error: null, updatedAt: new Date().toISOString() })
        setMonthInput(`${data.year}-${String(data.month).padStart(2, '0')}`)
      })
      .catch((error) => active && setState((current) => ({ ...current, loading: false, error })))
    return () => { active = false }
  }, [params])

  useEffect(() => load(), [load])

  const events = state.data?.events || []
  const maxPeriod = Math.max(1, ...(state.data?.periodBreakdown || []).map((item) => item.value))
  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return events
      .filter((item) => !selectedPeriod || item.periodStatus === selectedPeriod)
      .filter((item) => !normalized || `${item.title} ${item.placeName || ''} ${item.locationNote || ''}`.toLowerCase().includes(normalized))
      .sort((a, b) => {
        const direction = sort.direction === 'asc' ? 1 : -1
        const aValue = a[sort.key] ?? ''
        const bValue = b[sort.key] ?? ''
        if (typeof aValue === 'string') return aValue.localeCompare(String(bValue), 'vi') * direction
        return (aValue - bValue) * direction
      })
  }, [events, query, selectedPeriod, sort])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const paged = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)
  const activeRangeLabel = state.data ? `${state.data.startDate} → ${state.data.endDate}` : 'Tháng hiện tại'
  const isEmpty = state.data && state.data.totalEvents === 0

  useEffect(() => setPage(0), [query, selectedPeriod, sort])

  function applyMonth(event) {
    event.preventDefault()
    if (!monthInput) return
    setAppliedMonth(monthInput)
    setSelectedPeriod(null)
  }

  function resetFilters() {
    setAppliedMonth(null)
    setSelectedPeriod(null)
    setQuery('')
    setSort({ key: 'startAt', direction: 'asc' })
  }

  function changeSort(key) {
    setSort((current) => current.key === key ? { key, direction: current.direction === 'asc' ? 'desc' : 'asc' } : { key, direction: key === 'title' ? 'asc' : 'desc' })
  }

  return (
    <section className="monument-dashboard" aria-labelledby="monthly-event-stats-title">
      <header className="monument-dashboard__header">
        <div>
          <h1 id="monthly-event-stats-title">Thống kê sự kiện theo tháng</h1>
          <p>Theo dõi sự kiện đã diễn ra, đang diễn ra, sắp diễn ra và mức độ quan tâm của người dùng theo từng tháng.</p>
        </div>
        <div className="monument-dashboard__header-actions">
          <span>Cập nhật lần cuối <strong>{formatDateTime(state.data?.generatedAt || state.updatedAt)}</strong></span>
          <button className="button button--secondary" type="button" disabled={state.loading} onClick={load}>Làm mới</button>
        </div>
      </header>

      <form className="monument-filter monument-filter--compact" onSubmit={applyMonth}>
        <div className="monument-filter__presets" role="group" aria-label="Bộ lọc nhanh theo trạng thái">
          <button type="button" className={!selectedPeriod ? 'is-active' : ''} onClick={() => setSelectedPeriod(null)}>Tất cả</button>
          {Object.entries(PERIOD_LABELS).map(([key, label]) => <button key={key} type="button" className={selectedPeriod === key ? 'is-active' : ''} onClick={() => setSelectedPeriod(selectedPeriod === key ? null : key)}>{label}</button>)}
        </div>
        <label>Tháng thống kê<input type="month" value={monthInput} onChange={(event) => setMonthInput(event.target.value)} /></label>
        <button className="button button--primary" type="submit" disabled={!monthInput}>Áp dụng</button>
        <button className="button button--secondary" type="button" onClick={resetFilters}>Tháng hiện tại</button>
      </form>

      {state.loading && !state.data && <div className="monument-skeleton" aria-busy="true"><i /><i /><i /><i /><b /><b /></div>}
      {!state.loading && state.error && !state.data && <div className="monument-state monument-state--error"><FormMessage error={state.error} /><button className="button button--secondary" type="button" onClick={load}>Thử lại</button><button className="button button--primary" type="button" onClick={resetFilters}>Về tháng hiện tại</button></div>}

      {state.data && <>
        {state.error && <FormMessage error={state.error} />}
        <div className="monument-kpi-grid">
          <KpiCard icon="▦" label="Tổng sự kiện" value={formatNumber(state.data.totalEvents)} meta={activeRangeLabel} active={!selectedPeriod} onClick={() => setSelectedPeriod(null)} />
          <KpiCard icon="✓" label="Đã diễn ra" value={formatNumber(state.data.historicalEvents)} meta="Kết thúc trong tháng" active={selectedPeriod === 'HISTORICAL'} onClick={() => setSelectedPeriod('HISTORICAL')} />
          <KpiCard icon="●" label="Đang diễn ra" value={formatNumber(state.data.activeEvents)} meta="Đang trong thời gian tổ chức" active={selectedPeriod === 'ACTIVE'} onClick={() => setSelectedPeriod('ACTIVE')} />
          <KpiCard icon="◷" label="Sắp diễn ra" value={formatNumber(state.data.upcomingEvents)} meta="Còn trong lịch tháng" active={selectedPeriod === 'UPCOMING'} onClick={() => setSelectedPeriod('UPCOMING')} />
          <KpiCard icon="♙" label="Phiên quan tâm" value={formatNumber(state.data.participantRegistrations)} meta={`${formatNumber(state.data.authenticatedParticipants)} người dùng đăng nhập`} onClick={() => setSelectedPeriod(null)} />
          <KpiCard icon="↗" label="Sự kiện nổi bật" value={state.data.highestAttendedEvent || 'Chưa có'} meta={`${formatNumber(state.data.highestAttendedRegistrations)} phiên quan tâm`} onClick={() => setSelectedPeriod(null)} />
        </div>

        {isEmpty && <div className="monument-state"><strong>Chưa có sự kiện trong tháng này</strong><span>Hãy chọn tháng khác hoặc chờ sự kiện mới được xuất bản.</span><button className="button button--secondary" type="button" onClick={resetFilters}>Về tháng hiện tại</button></div>}

        <div className="monument-secondary-grid">
          <section className="monument-card">
            <header><div><p className="eyebrow">Tương tác</p><h2>Phiên quan tâm theo ngày</h2></div><span>{formatNumber(state.data.participantRegistrations)} phiên</span></header>
            <DailyLineChart data={state.data.dailyRegistrations} />
          </section>

          <section className="monument-card">
            <header><div><p className="eyebrow">Trạng thái tháng</p><h2>Vòng đời sự kiện</h2></div>{selectedPeriod && <button type="button" onClick={() => setSelectedPeriod(null)}>Bỏ lọc</button>}</header>
            {state.data.periodBreakdown.length ? <div className="monument-bar-chart">{state.data.periodBreakdown.map((item) => <button key={item.code} className={selectedPeriod === item.code ? 'is-active' : ''} type="button" onClick={() => setSelectedPeriod(selectedPeriod === item.code ? null : item.code)}><span>{PERIOD_LABELS[item.code] || item.code}</span><i><b style={{ width: `${Math.max(5, item.value / maxPeriod * 100)}%` }} /></i><strong>{formatNumber(item.value)}</strong></button>)}</div> : <div className="monument-empty-mini">Chưa có dữ liệu trạng thái.</div>}
          </section>
        </div>

        <section className="monument-card monument-table-card">
          <header><div><p className="eyebrow">Bảng sự kiện</p><h2>Chi tiết sự kiện trong tháng</h2></div><span>{formatNumber(filtered.length)} dòng</span></header>
          <div className="monument-table-toolbar">
            <label>Tìm kiếm<input value={query} placeholder="Tìm theo tên sự kiện hoặc địa điểm" onChange={(event) => setQuery(event.target.value)} /></label>
            {selectedPeriod && <button className="button button--secondary" type="button" onClick={() => setSelectedPeriod(null)}>Bỏ lọc trạng thái</button>}
          </div>
          <div className="monument-table-wrap">
            <table>
              <thead><tr><th>ID</th><th><button type="button" onClick={() => changeSort('title')}>Sự kiện</button></th><th><button type="button" onClick={() => changeSort('periodStatus')}>Trạng thái</button></th><th><button type="button" onClick={() => changeSort('startAt')}>Bắt đầu</button></th><th>Kết thúc</th><th><button type="button" onClick={() => changeSort('participantRegistrations')}>Phiên quan tâm</button></th><th>Thao tác</th></tr></thead>
              <tbody>{paged.map((item) => <tr key={item.eventId}><td>#{item.eventId}</td><td><strong>{item.title}</strong><small>{item.placeName || item.locationNote || item.slug}</small></td><td><span className={`monument-growth monument-growth--${item.periodStatus === 'UPCOMING' ? 'neutral' : item.periodStatus === 'ACTIVE' ? 'up' : 'down'}`}>{PERIOD_LABELS[item.periodStatus] || item.periodStatus}</span></td><td>{formatDateTime(item.startAt)}</td><td>{formatDateTime(item.endAt)}</td><td>{formatNumber(item.participantRegistrations)}</td><td><Link className="button button--secondary" to={`/events/${item.slug}`}>Xem</Link></td></tr>)}</tbody>
            </table>
          </div>
          {!paged.length && <div className="monument-empty-mini">Không có sự kiện phù hợp với bộ lọc hiện tại.</div>}
          <footer className="monument-table-footer"><span>Trang {page + 1} / {totalPages}</span><div><button className="button button--secondary" type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button><button className="button button--secondary" type="button" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>Sau</button></div></footer>
        </section>
      </>}
    </section>
  )
}

export default MonthlyEventStatisticsPage
