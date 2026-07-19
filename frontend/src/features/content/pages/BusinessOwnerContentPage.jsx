import { useEffect, useState } from 'react'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { businessOwnerApi } from '../api/businessOwnerApi.js'

/* ─── Shared style tokens ─────────────────────────────────── */
const C = {
  gold: 'rgba(240,192,64,0.2)',
  goldBorder: '1px solid rgba(240,192,64,0.2)',
  cream: '#fff2c6',
  creamFaint: 'rgba(253,246,236,0.5)',
  creamDim: 'rgba(253,246,236,0.4)',
  bg: 'rgba(0,0,0,0.2)',
  inputBg: 'rgba(255,255,255,0.03)',
  inputBorder: '1px solid rgba(253,246,236,0.15)',
  red: '#d32f2f',
}

const STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ xử lý',
  PUBLISHED: 'Đã xuất bản',
  ACTIVE: 'Đang hoạt động',
  REJECTED: 'Bị từ chối',
  EXPIRED: 'Đã hết hạn',
  ARCHIVED: 'Đã lưu trữ',
}

const BADGE_COLORS = {
  published: { background: 'rgba(15,115,85,0.15)', color: '#26c6da' },
  active:    { background: 'rgba(15,115,85,0.15)', color: '#26c6da' },
  draft:     { background: 'rgba(240,192,64,0.15)', color: '#ffd54f' },
  pending:   { background: 'rgba(240,192,64,0.15)', color: '#ffd54f' },
  rejected:  { background: 'rgba(239,83,80,0.15)',  color: '#ef5350' },
  expired:   { background: 'rgba(120,120,120,0.15)', color: 'rgba(253,246,236,0.4)' },
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value))
}

function discountLabel(item) {
  if (!item.discountType || item.discountValue == null) return 'Ưu đãi đặc biệt'
  if (item.discountType === 'PERCENTAGE') return `-${Number(item.discountValue).toLocaleString('vi-VN')}%`
  if (item.discountType === 'FIXED_AMOUNT') return `-${Number(item.discountValue).toLocaleString('vi-VN')} đ`
  return `-${Number(item.discountValue).toLocaleString('vi-VN')}`
}

/* ─── Reusable field wrapper ─────────────────────────────── */
const fieldStyle = { display: 'flex', flexDirection: 'column', gap: '6px' }
const labelStyle = { fontSize: '0.8rem', color: C.creamFaint, fontWeight: 600 }
const inputStyle = {
  background: C.inputBg,
  border: C.inputBorder,
  borderRadius: '8px',
  padding: '10px 14px',
  color: 'rgba(253,246,236,0.9)',
  fontSize: '0.95rem',
  outline: 'none',
  width: '100%',
  boxSizing: 'border-box',
}

function Field({ label, children }) {
  return (
    <div style={fieldStyle}>
      {label && <label style={labelStyle}>{label}</label>}
      {children}
    </div>
  )
}

/* ─── Toast ───────────────────────────────────────────────── */
function Toast({ message, onClose }) {
  useEffect(() => {
    const t = setTimeout(onClose, 2500)
    return () => clearTimeout(t)
  }, [onClose])
  return (
    <div style={{
      position: 'fixed', bottom: '28px', right: '28px', zIndex: 9999,
      background: '#1c1c1c', border: '1px solid rgba(240,192,64,0.35)',
      color: C.cream, borderRadius: '10px', padding: '14px 22px',
      fontSize: '0.95rem', boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
    }}>
      ✓ {message}
    </div>
  )
}

/* ─── Backdrop ────────────────────────────────────────────── */
function Backdrop({ onClose, children }) {
  return (
    <div
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 1000, padding: '16px',
      }}
    >
      {children}
    </div>
  )
}

