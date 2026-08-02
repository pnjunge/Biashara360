import React, { useEffect, useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../App'
import {
  LayoutDashboard, Package, ShoppingCart, Users, Receipt,
  CreditCard, BarChart3, Settings, LogOut, Bell, Search,
  ChevronLeft, ChevronRight, ChevronDown, Menu, FileCheck, MessageSquare, UserPlus, Building2, Store, ShoppingBag, Link, Download
} from 'lucide-react'
import styles from './AppShell.module.css'
import { accessApi, hospitalityApi } from '../../services/api'

const navItems = [
  { key:'DASHBOARD', to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { key:'POS', to: '/pos',           icon: Store,           label: 'Point of Sale' },
  { key:'HOSPITALITY', to: '/hospitality', icon: Receipt, label: 'Bar & Restaurant' },
  { key:'HOSPITALITY_OPS', to: '/hospitality-operations', icon: Building2, label: 'Hospitality Operations' },
  { key:'OPEN_TABS', to: '/open-tabs', icon: ShoppingCart, label: 'Open Tabs' },
  { key:'INVENTORY', to: '/inventory',     icon: Package,         label: 'Inventory' },
  { key:'ORDERS', to: '/orders',        icon: ShoppingCart,    label: 'Orders' },
  { key:'CUSTOMERS', to: '/customers',     icon: Users,           label: 'Customers' },
  { key:'EXPENSES', to: '/expenses',      icon: Receipt,         label: 'Expenses' },
  { key:'PAYMENTS', to: '/payments',      icon: CreditCard,      label: 'Payments' },
  { key:'TAX', to: '/tax',           icon: Receipt,          label: 'Tax' },
  { key:'KRA', to: '/kra',           icon: FileCheck,        label: 'KRA iTax' },
  { key:'SOCIAL', to: '/social',        icon: MessageSquare,    label: 'Social Inbox' },
  { key:'SOCIAL_SETUP', to: '/social-onboarding', icon: Link,         label: 'Social Setup' },
  { key:'USERS', to: '/users',         icon: UserPlus,         label: 'Users & Access' },
  { key:'REPORTS', to: '/reports',       icon: BarChart3,        label: 'Reports' },
  { key:'DOWNLOADS', to: '/downloads',     icon: Download,         label: 'Download Apps' },
]

export default function AppShell() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [showProfileMenu, setShowProfileMenu] = useState(false)
  const [allowedMenus, setAllowedMenus] = useState<Set<string> | null>(null)
  const [hospitalityEnabled, setHospitalityEnabled] = useState<boolean | null>(null)
  useEffect(() => {
    accessApi.me().then(result => {
      if (result.success && result.data) setAllowedMenus(new Set(result.data.enabledMenus))
    }).catch(() => setAllowedMenus(null))
    hospitalityApi.status().then(result => {
      if (result.success && result.data) setHospitalityEnabled(result.data.enabled)
    }).catch(() => setHospitalityEnabled(null))
    const handleModeChange = (event: Event) => {
      const enabled = (event as CustomEvent<{ enabled: boolean }>).detail?.enabled
      if (typeof enabled === 'boolean') setHospitalityEnabled(enabled)
    }
    window.addEventListener('hospitality-mode-changed', handleModeChange)
    return () => window.removeEventListener('hospitality-mode-changed', handleModeChange)
  }, [user?.id])
  const isStaff = (user?.role || '').toUpperCase() === 'STAFF'
  const visibleNavItems = navItems.filter(item => {
    if (allowedMenus && !allowedMenus.has(item.key) && !(item.key === 'PAYMENTS' && allowedMenus.has('CARD_PAYMENTS'))) return false
    if ((item.key === 'HOSPITALITY' || item.key === 'HOSPITALITY_OPS' || item.key === 'OPEN_TABS') && hospitalityEnabled !== true) return false
    if (!isStaff) return true
    return item.to !== '/users' && item.to !== '/business' && item.to !== '/cybersource-settings'
  })

  const userInitials = user?.name?.split(' ').map((n: string) => n[0]).join('').toUpperCase() || 'U'

  return (
    <div className={styles.shell}>
      {/* Backdrop for mobile */}
      {mobileOpen && <div className={styles.backdrop} onClick={() => setMobileOpen(false)} />}

      {/* ── Sidebar ── */}
      <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''} ${mobileOpen ? styles.mobileOpen : ''}`}>
        <div className={styles.sidebarTop}>
          <div className={styles.logo}>
            <div className={styles.logoIcon}>
              <ShoppingBag size={18} color="white" />
            </div>
            {!collapsed && (
              <div>
                <div className={styles.logoName}>Biashara360</div>
                <div className={styles.logoSub}>Business Management</div>
              </div>
            )}
          </div>
        </div>

        <nav className={styles.nav}>
          {visibleNavItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to} to={to}
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              title={collapsed ? label : undefined}
              onClick={() => setMobileOpen(false)}
            >
              <Icon size={18} className={styles.navIcon} />
              {!collapsed && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className={styles.sidebarBottom}>
          {!isStaff && (!allowedMenus || allowedMenus.has('SETTINGS')) && (
            <NavLink
              to="/settings"
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              title={collapsed ? 'Settings' : undefined}
              onClick={() => setMobileOpen(false)}
            >
              <Settings size={18} className={styles.navIcon} />
              {!collapsed && <span>Settings</span>}
            </NavLink>
          )}
          
          <button
            className={styles.collapseBtn}
            onClick={() => setCollapsed(c => !c)}
            title={collapsed ? "Expand Sidebar" : "Collapse Sidebar"}
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
            {!collapsed && <span style={{ marginLeft: 8 }}>Collapse Sidebar</span>}
          </button>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className={styles.main}>
        <header className={styles.topbar}>
          <button className={styles.menuToggle} onClick={() => setMobileOpen(true)}>
            <Menu size={20} />
          </button>
          
          <div className={styles.searchWrap}>
            <Search size={16} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Search anything..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>

          <div className={styles.topbarRight}>
            <button className={styles.iconBtn} title="Notifications">
              <Bell size={18} />
              <span className={styles.notifDot} />
            </button>

            <div style={{ position: 'relative' }}>
              <div
                className={styles.topbarUserCard}
                onClick={() => setShowProfileMenu(!showProfileMenu)}
              >
                <div className={styles.topbarAvatar}>
                  {userInitials}
                </div>
                <div className={styles.topbarUserInfo}>
                  <div className={styles.topbarUserName}>{user?.name ?? 'John Admin'}</div>
                  <div className={styles.topbarUserRole}>{user?.role ?? 'Admin'}</div>
                </div>
                <ChevronDown size={14} className={styles.chevronDown} />
              </div>

              {showProfileMenu && (
                <>
                  <div
                    className={styles.dropdownOverlay}
                    onClick={() => setShowProfileMenu(false)}
                  />
                  <div className={styles.dropdownMenu}>
                    <button
                      className={styles.dropdownItem}
                      onClick={() => {
                        setShowProfileMenu(false)
                        logout()
                        navigate('/login')
                      }}
                    >
                      <LogOut size={16} />
                      <span>Sign Out</span>
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>
        <main className={`${styles.content} app-content`}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
