import AppProviders from './providers/AppProviders.jsx'
import AppRouter from './router/AppRouter.jsx'

function App() {
  return (
    <AppProviders>
      <AppRouter />
    </AppProviders>
  )
}

export default App
