import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { analyticsApi } from '../api/analyticsApi.js'

const DAY = 86400000
const PAGE_SIZE = 8
const PRESETS = [
  ['TODAY', 'Hôm nay'],
  ['LAST_7', '7 ngày qua'],
  ['LAST_30', '30 ngày qua'],
  ['LAST_90', '90 ngày qua'],
  ['THIS_MONTH', 'Tháng này'],
  ['LAST_MONTH', 'Tháng trước'],
  ['CUSTOM', 'Tùy chọn'],
]
const GRANULARITIES = ['DAILY', 'WEEKLY', 'MONTHLY']
const GRANULARITY_LABELS = {
  DAILY: 'Theo ngày',
  WEEKLY: 'Theo tuần',
  MONTHLY: 'Theo tháng',
}

function dateInput(date) {
  return date.toISOString().slice(0, 10)
}

function presetRange(preset) {
  const now = new Date()
  const today = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()))
  if (preset === 'TODAY') return { startDate: dateInput(today), endDate: dateInput(today) }
  if (preset === 'LAST_7') return { startDate: dateInput(new Date(today.getTime() - 6 * DAY)), endDate: dateInput(today) }
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

function formatShortDate(value) {
  if (!value) return 'Chưa có'
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: 'short' }).format(new Date(`${value}T00:00:00Z`))
}

function csvCell(value) {
  const normalized = value === null || value === undefined ? '' : String(value)
  return `"${normalized.replaceAll('"', '""')}"`
}

function encodeUtf16Le(text) {
  const buffer = new ArrayBuffer(2 + text.length * 2)
  const view = new Uint8Array(buffer)
  view[0] = 0xff
  view[1] = 0xfe
  for (let index = 0; index < text.length; index += 1) {
    const code = text.charCodeAt(index)
    view[2 + index * 2] = code & 0xff
    view[3 + index * 2] = code >> 8
  }
  return buffer
}

function csvNumber(value, digits = null) {
  const number = Number(value)
  if (!Number.isFinite(number)) return 0
  return digits === null ? number : number.toFixed(digits)
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
      <svg viewBox="0 0 88 28" aria-hidden="true"><path d="M 0 22 C 18 18 22 8 38 13 S 60 25 88 7" /></svg>
    </button>
  )
}

