import SystemStatusPanel from '../components/SystemStatusPanel.jsx'
import { useSystemHealth } from '../hooks/useSystemHealth.js'

function SystemStatusPage() {
  const { kind, health, error, retry } = useSystemHealth()

  return (
    <section className="status-page" aria-labelledby="system-status-title">
      <header className="page-heading">
        <p className="eyebrow">System health</p>
        <h1 id="system-status-title">Trạng thái hệ thống</h1>
        <p>
          Trang này kiểm tra trực tiếp endpoint health của backend. Kết quả không
          được mô phỏng hoặc lưu cứng ở frontend.
        </p>
      </header>

      <SystemStatusPanel
        kind={kind}
        health={health}
        error={error}
        onRetry={retry}
      />
    </section>
  )
}

export default SystemStatusPage
