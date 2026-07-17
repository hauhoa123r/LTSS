import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import FormMessage from '../../auth/components/FormMessage.jsx'
import { placeApi } from '../../places/api/placeApi.js'
import { tourApi } from '../api/tourApi.js'

const EMPTY = { title: '', description: '', region: 'Sơn Tây', difficultyLevel: '', estimatedDistanceKm: '', estimatedDurationMinutes: '', version: null, items: [] }

function TourBuilderPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY)
  const [query, setQuery] = useState('')
  const [places, setPlaces] = useState([])
  const [state, setState] = useState({ loading: Boolean(id), saving: false, error: null })

  useEffect(() => {
    if (!id) return
    let active = true
    tourApi.detail(id).then((tour) => active && setForm({ title: tour.title, description: tour.description || '', region: tour.region || '', difficultyLevel: tour.difficultyLevel || '', estimatedDistanceKm: tour.estimatedDistanceKm ?? '', estimatedDurationMinutes: tour.estimatedDurationMinutes ?? '', version: tour.version, items: tour.items.map((item) => ({ placeId: item.placeId, placeName: item.placeName, durationMinutes: item.durationMinutes || '', transportMethod: item.transportMethod || '', note: item.note || '' })) })).then(() => active && setState({ loading: false, saving: false, error: null })).catch((error) => active && setState({ loading: false, saving: false, error })); return () => { active = false }
  }, [id])

  useEffect(() => { let active = true; placeApi.search({ q: query || undefined, size: 50 }).then((data) => active && setPlaces(data.content)).catch(() => active && setPlaces([])); return () => { active = false } }, [query])

  function add(place) { if (form.items.some((item) => item.placeId === place.id) || form.items.length >= 10) return; setForm({ ...form, items: [...form.items, { placeId: place.id, placeName: place.name, durationMinutes: '', transportMethod: '', note: '' }] }) }
  function remove(index) { setForm({ ...form, items: form.items.filter((_, itemIndex) => itemIndex !== index) }) }
  function move(index, offset) { const nextIndex = index + offset; if (nextIndex < 0 || nextIndex >= form.items.length) return; const items = [...form.items]; [items[index], items[nextIndex]] = [items[nextIndex], items[index]]; setForm({ ...form, items }) }
  function updateItem(index, field, value) { setForm({ ...form, items: form.items.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item) }) }

  async function save(event) {
    event.preventDefault()
    setState({ ...state, saving: true, error: null })
    const payload = { title: form.title, description: form.description || null, region: form.region || null, difficultyLevel: form.difficultyLevel || null, estimatedDistanceKm: form.estimatedDistanceKm === '' ? null : Number(form.estimatedDistanceKm), estimatedDurationMinutes: form.estimatedDurationMinutes === '' ? null : Number(form.estimatedDurationMinutes), version: form.version, items: form.items.map((item) => ({ placeId: item.placeId, plannedStartAt: null, durationMinutes: item.durationMinutes === '' ? null : Number(item.durationMinutes), transportMethod: item.transportMethod || null, note: item.note || null })) }
    try { const tour = id ? await tourApi.update(id, payload) : await tourApi.create(payload); navigate(`/tours/${tour.id}`) } catch (error) { setState({ ...state, saving: false, error }) }
  }

  if (state.loading) return <p className="form-status">Đang tải lịch trình…</p>
  return <section className="tour-builder"><header className="page-heading"><p className="eyebrow">Tour cá nhân</p><h1>{id ? 'Chỉnh sửa lịch trình' : 'Tạo lịch trình mới'}</h1><p>Chọn từ 2 đến 10 địa điểm; thứ tự trong danh sách là thứ tự ghé thăm.</p></header><form onSubmit={save}><div className="tour-builder__main"><section className="tour-builder__details"><label>Tên lịch trình<input required maxLength="200" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label><label>Mô tả<textarea maxLength="5000" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><div className="tour-builder__row"><label>Khu vực<input maxLength="150" value={form.region} onChange={(event) => setForm({ ...form, region: event.target.value })} /></label><label>Độ khó<input maxLength="30" value={form.difficultyLevel} onChange={(event) => setForm({ ...form, difficultyLevel: event.target.value })} /></label></div><div className="tour-builder__row"><label>Khoảng cách dự kiến (km)<input type="number" min="0" step="0.1" value={form.estimatedDistanceKm} onChange={(event) => setForm({ ...form, estimatedDistanceKm: event.target.value })} /></label><label>Thời lượng dự kiến (phút)<input type="number" min="1" value={form.estimatedDurationMinutes} onChange={(event) => setForm({ ...form, estimatedDurationMinutes: event.target.value })} /></label></div></section><aside className="tour-place-picker"><h2>Thêm địa điểm</h2><input type="search" placeholder="Tìm địa điểm…" value={query} onChange={(event) => setQuery(event.target.value)} /><div>{places.map((place) => <button type="button" key={place.id} disabled={form.items.some((item) => item.placeId === place.id) || form.items.length >= 10} onClick={() => add(place)}><strong>{place.name}</strong><span>{place.category?.name}</span></button>)}</div></aside></div><section className="tour-builder__stops"><h2>Điểm dừng ({form.items.length}/10)</h2>{!form.items.length && <p className="content-muted">Chọn ít nhất hai địa điểm ở danh sách bên cạnh.</p>}{form.items.map((item, index) => <article key={item.placeId}><span>{index + 1}</span><div><h3>{item.placeName}</h3><div className="tour-builder__row"><label>Thời lượng (phút)<input type="number" min="1" value={item.durationMinutes} onChange={(event) => updateItem(index, 'durationMinutes', event.target.value)} /></label><label>Phương tiện<input maxLength="50" value={item.transportMethod} onChange={(event) => updateItem(index, 'transportMethod', event.target.value)} /></label></div><label>Ghi chú<input maxLength="1000" value={item.note} onChange={(event) => updateItem(index, 'note', event.target.value)} /></label></div><div className="tour-builder__move"><button type="button" disabled={index === 0} onClick={() => move(index, -1)}>↑</button><button type="button" disabled={index === form.items.length - 1} onClick={() => move(index, 1)}>↓</button><button type="button" onClick={() => remove(index)}>×</button></div></article>)}</section><FormMessage error={state.error} /><div className="hero__actions"><button className="button button--primary" disabled={state.saving || form.items.length < 2}>{state.saving ? 'Đang lưu…' : 'Lưu lịch trình'}</button><Link className="button button--secondary" to="/my-tours">Hủy</Link></div></form></section>
}

export default TourBuilderPage
