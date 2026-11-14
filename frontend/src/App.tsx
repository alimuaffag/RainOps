import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { ArrowUpLeft, Droplets, LayoutDashboard, ListFilter, Menu, Plus, X } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import Incidents from './pages/Incidents'
import IncidentDetail from './pages/IncidentDetail'
import IncidentFormPage from './pages/IncidentFormPage'

export default function App() {
  const [menu, setMenu] = useState(false)
  const location = useLocation()
  useEffect(() => {
    function close(event: KeyboardEvent) {
      if (event.key === 'Escape') setMenu(false)
    }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [])
  const main = useRef<HTMLElement>(null)
  useEffect(() => {
    main.current?.focus()
    window.scrollTo({ top: 0 })
  }, [location.pathname])
  return (
    <div className="app">
      <a href="#main" className="skip-link">
        انتقل إلى المحتوى
      </a>
      {menu && (
        <button
          className="nav-backdrop"
          aria-label="إغلاق القائمة"
          onClick={() => setMenu(false)}
        />
      )}
      <aside className={'sidebar ' + (menu ? 'is-open' : '')}>
        <Link className="brand" to="/" onClick={() => setMenu(false)}>
          <span className="brand-mark">
            <Droplets size={26} />
          </span>
          <span dir="ltr">
            RainOps<span className="brand-caption">INCIDENT MANAGEMENT</span>
          </span>
        </Link>
        <button
          className="mobile-close icon-button"
          aria-label="إغلاق القائمة"
          onClick={() => setMenu(false)}
        >
          <X />
        </button>
        <div className="nav-label">مساحة العمل</div>
        <nav aria-label="التنقل الرئيسي">
          <NavLink to="/" end onClick={() => setMenu(false)}>
            <LayoutDashboard size={19} />
            نظرة عامة
          </NavLink>
          <NavLink to="/incidents" end onClick={() => setMenu(false)}>
            <ListFilter size={19} />
            البلاغات
          </NavLink>
          <NavLink to="/incidents/new" onClick={() => setMenu(false)}>
            <Plus size={19} />
            بلاغ جديد
          </NavLink>
        </nav>
        {import.meta.env.VITE_DEMO_MODE === 'true' && (
          <a className="demo-logout" href="/logout">
            نسخة تجريبية · تسجيل الخروج
          </a>
        )}
        <div className="sidebar-footer">
          إدارة بلاغات مياه الأمطار<span dir="ltr">v1.0</span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="topbar-title">
            <button
              className="icon-button mobile-menu"
              aria-label="فتح القائمة"
              aria-expanded={menu}
              onClick={() => setMenu(!menu)}
            >
              <Menu />
            </button>
            <span>مركز العمليات</span>
            <span className="topbar-slash">/</span>
            <strong>RainOps</strong>
          </div>
          <span className="topbar-context">إدارة بلاغات مياه الأمطار</span>
        </header>
        <main id="main" tabIndex={-1} ref={main}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/incidents" element={<Incidents />} />
            <Route path="/incidents/new" element={<IncidentFormPage />} />
            <Route path="/incidents/:id" element={<IncidentDetail />} />
            <Route path="/incidents/:id/edit" element={<IncidentFormPage />} />
            <Route
              path="*"
              element={
                <div className="state">
                  <h1>الصفحة غير موجودة</h1>
                  <Link to="/" className="button">
                    العودة للرئيسية
                    <ArrowUpLeft size={16} />
                  </Link>
                </div>
              }
            />
          </Routes>
        </main>
        <footer className="page-footer">
          <span>RainOps</span>
          <span>إدارة بلاغات مياه الأمطار</span>
        </footer>
      </div>
    </div>
  )
}