/* ─── Modal Shell ─────────────────────────────────────────── */
function ModalShell({ title, onClose, maxWidth = '480px', children }) {
  return (
    <div style={{
      background: '#1c1c1c', border: C.goldBorder, borderRadius: '16px',
      padding: '28px', width: '100%', maxWidth, boxSizing: 'border-box',
    }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        borderBottom: '1px solid rgba(240,192,64,0.1)', paddingBottom: '14px', marginBottom: '22px',
      }}>
        <h2 style={{ color: C.cream, fontSize: '1.1rem', margin: 0 }}>{title}</h2>
        <button onClick={onClose} style={{
          background: 'transparent', border: 'none', color: '#aaa',
          fontSize: '1.5rem', cursor: 'pointer', lineHeight: 1, padding: 0,
        }}>×</button>
      </div>
      {children}
    </div>
  )
}

/* ─── Confirm Dialog ──────────────────────────────────────── */
function ConfirmDialog({ message, onConfirm, onCancel }) {
  return (
    <Backdrop onClose={onCancel}>
      <div style={{
        background: '#1c1c1c', border: C.goldBorder, borderRadius: '16px',
        padding: '32px', maxWidth: '380px', width: '100%', textAlign: 'center',
      }}>
        <p style={{ color: C.cream, fontSize: '1rem', marginBottom: '24px' }}>{message}</p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button className="button button--secondary" onClick={onCancel}>Hủy</button>
          <button className="button button--primary" style={{ background: C.red, borderColor: C.red }} onClick={onConfirm}>
            Xác nhận
          </button>
        </div>
      </div>
    </Backdrop>
  )
}

