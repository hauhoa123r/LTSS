import { useCallback, useEffect, useMemo, useState } from 'react'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { analyticsApi } from '../api/analyticsApi.js'

const DAY = 86400000
const PAGE_SIZE = 8
const PRESETS = [
  ['LAST_30', '30 ngày qua'],
  ['LAST_90', '90 ngày qua'],
  ['THIS_MONTH', 'Tháng này'],
  ['LAST_MONTH', 'Tháng trước'],
  ['CUSTOM', 'Tùy chọn'],
]
const STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  PENDING: 'Chờ duyệt',
  SUSPENDED: 'Tạm ngưng',
  INACTIVE: 'Không hoạt động',
  REJECTED: 'Đã từ chối',
}

function dateInput(date) {
  return date.toISOString().slice(0, 10)
}

function presetRange(preset) {
  const now = new Date()
  const today = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()))
  if (preset === 'LAST_90') return { startDate: dateInput(new Date(today.getTime() - 89 * DAY)), endDate: dateInput(today) }
  if (preset === 'THIS_MONTH') return { startDate: dateInput(new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 1))), endDate: dateInput(today) }
  if (preset === 'LAST_MONTH') {
    const start = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth() - 1, 1))
    const end = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 0))
    return { startDate: dateInput(start), endDate: dateInput(end) }
  }
  return { startDate: dateInput(new Date(today.getTime() - 29 * DAY)), endDate: dateInput(today) }
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

function KpiCard({ icon, label, value, trend, meta, active, onClick }) {
  return (
    <button className={`monument-kpi ${active ? 'is-active' : ''}`} type="button" onClick={onClick}>
      <span className="monument-kpi__icon" aria-hidden="true">{icon}</span>
      <span className={`monument-kpi__trend monument-kpi__trend--${trendClass(trend)}`}>
        {trend > 0 ? '+' : ''}{formatNumber(trend, 1)}%
      </span>
      <strong>{value}</strong>
      <small>{label}</small>
      <em>{meta}</em>
      <svg viewBox="0 0 88 28" aria-hidden="true"><path d="M 0 20 C 20 11 28 18 42 10 S 66 2 88 12" /></svg>
    </button>
  )
}

function CategoryDonut({ categories, selectedSlug, onSelect }) {
  const top = categories.filter((item) => item.totalBusinesses > 0).slice(0, 5)
  const total = top.reduce((sum, item) => sum + item.totalBusinesses, 0)
  let offset = 25
  return (
    <div className="monument-donut">
      <svg viewBox="0 0 120 120" role="img" aria-label="Phân bổ tài khoản doanh nghiệp theo loại hình">
        <circle cx="60" cy="60" r="42" />
        {top.map((item) => {
          const value = total ? item.totalBusinesses / total * 100 : 0
          const segment = <circle key={item.categorySlug} className={selectedSlug === item.categorySlug ? 'is-active' : ''} cx="60" cy="60" r="42" pathLength="100" strokeDasharray={`${value} ${100 - value}`} strokeDashoffset={offset} onClick={() => onSelect(selectedSlug === item.categorySlug ? null : item.categorySlug)} />
          offset -= value
          return segment
        })}
        <text x="60" y="57">{formatNumber(total)}</text>
        <text x="60" y="73">tài khoản</text>
      </svg>
      <ul>{top.map((item) => <li key={item.categorySlug} className={selectedSlug === item.categorySlug ? 'is-active' : ''} onClick={() => onSelect(selectedSlug === item.categorySlug ? null : item.categorySlug)}><span />{item.categoryName}<strong>{formatNumber(item.totalBusinesses)}</strong></li>)}</ul>
    </div>
  )
}

