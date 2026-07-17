import AppProviders from './app/providers/AppProviders.jsx'
import AppRouter from './app/router/AppRouter.jsx'

function App() {
  return (
    <AppProviders>
      <AppRouter />
    </AppProviders>
  )
}

export default App