function LineChart({ data, selectedDay, onSelectDay }) {
  const width = 840
  const height = 260
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
      {!points.length ? <div className="monument-empty-mini">Không có dữ liệu lượt xem.</div> : <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Lượt xem theo thời gian">
        {[0, 0.25, 0.5, 0.75, 1].map((line) => <g key={line}><line x1={padding.left} x2={width - padding.right} y1={padding.top + chartHeight * line} y2={padding.top + chartHeight * line} /><text x="8" y={padding.top + chartHeight * line + 4}>{formatNumber(max * (1 - line))}</text></g>)}
        <path className="monument-line-chart__area" d={area} />
        <path className="monument-line-chart__line" d={path} />
        {points.map((point) => <circle key={point.day} className={`monument-chart-point ${selectedDay === point.day ? 'is-active' : ''}`} cx={point.x} cy={point.y} r="5" role="button" tabIndex="0" onClick={() => onSelectDay(selectedDay === point.day ? null : point.day)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') onSelectDay(selectedDay === point.day ? null : point.day) }}><title>{`${point.day}: ${formatNumber(point.value)} lượt xem`}</title></circle>)}
        {points.filter((_, index) => index % Math.max(1, Math.ceil(points.length / 8)) === 0 || index === points.length - 1).map((point) => <text key={point.day} className="monument-line-chart__label" x={point.x} y={height - 9}>{point.day.slice(5)}</text>)}
      </svg>}
    </div>
  )
}

function DonutChart({ monuments, selectedId, onSelect }) {
  const top = monuments.filter((item) => item.visits > 0).slice(0, 5)
  const total = top.reduce((sum, item) => sum + item.visits, 0)
  let offset = 25
  return (
    <div className="monument-donut">
      <svg viewBox="0 0 120 120" role="img" aria-label="Phân bổ lượt xem">
        <circle cx="60" cy="60" r="42" />
        {top.map((item, index) => {
          const value = total ? item.visits / total * 100 : 0
          const dash = `${value} ${100 - value}`
          const segment = <circle key={item.placeId} className={selectedId === item.placeId ? 'is-active' : ''} cx="60" cy="60" r="42" pathLength="100" strokeDasharray={dash} strokeDashoffset={offset} onClick={() => onSelect(selectedId === item.placeId ? null : item.placeId)} />
          offset -= value
          return segment
        })}
        <text x="60" y="57">{formatNumber(total)}</text>
        <text x="60" y="73">lượt xem</text>
      </svg>
      <ul>{top.map((item) => <li key={item.placeId} className={selectedId === item.placeId ? 'is-active' : ''} onClick={() => onSelect(selectedId === item.placeId ? null : item.placeId)}><span />{item.name}<strong>{formatNumber(item.visits)}</strong></li>)}</ul>
    </div>
  )
}

function Heatmap({ data, selectedDay, onSelectDay }) {
  const max = Math.max(1, ...data.map((item) => item.value))
  return (
    <div className="monument-heatmap">
      {data.length ? data.map((item) => <button key={item.day} className={selectedDay === item.day ? 'is-active' : ''} type="button" style={{ '--density': item.value / max }} title={`${item.day}: ${formatNumber(item.value)} lượt xem`} onClick={() => onSelectDay(selectedDay === item.day ? null : item.day)}><span>{item.day.slice(8)}</span></button>) : <div className="monument-empty-mini">Không có mật độ lượt xem trong khoảng thời gian này.</div>}
    </div>
  )
}

function MonumentStatisticsPage() {
  const [preset, setPreset] = useState('LAST_30')
  const [granularity, setGranularity] = useState('DAILY')
  const [appliedRange, setAppliedRange] = useState(null)
  const [draftRange, setDraftRange] = useState({ startDate: '', endDate: '' })
  const [state, setState] = useState({ data: null, loading: true, error: null, updatedAt: null })
  const [selectedMonumentId, setSelectedMonumentId] = useState(null)
  const [selectedDay, setSelectedDay] = useState(null)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState({ key: 'visits', direction: 'desc' })
  const [page, setPage] = useState(0)

  const params = useMemo(() => ({
    ...(appliedRange || {}),
    granularity,
  }), [appliedRange, granularity])

  const load = useCallback(() => {
    let active = true
    setState((current) => ({ ...current, loading: true, error: null }))
    analyticsApi.monumentStatistics(params)
      .then((data) => {
        if (!active) return
        setState({ data, loading: false, error: null, updatedAt: new Date().toISOString() })
        setDraftRange({ startDate: data.startDate, endDate: data.endDate })
      })
      .catch((error) => active && setState((current) => ({ ...current, loading: false, error })))
    return () => { active = false }
  }, [params])

  useEffect(() => load(), [load])

  const monuments = state.data?.monuments || []
  const topMonuments = monuments.filter((item) => item.visits > 0).slice(0, 8)
  const selectedMonument = monuments.find((item) => item.placeId === selectedMonumentId)
  const activeRangeLabel = state.data ? `${state.data.startDate} → ${state.data.endDate}` : '30 ngày qua'
  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return monuments
      .filter((item) => !selectedMonumentId || item.placeId === selectedMonumentId)
      .filter((item) => !normalized || `${item.name} ${item.address || ''} ${item.slug}`.toLowerCase().includes(normalized))
      .sort((a, b) => {
        const direction = sort.direction === 'asc' ? 1 : -1
        const aValue = a[sort.key] ?? ''
        const bValue = b[sort.key] ?? ''
        if (typeof aValue === 'string') return aValue.localeCompare(bValue, 'vi') * direction
        return (aValue - bValue) * direction
      })
  }, [monuments, query, selectedMonumentId, sort])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const paged = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  useEffect(() => setPage(0), [query, selectedMonumentId, sort])

  function choosePreset(nextPreset) {
    setPreset(nextPreset)
    setSelectedDay(null)
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
    setSelectedDay(null)
  }

  function resetFilters() {
    setPreset('LAST_30')
    setGranularity('DAILY')
    setAppliedRange(null)
    setSelectedMonumentId(null)
    setSelectedDay(null)
    setQuery('')
    setSort({ key: 'visits', direction: 'desc' })
  }

  function changeSort(key) {
    setSort((current) => current.key === key ? { key, direction: current.direction === 'asc' ? 'desc' : 'asc' } : { key, direction: key === 'name' ? 'asc' : 'desc' })
  }

  function exportCsv() {
    if (!state.data) return
    const headers = ['Xếp hạng', 'Di tích', 'Lượt xem', 'Tăng trưởng %', 'Đường dẫn']
    const rows = filtered.map((item, index) => [
      index + 1,
      item.name || '',
      csvNumber(item.visits),
      csvNumber(item.growthPercent, 1),
      item.slug || '',
    ])
    const csvBody = [headers, ...rows].map((row) => row.map(csvCell).join('\t')).join('\r\n')
    const url = URL.createObjectURL(new Blob([encodeUtf16Le(csvBody)], { type: 'text/csv;charset=utf-16le;' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `thong-ke-luot-xem-di-tich-${state.data.startDate}-${state.data.endDate}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  const isEmpty = state.data && state.data.totalVisits === 0

  return (
    <section className="monument-dashboard" aria-labelledby="monument-stats-title">
      <header className="monument-dashboard__header">
        <div>
          <h1 id="monument-stats-title">Thống kê lượt xem di tích</h1>
          <p>Phân tích lượt xem di tích lịch sử theo thời gian, xếp hạng, mật độ truy cập và so sánh theo kỳ.</p>
        </div>
        <div className="monument-dashboard__header-actions">
          <span>Cập nhật lần cuối <strong>{formatDateTime(state.data?.generatedAt || state.updatedAt)}</strong></span>
          <button className="button button--secondary" type="button" disabled={state.loading} onClick={load}>Làm mới</button>
          <button className="button button--primary" type="button" disabled={!state.data} onClick={exportCsv}>Xuất CSV</button>
        </div>
      </header>

      <form className="monument-filter" onSubmit={applyCustomRange}>
        <div className="monument-filter__presets" role="group" aria-label="Bộ lọc nhanh theo ngày">
          {PRESETS.map(([key, label]) => <button key={key} type="button" className={preset === key ? 'is-active' : ''} onClick={() => choosePreset(key)}>{label}</button>)}
        </div>
        <label>Ngày bắt đầu<input type="date" value={draftRange.startDate} max={draftRange.endDate || undefined} onChange={(event) => { setPreset('CUSTOM'); setDraftRange({ ...draftRange, startDate: event.target.value }) }} /></label>
        <label>Ngày kết thúc<input type="date" value={draftRange.endDate} min={draftRange.startDate || undefined} onChange={(event) => { setPreset('CUSTOM'); setDraftRange({ ...draftRange, endDate: event.target.value }) }} /></label>
        <label>Mức gom dữ liệu<select value={granularity} onChange={(event) => setGranularity(event.target.value)}>{GRANULARITIES.map((item) => <option key={item} value={item}>{GRANULARITY_LABELS[item]}</option>)}</select></label>
        <button className="button button--primary" type="submit" disabled={!draftRange.startDate || !draftRange.endDate}>Áp dụng</button>
        <button className="button button--secondary" type="button" onClick={resetFilters}>Đặt lại</button>
      </form>

      {state.loading && !state.data && <div className="monument-skeleton" aria-busy="true"><i /><i /><i /><i /><b /><b /></div>}
      {!state.loading && state.error && !state.data && <div className="monument-state monument-state--error"><FormMessage error={state.error} /><button className="button button--secondary" type="button" onClick={load}>Thử lại</button><button className="button button--primary" type="button" onClick={resetFilters}>Đặt lại bộ lọc</button></div>}

      {state.data && <>
        {state.error && <FormMessage error={state.error} />}
        <div className="monument-kpi-grid">
          <KpiCard icon="◌" label="Tổng lượt xem" value={formatNumber(state.data.totalVisits)} trend={state.data.growthPercent} meta={activeRangeLabel} active={!selectedMonumentId} onClick={() => setSelectedMonumentId(null)} />
          <KpiCard icon="◷" label="Ngày cao điểm" value={state.data.peakVisitDay ? formatShortDate(state.data.peakVisitDay) : 'Chưa có'} trend={0} meta={`${formatNumber(state.data.peakVisitCount)} lượt xem`} onClick={() => setSelectedDay(state.data.peakVisitDay)} />
          <KpiCard icon="⌖" label="Di tích được xem nhiều nhất" value={state.data.mostVisitedMonument || 'Chưa có'} trend={topMonuments[0]?.growthPercent || 0} meta={topMonuments[0] ? `${formatNumber(topMonuments[0].visits)} lượt xem` : 'Chưa có lượt xem'} active={selectedMonumentId === topMonuments[0]?.placeId} onClick={() => setSelectedMonumentId(topMonuments[0]?.placeId || null)} />
          <KpiCard icon="▦" label="Di tích có lượt xem" value={formatNumber(state.data.activeMonuments)} trend={0} meta={`${formatNumber(monuments.length)} di tích công khai`} onClick={() => setSelectedMonumentId(null)} />
          <KpiCard icon="△" label="Tăng trưởng so với kỳ trước" value={`${state.data.growthPercent > 0 ? '+' : ''}${formatNumber(state.data.growthPercent, 1)}%`} trend={state.data.growthPercent} meta={`${formatNumber(state.data.previousTotalVisits)} lượt xem kỳ trước`} onClick={() => setSelectedMonumentId(null)} />
        </div>

        {isEmpty && <div className="monument-state"><strong>Chưa có dữ liệu lượt xem trong kỳ này</strong><span>Hãy thử mở rộng khoảng thời gian hoặc chờ đến khi người dùng mở trang chi tiết di tích.</span><button className="button button--secondary" type="button" onClick={resetFilters}>Về 30 ngày qua</button></div>}

        <section className="monument-card monument-card--line">
          <header><div><p className="eyebrow">Biểu đồ chính</p><h2>Lượt xem theo thời gian</h2></div><div><span>{GRANULARITY_LABELS[granularity]}</span>{selectedDay && <button type="button" onClick={() => setSelectedDay(null)}>Bỏ chọn ngày</button>}</div></header>
          <LineChart data={state.data.trends} selectedDay={selectedDay} onSelectDay={setSelectedDay} />
        </section>

        <div className="monument-secondary-grid">
          <section className="monument-card">
            <header><div><p className="eyebrow">Xếp hạng</p><h2>Di tích được xem nhiều</h2></div>{selectedMonument && <button type="button" onClick={() => setSelectedMonumentId(null)}>Bỏ lọc</button>}</header>
            {topMonuments.length ? <div className="monument-bar-chart">{topMonuments.map((item) => <button key={item.placeId} className={selectedMonumentId === item.placeId ? 'is-active' : ''} type="button" onClick={() => setSelectedMonumentId(selectedMonumentId === item.placeId ? null : item.placeId)}><span>{item.name}</span><i><b style={{ width: `${Math.max(5, item.visits / Math.max(1, topMonuments[0].visits) * 100)}%` }} /></i><strong>{formatNumber(item.visits)}</strong></button>)}</div> : <div className="monument-empty-mini">Chưa có lượt xem để xếp hạng.</div>}
          </section>

          <section className="monument-card">
            <header><div><p className="eyebrow">Phân bổ</p><h2>Tỷ trọng lượt xem</h2></div><span>5 cao nhất</span></header>
            <DonutChart monuments={monuments} selectedId={selectedMonumentId} onSelect={setSelectedMonumentId} />
          </section>
        </div>

        <section className="monument-card">
          <header><div><p className="eyebrow">Mật độ</p><h2>Bản đồ nhiệt lượt xem</h2></div><span>Chọn một ngày để xem nhanh</span></header>
          <Heatmap data={state.data.dailyTrends || state.data.trends} selectedDay={selectedDay} onSelectDay={setSelectedDay} />
        </section>

        <section className="monument-card monument-table-card">
          <header><div><p className="eyebrow">Bảng xếp hạng</p><h2>Hiệu suất từng di tích</h2></div><span>{formatNumber(filtered.length)} dòng</span></header>
          <div className="monument-table-toolbar">
            <label>Tìm kiếm<input value={query} placeholder="Tìm theo tên di tích hoặc địa chỉ" onChange={(event) => setQuery(event.target.value)} /></label>
            {selectedMonument && <button className="button button--secondary" type="button" onClick={() => setSelectedMonumentId(null)}>Bỏ lọc chéo</button>}
          </div>
          <div className="monument-table-wrap">
            <table>
              <thead><tr><th>Hạng</th><th><button type="button" onClick={() => changeSort('name')}>Di tích</button></th><th><button type="button" onClick={() => changeSort('visits')}>Lượt xem</button></th><th><button type="button" onClick={() => changeSort('growthPercent')}>Tăng trưởng</button></th><th>Thao tác</th></tr></thead>
              <tbody>{paged.map((item, index) => <tr key={item.placeId}><td>#{page * PAGE_SIZE + index + 1}</td><td><strong>{item.name}</strong><small>{item.address || item.slug}</small></td><td>{formatNumber(item.visits)}</td><td><span className={`monument-growth monument-growth--${trendClass(item.growthPercent)}`}>{item.growthPercent > 0 ? '+' : ''}{formatNumber(item.growthPercent, 1)}%</span></td><td><Link className="button button--secondary" to={`/places/${item.slug}`}>Xem</Link></td></tr>)}</tbody>
            </table>
          </div>
          {!paged.length && <div className="monument-empty-mini">Không có di tích phù hợp với bộ lọc hiện tại.</div>}
          <footer className="monument-table-footer"><span>Trang {page + 1} / {totalPages}</span><div><button className="button button--secondary" type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</button><button className="button button--secondary" type="button" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>Sau</button></div></footer>
        </section>
      </>}
    </section>
  )
}

export default MonumentStatisticsPage