function BusinessStatisticsPage() {
  const [preset, setPreset] = useState('LAST_30')
  const [appliedRange, setAppliedRange] = useState(null)
  const [draftRange, setDraftRange] = useState({ startDate: '', endDate: '' })
  const [state, setState] = useState({ data: null, loading: true, error: null, updatedAt: null })
  const [selectedCategorySlug, setSelectedCategorySlug] = useState(null)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState({ key: 'createdAt', direction: 'desc' })
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    analyticsApi.businessStatistics(appliedRange || {})
      .then((data) => {
        if (!active) return
        setState({ data, loading: false, error: null, updatedAt: new Date().toISOString() })
        setDraftRange({ startDate: data.startDate, endDate: data.endDate })
      })
      .catch((error) => active && setState((current) => ({ ...current, loading: false, error })))
    return () => { active = false }
  }, [appliedRange])

  useEffect(() => load(), [load])

  const categories = state.data?.categoryDistribution || []
  const accounts = state.data?.accounts || []
  const selectedCategory = categories.find((item) => item.categorySlug === selectedCategorySlug)
  const activeRangeLabel = state.data ? `${state.data.startDate} → ${state.data.endDate}` : '30 ngày qua'
  const maxStatus = Math.max(1, ...(state.data?.statusBreakdown || []).map((item) => item.value))
  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return accounts
      .filter((item) => !selectedCategorySlug || item.categorySlug === selectedCategorySlug)
      .filter((item) => !normalized || `${item.businessName} ${item.categoryName} ${item.placeSlug}`.toLowerCase().includes(normalized))
      .sort((a, b) => {
        const direction = sort.direction === 'asc' ? 1 : -1
        const aValue = a[sort.key] ?? ''
        const bValue = b[sort.key] ?? ''
        if (typeof aValue === 'string') return aValue.localeCompare(String(bValue), 'vi') * direction
        return (aValue - bValue) * direction
      })
  }, [accounts, query, selectedCategorySlug, sort])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const paged = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)
  const isEmpty = state.data && state.data.totalBusinesses === 0

  useEffect(() => setPage(0), [query, selectedCategorySlug, sort])

  function choosePreset(nextPreset) {
    setPreset(nextPreset)
    setSelectedCategorySlug(null)
    if (nextPreset === 'CUSTOM') return
    const range = presetRange(nextPreset)
    setDraftRange(range)
    setAppliedRange(nextPreset === 'LAST_30' ? null : range)
  }

  function applyCustomRange(event) {
    event.preventDefault()
    if (!draftRange.startDate || !draftRange.endDate) return
    setPreset('CUSTOM')
    setAppliedRange(draftRange)
  }

  function resetFilters() {
    setPreset('LAST_30')
    setAppliedRange(null)
    setSelectedCategorySlug(null)
    setQuery('')
    setSort({ key: 'createdAt', direction: 'desc' })
  }

  function changeSort(key) {
    setSort((current) => current.key === key ? { key, direction: current.direction === 'asc' ? 'desc' : 'asc' } : { key, direction: key === 'businessName' ? 'asc' : 'desc' })
  }

  return (
    <section className="monument-dashboard" aria-labelledby="business-stats-title">
      <header className="monument-dashboard__header">
        <div>
          <h1 id="business-stats-title">Thống kê tài khoản doanh nghiệp</h1>
          <p>Theo dõi trạng thái đối tác, tốc độ đăng ký và phân bổ loại hình kinh doanh trong hệ thống quản trị.</p>
        </div>
        <div className="monument-dashboard__header-actions">
          <span>Cập nhật lần cuối <strong>{formatDateTime(state.data?.generatedAt || state.updatedAt)}</strong></span>
          <button className="button button--secondary" type="button" disabled={state.loading} onClick={load}>Làm mới</button>
        </div>
      </header>

      <form className="monument-filter" onSubmit={applyCustomRange}>
        <div className="monument-filter__presets" role="group" aria-label="Bộ lọc nhanh theo ngày">
          {PRESETS.map(([key, label]) => <button key={key} type="button" className={preset === key ? 'is-active' : ''} onClick={() => choosePreset(key)}>{label}</button>)}
        </div>
        <label>Ngày bắt đầu<input type="date" value={draftRange.startDate} max={draftRange.endDate || undefined} onChange={(event) => { setPreset('CUSTOM'); setDraftRange({ ...draftRange, startDate: event.target.value }) }} /></label>
        <label>Ngày kết thúc<input type="date" value={draftRange.endDate} min={draftRange.startDate || undefined} onChange={(event) => { setPreset('CUSTOM'); setDraftRange({ ...draftRange, endDate: event.target.value }) }} /></label>
        <label>Loại hình<select value={selectedCategorySlug || ''} onChange={(event) => setSelectedCategorySlug(event.target.value || null)}><option value="">Tất cả loại hình</option>{categories.map((item) => <option key={item.categorySlug} value={item.categorySlug}>{item.categoryName}</option>)}</select></label>
        <button className="button button--primary" type="submit" disabled={!draftRange.startDate || !draftRange.endDate}>Áp dụng</button>
        <button className="button button--secondary" type="button" onClick={resetFilters}>Đặt lại</button>
      </form>

      {state.loading && !state.data && <div className="monument-skeleton" aria-busy="true"><i /><i /><i /><i /><b /><b /></div>}
      {!state.loading && state.error && !state.data && <div className="monument-state monument-state--error"><FormMessage error={state.error} /><button className="button button--secondary" type="button" onClick={load}>Thử lại</button><button className="button button--primary" type="button" onClick={resetFilters}>Đặt lại bộ lọc</button></div>}

      {state.data && <>
        {state.error && <FormMessage error={state.error} />}
        <div className="monument-kpi-grid">
          <KpiCard icon="◇" label="Tổng tài khoản" value={formatNumber(state.data.totalBusinesses)} trend={0} meta={activeRangeLabel} active={!selectedCategorySlug} onClick={() => setSelectedCategorySlug(null)} />
          <KpiCard icon="●" label="Đang hoạt động" value={formatNumber(state.data.totalActiveBusinesses)} trend={0} meta="Đủ điều kiện hiển thị công khai" onClick={() => setSelectedCategorySlug(null)} />
          <KpiCard icon="◷" label="Chờ duyệt" value={formatNumber(state.data.pendingApprovals)} trend={0} meta="Cần quản trị viên hoặc kiểm duyệt xử lý" onClick={() => setSelectedCategorySlug(null)} />
          <KpiCard icon="×" label="Tạm ngưng / không hoạt động" value={formatNumber(state.data.inactiveOrSuspendedAccounts)} trend={0} meta={`${formatNumber(state.data.rejectedAccounts)} tài khoản bị từ chối`} onClick={() => setSelectedCategorySlug(null)} />
          <KpiCard icon="↗" label="Đăng ký trong kỳ" value={formatNumber(state.data.currentPeriodRegistrations)} trend={state.data.growthPercent} meta={`${formatNumber(state.data.previousPeriodRegistrations)} tài khoản kỳ trước`} onClick={() => setSelectedCategorySlug(null)} />
          <KpiCard icon="▦" label="Loại hình nổi bật" value={selectedCategory?.categoryName || categories[0]?.categoryName || 'Chưa có'} trend={0} meta={selectedCategory ? `${formatNumber(selectedCategory.totalBusinesses)} tài khoản` : 'Nhấn biểu đồ để lọc'} active={Boolean(selectedCategorySlug)} onClick={() => setSelectedCategorySlug(categories[0]?.categorySlug || null)} />
        </div>

        {isEmpty && <div className="monument-state"><strong>Chưa có tài khoản doanh nghiệp</strong><span>Dữ liệu sẽ xuất hiện khi đối tác đăng ký và được lưu trong hệ thống.</span><button className="button button--secondary" type="button" onClick={resetFilters}>Về 30 ngày qua</button></div>}

        <div className="monument-secondary-grid">
          <section className="monument-card">
            <header><div><p className="eyebrow">Trạng thái</p><h2>Phân bổ tài khoản</h2></div><span>{formatNumber(state.data.totalBusinesses)} tài khoản</span></header>
            {state.data.statusBreakdown.length ? <div className="monument-bar-chart">{state.data.statusBreakdown.map((item) => <button key={item.code} type="button"><span>{STATUS_LABELS[item.code] || item.code}</span><i><b style={{ width: `${Math.max(5, item.value / maxStatus * 100)}%` }} /></i><strong>{formatNumber(item.value)}</strong></button>)}</div> : <div className="monument-empty-mini">Chưa có dữ liệu trạng thái.</div>}
          </section>

          <section className="monument-card">
            <header><div><p className="eyebrow">Loại hình</p><h2>Tỷ trọng ngành kinh doanh</h2></div>{selectedCategorySlug && <button type="button" onClick={() => setSelectedCategorySlug(null)}>Bỏ lọc</button>}</header>
            <CategoryDonut categories={categories} selectedSlug={selectedCategorySlug} onSelect={setSelectedCategorySlug} />
          </section>
        </div>

        <section className="monument-card monument-table-card">
          <header><div><p className="eyebrow">Danh sách đã ẩn thông tin nhạy cảm</p><h2>Tài khoản doanh nghiệp</h2></div><span>{formatNumber(filtered.length)} dòng</span></header>
          <div className="monument-table-toolbar">
            <label>Tìm kiếm<input value={query} placeholder="Tìm theo tên doanh nghiệp, loại hình hoặc đường dẫn" onChange={(event) => setQuery(event.target.value)} /></label>
            {selectedCategorySlug && <button className="button button--secondary" type="button" onClick={() => setSelectedCategorySlug(null)}>Bỏ lọc loại hình</button>}
          </div>
          <div className="monument-table-wrap">
            <table>
              <thead><tr><th>ID</th><th><button type="button" onClick={() => changeSort('businessName')}>Doanh nghiệp</button></th><th><button type="button" onClick={() => changeSort('categoryName')}>Loại hình</button></th><th><button type="button" onClick={() => changeSort('status')}>Trạng thái</button></th><th><button type="button" onClick={() => changeSort('createdAt')}>Ngày đăng ký</button></th><th>Ngày duyệt</th></tr></thead>
              <tbody>{paged.map((item) => <tr key={item.businessId}><td>#{item.businessId}</td><td><strong>{item.businessName}</strong><small>{item.placeSlug}</small></td><td>{item.categoryName}</td><td><span className={`monument-growth monument-growth--${item.status === 'ACTIVE' ? 'up' : item.status === 'PENDING' ? 'neutral' : 'down'}`}>{STATUS_LABELS[item.status] || item.status}</span></td><td>{formatDateTime(item.createdAt)}</td><td>{formatDateTime(item.approvedAt)}</td></tr>)}</tbody>
            </table>
          </div>
          {!paged.length && <div className="monument-empty-mini">Không có doanh nghiệp phù hợp với bộ lọc hiện tại.</div>}
          <footer className="monument-table-footer"><span>Trang {page + 1} / {totalPages}</span><div><button className="button button--secondary" type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button><button className="button button--secondary" type="button" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>Sau</button></div></footer>
        </section>
      </>}
    </section>
  )
}

export default BusinessStatisticsPage
