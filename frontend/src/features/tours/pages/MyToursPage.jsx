import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { tourApi } from '../api/tourApi.js'

const STATUS = { DRAFT: 'Bản nháp', REJECTED: 'Bị từ chối', PUBLISHED: 'Đã xuất bản', SUBMITTED: 'Đã gửi', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', ARCHIVED: 'Lưu trữ' }

function MyToursPage() {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  function load() { setState({ data: null, loading: true, error: null }); tourApi.mine({ size: 50 }).then((data) => setState({ data, loading: false, error: null })).catch((error) => setState({ data: null, loading: false, error })) }
  useEffect(load, [])
  async function remove(tour) { if (!window.confirm(`Xóa lịch trình “${tour.title}”?`)) return; try { await tourApi.delete(tour.id, tour.version); load() } catch (error) { setState((current) => ({ ...current, error })) } }
  return <section className="tour-page"><header className="page-heading"><p className="eyebrow">Tài khoản</p><h1>Tour của tôi</h1><p>Tạo và sắp xếp lịch trình gồm từ 2 đến 10 địa điểm đã được xuất bản.</p><Link className="button button--primary" to="/my-tours/new">Tạo lịch trình</Link></header>{state.loading && <p className="form-status">Đang tải…</p>}<FormMessage error={state.error} />{!state.loading && !state.data?.content.length && <div className="discovery-empty"><h2>Bạn chưa có lịch trình</h2><p>Hãy bắt đầu bằng hai địa điểm bạn muốn ghé thăm.</p></div>}{state.data?.content.length > 0 && <div className="my-tour-list">{state.data.content.map((tour) => <article key={tour.id}><div><span className="tour-status">{STATUS[tour.status] || tour.status}</span><h2>{tour.title}</h2><p>{tour.stopCount} điểm · {tour.visibility}</p></div><div><Link className="button button--secondary" to={`/tours/${tour.id}`}>Xem</Link>{(tour.status === 'DRAFT' || tour.status === 'REJECTED') && <><Link className="button button--primary" to={`/my-tours/${tour.id}/edit`}>Sửa</Link><button className="button button--danger" onClick={() => remove(tour)}>Xóa</button></>}</div></article>)}</div>}</section>
}

export default MyToursPage