/* ─── View Post Modal ─────────────────────────────────────── */
function ViewPostModal({ item, onClose }) {
  return (
    <Backdrop onClose={onClose}>
      <ModalShell title="Chi tiết bài viết" onClose={onClose}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <p style={{ color: C.creamFaint, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.08em', margin: '0 0 6px' }}>
              Tiêu đề
            </p>
            <p style={{ color: C.cream, fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>{item.title}</p>
          </div>
          <div>
            <p style={{ color: C.creamFaint, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.08em', margin: '0 0 6px' }}>
              Mô tả
            </p>
            <p style={{ color: 'rgba(253,246,236,0.85)', lineHeight: 1.7, margin: 0 }}>
              {item.summary || item.description
                ? (item.summary || item.description)
                : <em style={{ color: 'rgba(253,246,236,0.3)' }}>Chưa có mô tả</em>}
            </p>
          </div>
        </div>
        <div style={{ marginTop: '28px', display: 'flex', justifyContent: 'flex-end' }}>
          <button className="button button--secondary" onClick={onClose}>Đóng</button>
        </div>
      </ModalShell>
    </Backdrop>
  )
}

/* ─── Post Card ───────────────────────────────────────────── */
function PostCard({ item, onEdit, onDelete, onView }) {
  const statusKey = (item.status || 'DRAFT').toLowerCase()
  const badgeColor = BADGE_COLORS[statusKey] || { background: 'rgba(120,120,120,0.15)', color: '#aaa' }
  return (
    <article style={{
      background: C.bg, border: '1px solid rgba(240,192,64,0.08)',
      borderRadius: '16px', overflow: 'hidden', display: 'flex',
      minHeight: '140px', transition: 'transform 0.2s, border-color 0.2s',
    }}>
      {item.imageUrl ? (
        <img src={item.imageUrl} alt={item.title} style={{
          width: '120px', objectFit: 'cover',
          borderRight: '1px solid rgba(240,192,64,0.05)',
        }} />
      ) : (
        <div style={{
          width: '120px', flexShrink: 0, display: 'grid', placeItems: 'center',
          color: 'rgba(253,246,236,0.2)', fontSize: '2rem',
          borderRight: '1px solid rgba(240,192,64,0.05)',
          background: 'rgba(255,255,255,0.03)',
        }}>🖼️</div>
      )}
      <div style={{ flex: 1, padding: '16px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '12px' }}>
        <div>
          <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'rgba(253,246,236,0.95)', margin: '0 0 6px', lineHeight: 1.4 }}>
            {item.title}
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: '0.8rem', color: C.creamFaint }}>
              {formatDate(item.updatedAt || item.createdAt)}
            </span>
            <span style={{
              display: 'inline-block', alignSelf: 'flex-start',
              fontSize: '0.75rem', fontWeight: 700, padding: '3px 8px',
              borderRadius: '6px', ...badgeColor,
            }}>
              {STATUS_LABELS[item.status] || item.status}
            </span>
          </div>
        </div>
        <div style={{
          display: 'flex', gap: '10px', alignItems: 'center',
          borderTop: '1px solid rgba(255,255,255,0.04)', paddingTop: '10px',
        }}>
          {[
            { icon: '✏️', title: 'Sửa', onClick: onEdit },
            { icon: '🗑️', title: 'Xóa', onClick: onDelete, color: '#ef5350' },
            { icon: '👁️', title: 'Xem', onClick: onView },
          ].map(({ icon, title, onClick, color }) => (
            <button key={title} title={title} onClick={onClick} style={{
              background: 'transparent', border: '1px solid rgba(253,246,236,0.1)',
              cursor: 'pointer', fontSize: '1.1rem', color: color || 'rgba(253,246,236,0.6)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              width: '28px', height: '28px', borderRadius: '6px',
              transition: 'color 0.2s, background 0.2s',
            }}>
              {icon}
            </button>
          ))}
        </div>
      </div>
    </article>
  )
}

/* ─── Promo Card ──────────────────────────────────────────── */
function PromoCard({ item, onEdit, expired }) {
  return (
    <div style={{
      background: C.bg, border: '1px solid rgba(240,192,64,0.08)',
      borderRadius: '16px', padding: '20px',
      display: 'flex', flexDirection: 'column', gap: '16px',
      opacity: expired ? 0.6 : 1,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span style={{
          background: expired ? '#757575' : C.red, color: 'white',
          fontWeight: 800, fontSize: '0.95rem', padding: '4px 10px', borderRadius: '8px',
        }}>
          {discountLabel(item)}
        </span>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'rgba(253,246,236,0.95)', margin: 0 }}>
          {item.title}
        </h3>
      </div>
      <div style={{ fontSize: '0.85rem', color: C.creamDim }}>
        Mã: <strong style={{ color: 'rgba(253,246,236,0.8)', fontFamily: 'monospace', fontSize: '1rem', marginLeft: '6px' }}>
          {item.promoCode || 'N/A'}
        </strong>
      </div>
      <div style={{
        fontSize: '0.85rem', color: C.creamFaint,
        borderTop: '1px solid rgba(255,255,255,0.04)', paddingTop: '12px',
      }}>
        Hết hạn: {formatDate(item.endAt)}
      </div>
      <button
        onClick={expired ? undefined : onEdit}
        disabled={expired}
        style={{
          marginTop: 'auto', width: '100%', padding: '8px',
          borderRadius: '8px', border: expired ? '1px solid rgba(255,255,255,0.1)' : '1px solid rgba(240,192,64,0.3)',
          background: 'transparent',
          color: expired ? 'rgba(253,246,236,0.3)' : C.cream,
          fontWeight: 600, cursor: expired ? 'not-allowed' : 'pointer',
          transition: 'background 0.2s',
        }}
      >
        {expired ? 'Hết hạn' : 'Chỉnh sửa'}
      </button>
    </div>
  )
}

/* ─── Main Page ───────────────────────────────────────────── */
function BusinessOwnerContentPage({ mode }) {
  const [page] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const isPosts = mode === 'posts'

  const [searchQuery, setSearchQuery] = useState('')
  const [localItems, setLocalItems] = useState([])
  const [isEditorOpen, setIsEditorOpen] = useState(false)
  const [editorItem, setEditorItem] = useState(null)
  const [viewItem, setViewItem] = useState(null)
  const [confirmState, setConfirmState] = useState(null)
  const [toast, setToast] = useState(null)
  const showToast = (msg) => setToast(msg)

  const [formFields, setFormFields] = useState({
    title: '', summary: '', content: '',
    discountType: 'PERCENTAGE', discountValue: '',
    promoCode: '', startAt: '', endAt: '', status: 'DRAFT',
  })

  useEffect(() => {
    let active = true
    setState({ data: null, loading: true, error: null })
    const request = isPosts
      ? businessOwnerApi.posts({ page, size: 20 })
      : businessOwnerApi.promotions({ page, size: 20 })
    request
      .then((data) => {
        if (active) {
          setState({ data, loading: false, error: null })
          if (data?.content) setLocalItems(data.content)
        }
      })
      .catch((error) => active && setState({ data: null, loading: false, error }))
    return () => { active = false }
  }, [isPosts, page])

  const filteredItems = localItems.filter(item =>
    item.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    item.promoCode?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleOpenCreate = () => {
    setEditorItem(null)
    setFormFields({
      title: '', summary: '', content: 'Nội dung chi tiết...',
      discountType: 'PERCENTAGE', discountValue: '15',
      promoCode: 'SUMMER' + Math.floor(Math.random() * 90 + 10),
      startAt: new Date().toISOString().substring(0, 10),
      endAt: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString().substring(0, 10),
      status: 'PUBLISHED',
    })
    setIsEditorOpen(true)
  }

  const handleOpenEdit = (item) => {
    setEditorItem(item)
    setFormFields({
      title: item.title || '', summary: item.summary || item.description || '',
      content: item.content || '', discountType: item.discountType || 'PERCENTAGE',
      discountValue: item.discountValue || '', promoCode: item.promoCode || '',
      startAt: item.startAt ? new Date(item.startAt).toISOString().substring(0, 10) : '',
      endAt: item.endAt ? new Date(item.endAt).toISOString().substring(0, 10) : '',
      status: item.status || 'PUBLISHED',
    })
    setIsEditorOpen(true)
  }

  const handleSave = (e) => {
    e.preventDefault()
    if (editorItem) {
      setLocalItems(prev => prev.map(item =>
        item.id === editorItem.id
          ? { ...item, ...formFields, description: formFields.summary, updatedAt: new Date().toISOString() }
          : item
      ))
      showToast('Cập nhật thông tin thành công!')
    } else {
      setLocalItems(prev => [{
        id: Math.floor(Math.random() * 1000 + 50),
        ...formFields, description: formFields.summary,
        updatedAt: new Date().toISOString(), createdAt: new Date().toISOString(),
      }, ...prev])
      showToast('Thêm mới thành công!')
    }
    setIsEditorOpen(false)
  }

  const handleDelete = (id) => {
    setConfirmState({
      message: 'Bạn có chắc chắn muốn xóa mục này?',
      onConfirm: () => {
        setLocalItems(prev => prev.filter(item => item.id !== id))
        setConfirmState(null)
        showToast('Xóa thành công!')
      },
    })
  }

  const setField = (key) => (e) => setFormFields(prev => ({ ...prev, [key]: e.target.value }))

  const activePromotions = filteredItems.filter(item => item.status !== 'EXPIRED')
  const expiredPromotions = filteredItems.filter(item => item.status === 'EXPIRED')

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: '24px', width: '100%' }} aria-labelledby="business-content-title">
      <header className="page-heading">
        <p className="eyebrow">Không gian doanh nghiệp</p>
        <h1 id="business-content-title">{isPosts ? 'Quản lý bài viết' : 'Quản lý khuyến mãi'}</h1>
        <p>
          {isPosts
            ? 'Quản lý trạng thái bài viết và cập nhật nội dung liên quan tới doanh nghiệp của bạn.'
            : 'Quản lý các chương trình ưu đãi, mã giảm giá thuộc doanh nghiệp của bạn.'}
        </p>
      </header>

      {/* Toolbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
          <input
            type="text"
            placeholder="Tìm kiếm..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ ...inputStyle, paddingRight: '40px' }}
          />
          <span style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', color: C.creamFaint }}>🔍</span>
        </div>
        <button className="button button--primary" onClick={handleOpenCreate} style={{ background: C.red, borderColor: C.red }}>
          {isPosts ? 'Thêm bài viết mới' : 'Tạo mã mới'}
        </button>
      </div>

      {state.loading && <p className="form-status">Đang tải nội dung…</p>}
      {!state.loading && state.error && <FormMessage error={state.error} />}

      {!state.loading && !state.error && (
        <>
          {isPosts ? (
            <>
              {!filteredItems.length ? (
                <div className="discovery-empty"><h2>Chưa có bài đăng nào khớp</h2></div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(320px,1fr))', gap: '20px' }}>
                  {filteredItems.map(item => (
                    <PostCard
                      key={item.id}
                      item={item}
                      onEdit={() => handleOpenEdit(item)}
                      onDelete={() => handleDelete(item.id)}
                      onView={() => setViewItem(item)}
                    />
                  ))}
                </div>
              )}
            </>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
              {/* Active promos */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <h2 style={{ fontSize: '1.2rem', color: C.cream, margin: 0, fontWeight: 700 }}>Mã khuyến mãi đang hoạt động</h2>
                {!activePromotions.length ? (
                  <p style={{ color: C.creamDim, fontStyle: 'italic' }}>Không có mã đang hoạt động</p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(280px,1fr))', gap: '20px' }}>
                    {activePromotions.map(item => (
                      <PromoCard key={item.id} item={item} onEdit={() => handleOpenEdit(item)} expired={false} />
                    ))}
                  </div>
                )}
              </div>

              {/* Expired promos */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <h2 style={{ fontSize: '1.2rem', color: C.cream, margin: 0, fontWeight: 700 }}>Mã khuyến mãi đã hết hạn</h2>
                {!expiredPromotions.length ? (
                  <p style={{ color: C.creamDim, fontStyle: 'italic' }}>Không có mã đã hết hạn</p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(280px,1fr))', gap: '20px' }}>
                    {expiredPromotions.map(item => (
                      <PromoCard key={item.id} item={item} expired />
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {/* Modals */}
      {viewItem && <ViewPostModal item={viewItem} onClose={() => setViewItem(null)} />}
      {confirmState && <ConfirmDialog message={confirmState.message} onConfirm={confirmState.onConfirm} onCancel={() => setConfirmState(null)} />}
      {toast && <Toast message={toast} onClose={() => setToast(null)} />}

      {/* Editor Modal */}
      {isEditorOpen && (
        <Backdrop onClose={() => setIsEditorOpen(false)}>
          <ModalShell title={editorItem ? 'Chỉnh sửa thông tin' : 'Tạo mới'} onClose={() => setIsEditorOpen(false)} maxWidth="500px">
            <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <Field label="Tiêu đề">
                <input style={inputStyle} type="text" value={formFields.title} onChange={setField('title')} required />
              </Field>

              {isPosts ? (
                <>
                  <Field label="Tóm tắt ngắn">
                    <textarea style={{ ...inputStyle, minHeight: '100px', resize: 'vertical' }}
                      value={formFields.summary} onChange={setField('summary')} />
                  </Field>
                  <Field label="Trạng thái">
                    <select style={inputStyle} value={formFields.status} onChange={setField('status')}>
                      <option value="DRAFT">Bản nháp</option>
                      <option value="PUBLISHED">Đăng công khai</option>
                    </select>
                  </Field>
                </>
              ) : (
                <>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <Field label="Loại giảm giá">
                      <select style={inputStyle} value={formFields.discountType} onChange={setField('discountType')}>
                        <option value="PERCENTAGE">Phần trăm (%)</option>
                        <option value="FIXED_AMOUNT">Số tiền cố định (đ)</option>
                      </select>
                    </Field>
                    <Field label="Giá trị giảm">
                      <input style={inputStyle} type="number" value={formFields.discountValue} onChange={setField('discountValue')} required />
                    </Field>
                  </div>
                  <Field label="Mã Code">
                    <input style={inputStyle} type="text" value={formFields.promoCode} onChange={setField('promoCode')} required />
                  </Field>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                    <Field label="Ngày bắt đầu">
                      <input style={inputStyle} type="date" value={formFields.startAt} onChange={setField('startAt')} />
                    </Field>
                    <Field label="Ngày hết hạn">
                      <input style={inputStyle} type="date" value={formFields.endAt} onChange={setField('endAt')} />
                    </Field>
                  </div>
                </>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '4px' }}>
                <button type="button" className="button button--secondary" onClick={() => setIsEditorOpen(false)}>Hủy</button>
                <button type="submit" className="button button--primary" style={{ background: C.red, borderColor: C.red }}>Lưu lại</button>
              </div>
            </form>
          </ModalShell>
        </Backdrop>
      )}
    </section>
  )
}

export default BusinessOwnerContentPage
