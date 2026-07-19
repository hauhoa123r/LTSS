import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import FormMessage from '../../../shared/components/FormMessage.jsx'
import { useAuth } from '../../auth/context/AuthContext.jsx'
import { administrationApi } from '../api/administrationApi.js'
import { DEFAULT_ROLE_LABELS, roleLabel } from '../roleLabels.js'

const STATUS_LABELS = {
  PENDING_VERIFICATION: 'Chờ xác minh',
  ACTIVE: 'Đang hoạt động',
  DEACTIVATED: 'Đã vô hiệu hóa',
  SUSPENDED: 'Tạm ngưng',
  DELETED: 'Đã xóa',
}

function formatDate(value) {
  if (!value) return 'Chưa có dữ liệu'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function normalizeText(value) {
  return value?.trim() || ''
}

function formFromAccount(account) {
  return {
    fullName: account.fullName || '',
    displayName: account.displayName || '',
    phone: account.phone || '',
    address: account.address || '',
    reason: '',
  }
}

function AdminUserDetailPage() {
  const { id } = useParams()
  const { user: currentUser } = useAuth()
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const [action, setAction] = useState({ loading: false, error: null, success: '' })
  const [dialog, setDialog] = useState(null)
  const [editForm, setEditForm] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  const load = useCallback(() => {
    setState({ data: null, loading: true, error: null })
    return administrationApi.user(id)
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((error) => setState({ data: null, loading: false, error }))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!dialog) return undefined
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function onKeyDown(event) {
      if (event.key === 'Escape' && !action.loading) setDialog(null)
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [dialog, action.loading])

  const account = state.data
  const self = account?.id === currentUser?.id
  const roleLabels = account?.roleLabels || DEFAULT_ROLE_LABELS
  const roleCodes = useMemo(() => {
    const codes = Object.keys(roleLabels).length ? Object.keys(roleLabels) : Object.keys(DEFAULT_ROLE_LABELS)
    return codes.filter((role) => role !== 'GUEST')
  }, [roleLabels])
  const canEditAccount = Boolean(account && !self && account.status !== 'DELETED')

  function openStatusDialog(status) {
    setAction({ loading: false, error: null, success: '' })
    setDialog({ type: 'status', status, reason: '' })
  }

  function openPasswordDialog() {
    setAction({ loading: false, error: null, success: '' })
    setDialog({ type: 'password', reason: '' })
  }

  function openRoleDialog(role, assigned) {
    setAction({ loading: false, error: null, success: '' })
    setDialog({ type: 'role', role, assigned, reason: '' })
  }

  function startEdit() {
    if (!account) return
    setAction({ loading: false, error: null, success: '' })
    setFieldErrors({})
    setEditForm(formFromAccount(account))
  }

  function cancelEdit() {
    setEditForm(null)
    setFieldErrors({})
    setAction((current) => ({ ...current, error: null }))
  }

  function updateEditField(field, value) {
    setEditForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: null }))
  }

  function validateEditForm() {
    const errors = {}
    if (!normalizeText(editForm.fullName)) errors.fullName = 'Vui lòng nhập họ và tên.'
    if (!normalizeText(editForm.displayName)) errors.displayName = 'Vui lòng nhập tên hiển thị.'
    if (editForm.phone.trim() && !/^[0-9]{10}$/.test(editForm.phone.trim())) {
      errors.phone = 'Số điện thoại phải gồm đúng 10 chữ số.'
    }
    if (!normalizeText(editForm.reason)) errors.reason = 'Vui lòng nhập lý do để lưu nhật ký kiểm toán.'
    setFieldErrors(errors)
    return !Object.keys(errors).length
  }

  async function submitEdit(event) {
    event.preventDefault()
    if (!account || !editForm || !validateEditForm()) return
    setAction({ loading: true, error: null, success: '' })
    try {
      const updated = await administrationApi.updateUser(account.id, {
        fullName: normalizeText(editForm.fullName),
        displayName: normalizeText(editForm.displayName),
        phone: normalizeText(editForm.phone),
        address: normalizeText(editForm.address),
        reason: normalizeText(editForm.reason),
        version: account.version,
      })
      setState({ data: updated, loading: false, error: null })
      setEditForm(null)
      setFieldErrors({})
      setAction({ loading: false, error: null, success: 'User account updated successfully.' })
    } catch (error) {
      setAction({ loading: false, error, success: '' })
    }
  }

  async function submitAction(event) {
    event.preventDefault()
    if (!dialog?.reason.trim() || !state.data) return
    setAction({ loading: true, error: null, success: '' })
    try {
      let updated
      if (dialog.type === 'status') {
        updated = await administrationApi.status(state.data.id, {
          status: dialog.status,
          reason: dialog.reason.trim(),
          version: state.data.version,
        })
      } else if (dialog.type === 'password') {
        await administrationApi.resetPassword(state.data.id, { reason: dialog.reason.trim() })
        setAction({ loading: false, error: null, success: 'Đã đặt lại mật khẩu về 123@123. Người dùng cần đăng nhập lại.' })
        setDialog(null)
        return
      } else if (dialog.assigned) {
        updated = await administrationApi.revokeRole(state.data.id, dialog.role, { reason: dialog.reason.trim() })
      } else {
        updated = await administrationApi.assignRole(state.data.id, dialog.role, { reason: dialog.reason.trim() })
      }
      setState({ data: updated, loading: false, error: null })
      setAction({ loading: false, error: null, success: '' })
      setDialog(null)
    } catch (error) {
      setAction({ loading: false, error, success: '' })
    }
  }

  return (
    <section className="admin-page admin-user-detail" aria-labelledby="admin-user-detail-title">
      <header className="page-heading page-heading--actions">
        <div><p className="eyebrow">Quản trị / Người dùng</p><h1 id="admin-user-detail-title">{account ? account.displayName : 'Chi tiết người dùng'}</h1><p>Xem thông tin tài khoản và cập nhật hồ sơ, bảo mật trong một không gian riêng.</p></div>
        <Link className="button button--secondary" to="/admin/users">← Danh sách người dùng</Link>
      </header>

      {state.loading && <p className="form-status">Đang tải thông tin người dùng…</p>}
      <FormMessage error={state.error} />
      {!state.loading && !state.error && account && <>
        <div className="admin-user-detail__overview">
          <section>
            <p className="eyebrow">Tài khoản</p>
            <h2>{account.fullName || account.displayName}</h2>
            <a href={`mailto:${account.email}`}>{account.email}</a>
            {account.phone && <p>{account.phone}</p>}
            {account.address && <p>{account.address}</p>}
          </section>
          <dl>
            <div><dt>ID</dt><dd>#{account.id}</dd></div>
            <div><dt>Trạng thái</dt><dd><span className={`admin-dashboard__status admin-dashboard__status--${account.status.toLowerCase()}`}>{STATUS_LABELS[account.status] || account.status}</span></dd></div>
            <div><dt>Vai trò trực tiếp</dt><dd>{account.directRoles.map((role) => roleLabel(account, role)).join(', ') || 'Chưa có'}</dd></div>
          </dl>
        </div>

        <section className="admin-user-detail__panel admin-user-detail__edit-panel">
          <header>
            <div><h2>Cập nhật thông tin</h2></div>
            {!editForm && canEditAccount && <button className="button button--primary" type="button" disabled={action.loading} onClick={startEdit}>Sửa thông tin</button>}
          </header>
          <FormMessage success={action.success} />
          {self && <p className="admin-user-detail__notice">Bạn không thể cập nhật tài khoản của chính mình qua luồng quản trị.</p>}
          {account.status === 'DELETED' && <p className="admin-user-detail__notice">Tài khoản đã xóa không hỗ trợ cập nhật thông tin.</p>}
          {!editForm ? (
            <dl className="admin-user-detail__edit-summary">
              <div><dt>Họ và tên</dt><dd>{account.fullName || 'Chưa cập nhật'}</dd></div>
              <div><dt>Tên hiển thị</dt><dd>{account.displayName || 'Chưa cập nhật'}</dd></div>
              <div><dt>Số điện thoại</dt><dd>{account.phone || 'Chưa cập nhật'}</dd></div>
              <div><dt>Địa chỉ</dt><dd>{account.address || 'Chưa cập nhật'}</dd></div>
              <div><dt>Email</dt><dd>{account.email}</dd></div>
            </dl>
          ) : (
            <form className="admin-user-detail__edit-form" onSubmit={submitEdit}>
              <label>Họ và tên <span>*</span><input value={editForm.fullName} maxLength="150" disabled={action.loading} onChange={(event) => updateEditField('fullName', event.target.value)} />{fieldErrors.fullName && <small>{fieldErrors.fullName}</small>}</label>
              <label>Tên hiển thị <span>*</span><input value={editForm.displayName} maxLength="150" disabled={action.loading} onChange={(event) => updateEditField('displayName', event.target.value)} />{fieldErrors.displayName && <small>{fieldErrors.displayName}</small>}</label>
              <label>Email<input value={account.email} disabled readOnly /></label>
              <label>Số điện thoại<input value={editForm.phone} inputMode="numeric" maxLength="10" placeholder="Ví dụ: 0912345678" disabled={action.loading} onChange={(event) => updateEditField('phone', event.target.value.replace(/\D/g, ''))} />{fieldErrors.phone && <small>{fieldErrors.phone}</small>}</label>
              <label className="admin-user-detail__edit-form-wide">Địa chỉ<textarea rows="3" maxLength="500" value={editForm.address} disabled={action.loading} onChange={(event) => updateEditField('address', event.target.value)} /></label>
              <label className="admin-user-detail__edit-form-wide">Lý do thay đổi <span>*</span><textarea rows="4" maxLength="500" value={editForm.reason} placeholder="Nhập lý do để lưu vào nhật ký kiểm toán…" disabled={action.loading} onChange={(event) => updateEditField('reason', event.target.value)} />{fieldErrors.reason && <small>{fieldErrors.reason}</small>}</label>
              <FormMessage error={action.error} />
              <footer>
                <button className="button button--secondary" type="button" disabled={action.loading} onClick={cancelEdit}>Hủy</button>
                <button className="button button--primary" type="submit" disabled={action.loading}>{action.loading ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
              </footer>
            </form>
          )}
        </section>

        <div className="admin-user-detail__grid">
          <section className="admin-user-detail__panel">
            <header><div><p className="eyebrow">Trạng thái</p><h2>Quản lý truy cập</h2></div></header>
            {self ? <p className="admin-user-detail__notice">Bạn không thể thay đổi trạng thái tài khoản của chính mình.</p> : <div className="admin-user-detail__actions">{account.status === 'ACTIVE' && <><button className="button button--secondary" type="button" disabled={action.loading} onClick={() => openStatusDialog('SUSPENDED')}>Tạm ngưng tài khoản</button><button className="button button--danger" type="button" disabled={action.loading} onClick={() => openStatusDialog('DEACTIVATED')}>Vô hiệu hóa</button></>}{['SUSPENDED', 'DEACTIVATED'].includes(account.status) && <button className="button button--primary" type="button" disabled={action.loading} onClick={() => openStatusDialog('ACTIVE')}>Kích hoạt lại</button>}{['PENDING_VERIFICATION', 'DELETED'].includes(account.status) && <p className="admin-user-detail__notice">Trạng thái này không hỗ trợ thay đổi qua luồng quản trị.</p>}</div>}
          </section>

          <section className="admin-user-detail__panel">
            <header><div><p className="eyebrow">Vai trò</p><h2>Phân quyền hiện tại</h2></div></header>
            <p className="admin-user-detail__description">Chọn vai trò trực tiếp cho tài khoản. Mỗi thay đổi cần lý do để lưu vào nhật ký kiểm toán.</p>
            <div className="admin-user-detail__roles">{roleCodes.map((role) => { const assigned = account.directRoles.includes(role); const inherited = !assigned && account.effectiveRoles.includes(role); return <label key={role} title={inherited ? 'Vai trò được kế thừa' : undefined}><input type="checkbox" checked={assigned} disabled={self || action.loading || account.status === 'DELETED'} onChange={() => openRoleDialog(role, assigned)} /><span>{roleLabel(account, role)}</span>{assigned && <small>Trực tiếp</small>}{inherited && <small>Kế thừa</small>}</label> })}</div>
            {self && <p className="admin-user-detail__notice">Bạn không thể thay đổi vai trò của chính mình.</p>}
            {account.status === 'DELETED' && <p className="admin-user-detail__notice">Tài khoản đã xóa không hỗ trợ phân quyền vai trò.</p>}
          </section>
        </div>

        <section className="admin-user-detail__panel admin-user-detail__panel--meta">
          <header><div><p className="eyebrow">Thông tin bổ sung</p><h2>Trạng thái bảo mật</h2></div></header>
          <div className="admin-user-detail__security-actions">
            <div><p className="admin-user-detail__description">Đặt lại mật khẩu tài khoản về mật khẩu mặc định <strong>123@123</strong> và thu hồi phiên đăng nhập hiện tại.</p></div>
            {self ? <p className="admin-user-detail__notice">Bạn không thể đặt lại mật khẩu của chính mình qua luồng quản trị.</p> : account.status === 'DELETED' ? <p className="admin-user-detail__notice">Tài khoản đã xóa không hỗ trợ đặt lại mật khẩu.</p> : <button className="button button--danger" type="button" disabled={action.loading} onClick={openPasswordDialog}>Đặt lại mật khẩu</button>}
          </div>
          <dl className="admin-user-detail__metadata"><div><dt>Email đã xác minh</dt><dd>{formatDate(account.emailVerifiedAt)}</dd></div><div><dt>Khóa đến</dt><dd>{formatDate(account.lockedUntil)}</dd></div><div><dt>Vô hiệu hóa lúc</dt><dd>{formatDate(account.deactivatedAt)}</dd></div><div><dt>Người vô hiệu hóa</dt><dd>{account.deactivatedByUserId ? `#${account.deactivatedByUserId}` : '—'}</dd></div></dl>
        </section>
      </>}

      {dialog && <div className="admin-action-modal__backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !action.loading) setDialog(null) }}>
        <section className="admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="admin-action-modal-title">
          <header>
            <div><p className="eyebrow">Xác nhận thay đổi</p><h2 id="admin-action-modal-title">{dialog.type === 'password' ? 'Đặt lại mật khẩu người dùng' : dialog.type === 'role' ? `${dialog.assigned ? 'Thu hồi' : 'Gán'} vai trò ${roleLabel(account, dialog.role)}` : dialog.status === 'ACTIVE' ? 'Kích hoạt lại tài khoản' : dialog.status === 'SUSPENDED' ? 'Tạm ngưng tài khoản' : 'Vô hiệu hóa tài khoản'}</h2></div>
            <button type="button" aria-label="Đóng cửa sổ" disabled={action.loading} onClick={() => setDialog(null)}>×</button>
          </header>
          <p>{dialog.type === 'password' ? `Mật khẩu của ${account?.displayName} sẽ được đặt về 123@123 và các phiên đăng nhập hiện tại sẽ bị thu hồi.` : dialog.type === 'role' ? `Vai trò ${roleLabel(account, dialog.role)} sẽ được ${dialog.assigned ? 'thu hồi khỏi' : 'gán cho'} ${account?.displayName}.` : `Trạng thái của ${account?.displayName} sẽ chuyển sang ${STATUS_LABELS[dialog.status]}.`}</p>
          <form onSubmit={submitAction}>
            <label htmlFor="admin-action-reason">Lý do thay đổi <span>*</span></label>
            <textarea id="admin-action-reason" autoFocus required maxLength="500" rows="4" value={dialog.reason} placeholder="Nhập lý do để lưu vào nhật ký kiểm toán…" onChange={(event) => setDialog({ ...dialog, reason: event.target.value })} />
            <div className="admin-action-modal__counter">{dialog.reason.length}/500</div>
            <FormMessage error={action.error} />
            <footer><button className="button button--secondary" type="button" disabled={action.loading} onClick={() => setDialog(null)}>Hủy</button><button className={`button ${(dialog.type === 'status' && dialog.status !== 'ACTIVE') || dialog.type === 'password' || (dialog.type === 'role' && dialog.assigned) ? 'button--danger' : 'button--primary'}`} type="submit" disabled={action.loading || !dialog.reason.trim()}>{action.loading ? 'Đang xử lý…' : 'Xác nhận thay đổi'}</button></footer>
          </form>
        </section>
      </div>}
    </section>
  )
}

export default AdminUserDetailPage
