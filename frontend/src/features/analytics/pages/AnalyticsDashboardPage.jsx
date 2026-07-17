import { useEffect, useState } from 'react'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { analyticsApi } from '../api/analyticsApi.js'

const today = new Date().toISOString().slice(0, 10)
const monthAgo = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10)

function AnalyticsDashboardPage({ mode }) {
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
  return <section className="analytics-page"><header className="page-heading"><p className="eyebrow">Phase 8</p><h1>{mode === 'admin' ? 'Dashboard hệ thống' : 'Hiệu quả doanh nghiệp'}</h1><p>{mode === 'admin' ? 'Dữ liệu tổng hợp theo UTC, chỉ dành cho Administrator.' : 'Chỉ số được giới hạn theo doanh nghiệp, địa điểm, bài đăng và ưu đãi thuộc tài khoản.'}</p></header><div className="analytics-range"><label>Từ ngày<input type="date" value={range.from} max={range.to} onChange={(event) => setRange({ ...range, from: event.target.value })} /></label><label>Đến ngày<input type="date" value={range.to} min={range.from} onChange={(event) => setRange({ ...range, to: event.target.value })} /></label></div>{state.loading && <p className="form-status">Đang tổng hợp dữ liệu…</p>}<FormMessage error={state.error} />{analytics && <>{mode === 'admin' && <div className="metric-grid"><article><span>Địa điểm công khai</span><strong>{state.data.publishedPlaces}</strong></article><article><span>Doanh nghiệp active</span><strong>{state.data.activeBusinesses}</strong></article><article><span>Người dùng active</span><strong>{state.data.usersByStatus.ACTIVE || 0}</strong></article></div>}<div className="metric-grid"><article><span>Tổng tương tác</span><strong>{analytics.totalEvents}</strong></article><article><span>Phiên duy nhất</span><strong>{analytics.uniqueSessions}</strong></article><article><span>Người dùng đăng nhập</span><strong>{analytics.authenticatedUsers}</strong></article></div><div className="analytics-panels"><section><h2>Theo loại sự kiện</h2>{!analytics.byType.length ? <p>Chưa có dữ liệu.</p> : <table><thead><tr><th>Loại</th><th>Số lượng</th></tr></thead><tbody>{analytics.byType.map((item) => <tr key={item.code}><td>{item.code}</td><td>{item.value}</td></tr>)}</tbody></table>}</section><section><h2>Xu hướng theo ngày</h2>{!analytics.daily.length ? <p>Chưa có dữ liệu.</p> : <div className="daily-chart">{analytics.daily.map((item) => <div key={item.day}><span>{item.day.slice(5)}</span><i style={{ height: `${Math.max(8, item.value / maxDaily * 140)}px` }} /><strong>{item.value}</strong></div>)}</div>}</section></div></>}{state.retention && <aside className="retention-card"><h2>Retention</h2><p>{state.retention.message}</p><dl><div><dt>Engagement events</dt><dd>{state.retention.engagementEventCount}</dd></div><div><dt>Audit logs</dt><dd>{state.retention.auditLogCount}</dd></div></dl></aside>}</section>
}

export default AnalyticsDashboardPage
