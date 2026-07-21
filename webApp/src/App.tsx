import React, { createContext, useContext, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import InventoryPage from './pages/InventoryPage'
import OrdersPage from './pages/OrdersPage'
import PosPage from './pages/PosPage'
import CustomersPage from './pages/CustomersPage'
import ExpensesPage from './pages/ExpensesPage'
import PaymentsPage from './pages/PaymentsPage'
import ReportsPage from './pages/ReportsPage'
import SettingsPage from './pages/SettingsPage'
import CyberSourcePage from './pages/CyberSourcePage'
import CyberSourceSettingsPage from './pages/CyberSourceSettingsPage'
import MpesaSettingsPage from './pages/MpesaSettingsPage'
import ReceiptTemplatePage from './pages/ReceiptTemplatePage'
import TaxPage from './pages/TaxPage'
import KraPage from './pages/KraPage'
import SocialPage from './pages/SocialPage'
import SocialOnboardingPage from './pages/SocialOnboardingPage'
import UserCreationPage from './pages/UserCreationPage'
import BusinessPage from './pages/BusinessPage'

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
  const [isAuthenticated, setIsAuthenticated] = useState(() =>
    Boolean(localStorage.getItem('accessToken') && localStorage.getItem('refreshToken'))
  )
  const [user, setUser] = useState<AuthUser | null>(() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null') } catch { return null }
  })
  const login = () => {
    if (!localStorage.getItem('accessToken') || !localStorage.getItem('refreshToken')) return
    localStorage.setItem('isAuthenticated', 'true')
    setIsAuthenticated(true)
    try { setUser(JSON.parse(localStorage.getItem('user') || 'null')) } catch { /* ignore */ }
  }
  const logout = () => {
    localStorage.removeItem('isAuthenticated')
    localStorage.removeItem('user')
    setIsAuthenticated(false)
    setUser(null)
  }
  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
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
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  )
}
