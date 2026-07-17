import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function ProtectedRoute() {
  const { isReady, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isReady) {
    return <p className="form-status">Đang kiểm tra phiên đăng nhập…</p>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}

export default ProtectedRoute
