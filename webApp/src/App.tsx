import React, { Suspense, createContext, lazy, useContext, useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import { settingsApi } from './services/api'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const InventoryPage = lazy(() => import('./pages/InventoryPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const PosPage = lazy(() => import('./pages/PosPage'))
const CustomersPage = lazy(() => import('./pages/CustomersPage'))
const ExpensesPage = lazy(() => import('./pages/ExpensesPage'))
const PaymentsPage = lazy(() => import('./pages/PaymentsPage'))
const ReportsPage = lazy(() => import('./pages/ReportsPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))
const CyberSourcePage = lazy(() => import('./pages/CyberSourcePage'))
const CyberSourceSettingsPage = lazy(() => import('./pages/CyberSourceSettingsPage'))
const MpesaSettingsPage = lazy(() => import('./pages/MpesaSettingsPage'))
const ReceiptTemplatePage = lazy(() => import('./pages/ReceiptTemplatePage'))
const SessionTimeoutSettingsPage = lazy(() => import('./pages/SessionTimeoutSettingsPage'))
const TaxPage = lazy(() => import('./pages/TaxPage'))
const KraPage = lazy(() => import('./pages/KraPage'))
const SocialPage = lazy(() => import('./pages/SocialPage'))
const SocialOnboardingPage = lazy(() => import('./pages/SocialOnboardingPage'))
const UserCreationPage = lazy(() => import('./pages/UserCreationPage'))
const BusinessPage = lazy(() => import('./pages/BusinessPage'))
const DownloadsPage = lazy(() => import('./pages/DownloadsPage'))

// ── Auth Context ──────────────────────────────────────────────────────────────
interface AuthUser {
  id: string
  name: string
  email: string
  phone: string
  role: string
  businessId: string | null
  businessName?: string | null
  preferredLanguage: string
}
interface AuthCtx { isAuthenticated: boolean; user: AuthUser | null; login: () => void; logout: () => void }
const DEFAULT_SESSION_IDLE_TIMEOUT_SECONDS = Number(import.meta.env.VITE_SESSION_IDLE_TIMEOUT_SECONDS || 1800)
const LAST_ACTIVITY_KEY = 'sessionLastActivity'
export const AuthContext = createContext<AuthCtx>({ isAuthenticated: false, user: null, login: () => {}, logout: () => {} })
export const useAuth = () => useContext(AuthContext)

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function RoleProtectedRoute({ children, blockedRoles }: { children: React.ReactNode; blockedRoles: string[] }) {
  const { user } = useAuth()
  const role = (user?.role || '').toUpperCase()
  return blockedRoles.includes(role) ? <Navigate to="/dashboard" replace /> : <>{children}</>
}

export default function App() {
  const [sessionIdleTimeoutSeconds, setSessionIdleTimeoutSeconds] = useState(DEFAULT_SESSION_IDLE_TIMEOUT_SECONDS)
  const [isAuthenticated, setIsAuthenticated] = useState(() =>
    Boolean(localStorage.getItem('accessToken') && localStorage.getItem('refreshToken'))
  )
  const [user, setUser] = useState<AuthUser | null>(() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null') } catch { return null }
  })
  const login = () => {
    if (!localStorage.getItem('accessToken') || !localStorage.getItem('refreshToken')) return
    localStorage.setItem('isAuthenticated', 'true')
    localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
    setIsAuthenticated(true)
    try { setUser(JSON.parse(localStorage.getItem('user') || 'null')) } catch { /* ignore */ }
  }
  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('isAuthenticated')
    localStorage.removeItem(LAST_ACTIVITY_KEY)
    localStorage.removeItem('user')
    setIsAuthenticated(false)
    setUser(null)
  }
  useEffect(() => {
    if (!isAuthenticated) return
    let active = true
    settingsApi.getSessionTimeouts()
      .then(res => {
        if (active && res.success && res.data) {
          setSessionIdleTimeoutSeconds(Math.min(86_400, Math.max(60, res.data.webTimeoutSeconds)))
        }
      })
      .catch(() => { /* retain the build-time fallback while offline */ })
    const touch = () => localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
    const updateTimeout = (event: Event) => {
      const seconds = (event as CustomEvent<number>).detail
      if (Number.isFinite(seconds)) setSessionIdleTimeoutSeconds(Math.min(86_400, Math.max(60, seconds)))
    }
    const check = () => {
      if (!localStorage.getItem('accessToken') || !localStorage.getItem('refreshToken')) {
        logout()
        return
      }
      const last = Number(localStorage.getItem(LAST_ACTIVITY_KEY) || Date.now())
      if (Date.now() - last >= sessionIdleTimeoutSeconds * 1000) logout()
    }
    const events = ['mousedown', 'keydown', 'touchstart', 'scroll', 'click']
    events.forEach((event) => window.addEventListener(event, touch, { passive: true }))
    window.addEventListener('session-timeout-updated', updateTimeout)
    const timer = window.setInterval(check, 15_000)
    check()
    return () => {
      active = false
      events.forEach((event) => window.removeEventListener(event, touch))
      window.removeEventListener('session-timeout-updated', updateTimeout)
      window.clearInterval(timer)
    }
  }, [isAuthenticated, sessionIdleTimeoutSeconds])
  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <Suspense fallback={<div style={{ padding: 32, textAlign: 'center' }}>Loading…</div>}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<PrivateRoute><AppShell /></PrivateRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard"  element={<DashboardPage />} />
            <Route path="inventory"  element={<InventoryPage />} />
            <Route path="pos"        element={<PosPage />} />
            <Route path="orders"     element={<OrdersPage />} />
            <Route path="customers"  element={<CustomersPage />} />
            <Route path="expenses"   element={<ExpensesPage />} />
            <Route path="payments"   element={<PaymentsPage />} />
            <Route path="reports"    element={<ReportsPage />} />
            <Route path="downloads"  element={<DownloadsPage />} />
            <Route path="settings"   element={<RoleProtectedRoute blockedRoles={["STAFF"]}><SettingsPage /></RoleProtectedRoute>} />
            <Route path="card-payments" element={<CyberSourcePage />} />
            <Route path="cybersource-settings" element={<RoleProtectedRoute blockedRoles={["STAFF"]}><CyberSourceSettingsPage /></RoleProtectedRoute>} />
            <Route path="tax"           element={<TaxPage />} />
            <Route path="kra"           element={<KraPage />} />
            <Route path="social"        element={<SocialPage />} />
            <Route path="social-onboarding" element={<SocialOnboardingPage />} />
            <Route path="users"         element={<RoleProtectedRoute blockedRoles={["STAFF"]}><UserCreationPage /></RoleProtectedRoute>} />
            <Route path="business"      element={<RoleProtectedRoute blockedRoles={["STAFF"]}><BusinessPage /></RoleProtectedRoute>} />
            <Route path="mpesa-settings" element={<RoleProtectedRoute blockedRoles={["STAFF"]}><MpesaSettingsPage /></RoleProtectedRoute>} />
            <Route path="receipt-template" element={<RoleProtectedRoute blockedRoles={["STAFF"]}><ReceiptTemplatePage /></RoleProtectedRoute>} />
            <Route path="session-timeouts" element={<RoleProtectedRoute blockedRoles={["STAFF", "SUPERADMIN"]}><SessionTimeoutSettingsPage /></RoleProtectedRoute>} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthContext.Provider>
  )
}
