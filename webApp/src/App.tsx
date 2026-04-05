import React, { createContext, useContext, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import InventoryPage from './pages/InventoryPage'
import OrdersPage from './pages/OrdersPage'
import CustomersPage from './pages/CustomersPage'
import ExpensesPage from './pages/ExpensesPage'
import PaymentsPage from './pages/PaymentsPage'
import ReportsPage from './pages/ReportsPage'
import SettingsPage from './pages/SettingsPage'
import CyberSourcePage from './pages/CyberSourcePage'
import TaxPage from './pages/TaxPage'
import KraPage from './pages/KraPage'
import SocialPage from './pages/SocialPage'

// ── Auth Context ──────────────────────────────────────────────────────────────
interface AuthCtx { isAuthenticated: boolean; login: () => void; logout: () => void }
export const AuthContext = createContext<AuthCtx>({ isAuthenticated: false, login: () => {}, logout: () => {} })
export const useAuth = () => useContext(AuthContext)

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  return (
    <AuthContext.Provider value={{ isAuthenticated, login: () => setIsAuthenticated(true), logout: () => setIsAuthenticated(false) }}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<PrivateRoute><AppShell /></PrivateRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard"  element={<DashboardPage />} />
            <Route path="inventory"  element={<InventoryPage />} />
            <Route path="orders"     element={<OrdersPage />} />
            <Route path="customers"  element={<CustomersPage />} />
            <Route path="expenses"   element={<ExpensesPage />} />
            <Route path="payments"   element={<PaymentsPage />} />
            <Route path="reports"    element={<ReportsPage />} />
            <Route path="settings"   element={<SettingsPage />} />
            <Route path="card-payments" element={<CyberSourcePage />} />
            <Route path="tax"           element={<TaxPage />} />
            <Route path="kra"           element={<KraPage />} />
            <Route path="social"        element={<SocialPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  )
}
