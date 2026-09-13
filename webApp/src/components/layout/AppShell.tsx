import React, { useEffect, useState } from 'react'
import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../App'
import {
  LayoutDashboard, Package, ShoppingCart, Users, Receipt,
  CreditCard, BarChart3, Settings, LogOut, Bell, Search,
  ChevronLeft, ChevronRight, ChevronDown, Menu, MessageSquare, UserPlus, Building2, Store, ShoppingBag, Download, ChefHat, CalendarClock
} from 'lucide-react'
import styles from './AppShell.module.css'
import PortalOrdersInbox from '../orders/PortalOrdersInbox'
import { accessApi, hospitalityApi, servicesApi } from '../../services/api'

const navItems = [
  { key:'DASHBOARD', to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { key:'POS', to: '/pos',           icon: Store,           label: 'Point of Sale' },
  { key:'HOSPITALITY', to: '/hospitality', icon: Receipt, label: 'Bar & Restaurant' },
  { key:'HOSPITALITY_OPS', to: '/hospitality-operations', icon: Building2, label: 'Hospitality Operations' },
  { key:'OPEN_TABS', to: '/open-tabs', icon: ShoppingCart, label: 'Open Tabs' },
  { key:'HOSPITALITY', to: '/kitchen-display', icon: ChefHat, label: 'Kitchen & Bar Display' },
  { key:'SERVICES', to: '/services', icon: CalendarClock, label: 'Appointments & Services' },
  { key:'INVENTORY', to: '/inventory',     icon: Package,         label: 'Inventory' },
  { key:'ORDERS', to: '/orders',        icon: ShoppingCart,    label: 'Orders' },
  { key:'CUSTOMERS', to: '/customers',     icon: Users,           label: 'Customers' },
  { key:'EXPENSES', to: '/expenses',      icon: Receipt,         label: 'Expenses' },
  { key:'PAYMENTS', to: '/payments',      icon: CreditCard,      label: 'Payments' },
  { key:'SOCIAL', to: '/social',        icon: MessageSquare,    label: 'Social Inbox' },
  { key:'USERS', to: '/users',         icon: UserPlus,         label: 'Users & Access' },
  { key:'REPORTS', to: '/reports',       icon: BarChart3,        label: 'Reports' },
  { key:'DOWNLOADS', to: '/downloads',     icon: Download,         label: 'Download Apps' },
  { key:'SETTINGS', to: '/settings',     icon: Settings,         label: 'Settings' },
]

const navSectionDefinitions = [
  { key: 'OPERATIONS', label: 'OPERATIONS', itemKeys: ['HOSPITALITY', 'HOSPITALITY_OPS', 'OPEN_TABS', 'SERVICES', 'INVENTORY', 'ORDERS', 'CUSTOMERS'] },
  { key: 'FINANCE', label: 'FINANCE', itemKeys: ['EXPENSES', 'PAYMENTS'] },
  { key: 'ENGAGEMENT', label: 'ENGAGEMENT', itemKeys: ['SOCIAL', 'REPORTS', 'DOWNLOADS'] },
  { key: 'ADMINISTRATION', label: 'ADMINISTRATION', itemKeys: ['USERS', 'SETTINGS'] },
]

export default function AppShell() {
  const { logout, user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [showProfileMenu, setShowProfileMenu] = useState(false)
  const [allowedMenus, setAllowedMenus] = useState<Set<string> | null>(null)
  const [servicesEnabled, setServicesEnabled] = useState(false)
  const [hospitalityEnabled, setHospitalityEnabled] = useState<boolean | null>(null)
  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    OPERATIONS: true,
    FINANCE: true,
    ENGAGEMENT: true,
    ADMINISTRATION: true,
  })
  useEffect(() => {
    accessApi.me().then(result => {
      if (result.success && result.data) setAllowedMenus(new Set(result.data.enabledMenus))
    }).catch(() => setAllowedMenus(null))
    servicesApi.status().then(result => setServicesEnabled(result.success && result.data?.enabled === true)).catch(() => setServicesEnabled(false))
    hospitalityApi.status().then(result => {
      if (result.success && result.data) setHospitalityEnabled(result.data.enabled)
    }).catch(() => setHospitalityEnabled(null))
    const handleModeChange = (event: Event) => {
      const enabled = (event as CustomEvent<{ enabled: boolean }>).detail?.enabled
      if (typeof enabled === 'boolean') setHospitalityEnabled(enabled)
    }
    const handleServicesChange = (event: Event) => {
      setServicesEnabled((event as CustomEvent<{ enabled: boolean }>).detail?.enabled === true)
      accessApi.me().then(result => {
        if (result.success && result.data) setAllowedMenus(new Set(result.data.enabledMenus))
      }).catch(() => {})
    }
    window.addEventListener('services-mode-changed', handleServicesChange)
    window.addEventListener('hospitality-mode-changed', handleModeChange)
    return () => { window.removeEventListener('hospitality-mode-changed', handleModeChange); window.removeEventListener('services-mode-changed', handleServicesChange) }
  }, [user?.id])
  const isStaff = (user?.role || '').toUpperCase() === 'STAFF'
  const visibleNavItems = navItems.filter(item => {
    if (item.key === 'SERVICES' && !servicesEnabled) return false
    const accessKeys = [item.key]
    if (allowedMenus && !accessKeys.some(key => allowedMenus.has(key) || (key === 'PAYMENTS' && allowedMenus.has('CARD_PAYMENTS')))) return false
    const isHospitalityNav = item.key === 'HOSPITALITY' || item.key === 'HOSPITALITY_OPS' || item.key === 'OPEN_TABS' || item.to === '/kitchen-display'
    if (isHospitalityNav && hospitalityEnabled !== true) return false
    if (!isStaff) return true
    return item.to !== '/users' && item.to !== '/settings' && item.to !== '/business' && item.to !== '/cybersource-settings'
  })

  const visibleTopNavItems = visibleNavItems.filter(item => item.key === 'DASHBOARD' || item.key === 'POS')
  const visibleNavSections = navSectionDefinitions.map(section => ({
    ...section,
    items: section.itemKeys.flatMap(key => visibleNavItems.filter(item => item.key === key))
  })).filter(section => section.items.length > 0)

  const renderNavItem = (item: typeof navItems[number]) => {
    const Icon = item.icon
    return (
      <NavLink key={item.to} to={item.to!} className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`} title={collapsed ? item.label : undefined} onClick={() => setMobileOpen(false)}>
        <Icon size={18} className={styles.navIcon} />
        {!collapsed && <span>{item.label}</span>}
      </NavLink>
    )
  }

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
          {visibleTopNavItems.map(renderNavItem)}
          {visibleNavSections.map(section => (
            <div key={section.key} className={styles.navSection}>
              {!collapsed && (
                <button
                  type="button"
                  className={styles.navSectionHeader}
                  onClick={() => setOpenSections(current => ({ ...current, [section.key]: !current[section.key] }))}
                  aria-expanded={openSections[section.key]}
                >
                  <span>{section.label}</span>
                  <ChevronDown size={14} className={`${styles.groupChevron} ${openSections[section.key] ? styles.groupChevronOpen : ''}`} />
                </button>
              )}
              {(collapsed || openSections[section.key]) && section.items.map(renderNavItem)}
            </div>
          ))}
        </nav>

        <div className={styles.sidebarBottom}>
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
              placeholder="Search orders, tables, menu items..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            <span className={styles.searchShortcut}>Ctrl + K</span>
          </div>

          <div className={styles.topbarRight}>
            {user?.businessId && <PortalOrdersInbox key={`${user.businessId}:${user.id}`} />}
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
