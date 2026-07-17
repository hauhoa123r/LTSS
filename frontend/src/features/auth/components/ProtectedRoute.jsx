import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function ProtectedRoute({ allowedRoles }) {
  const { isReady, isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isReady) {
    return <p className="form-status">Đang kiểm tra phiên đăng nhập…</p>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (allowedRoles?.length && !allowedRoles.some((role) => user?.roles?.includes(role))) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

export default ProtectedRoute
