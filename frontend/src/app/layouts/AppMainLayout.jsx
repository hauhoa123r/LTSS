import MainLayout from '../../layouts/MainLayout.jsx'
import { useMainNavigation } from '../hooks/useMainNavigation.js'

function AppMainLayout() {
  return <MainLayout {...useMainNavigation()} />
}

export default AppMainLayout
