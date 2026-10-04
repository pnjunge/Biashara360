import React, { useEffect, useState } from 'react'
import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../App'
import {
  LayoutDashboard, Package, ShoppingCart, Users, Receipt,
  CreditCard, BarChart3, Settings, LogOut, Search,
  ChevronLeft, ChevronRight, ChevronDown, Menu, MessageSquare, UserPlus, Building2, Store, ShoppingBag, Download, ChefHat, CalendarClock, ScrollText, Check, Plus
} from 'lucide-react'
import styles from './AppShell.module.css'
import PortalOrdersInbox from '../orders/PortalOrdersInbox'
import OrderSoundAlerts from '../alerts/OrderSoundAlerts'
import { accessApi, hospitalityApi, servicesApi, branchApi, BranchResponse } from '../../services/api'

const navItems = [
  { key:'DASHBOARD', to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { key:'POS', to: '/pos',           icon: Store,           label: 'Point of Sale' },
  { key:'HOSPITALITY', to: '/hospitality', icon: Receipt, label: 'Bar & Restaurant' },
  { key:'HOTEL', to: '/hotel', icon: Building2, label: 'Hotel & Accommodation' },
  { key:'HOSPITALITY_OPS', to: '/hospitality-operations', icon: Building2, label: 'Hospitality Operations' },
  { key:'OPEN_TABS', to: '/open-tabs', icon: ShoppingCart, label: 'Open Tabs' },
  { key:'HOSPITALITY', to: '/kitchen-display', icon: ChefHat, label: 'Kitchen Display' },
  { key:'SERVICES', to: '/services', icon: CalendarClock, label: 'Appointments & Services' },
  { key:'INVENTORY', to: '/inventory',     icon: Package,         label: 'Inventory' },
  { key:'PURCHASES', to: '/purchases',     icon: ShoppingBag,     label: 'Purchases' },
  { key:'ORDERS', to: '/orders',        icon: ShoppingCart,    label: 'Orders' },
  { key:'CUSTOMERS', to: '/customers',     icon: Users,           label: 'Customers' },
  { key:'EXPENSES', to: '/expenses',      icon: Receipt,         label: 'Expenses' },
  { key:'PAYMENTS', to: '/payments',      icon: CreditCard,      label: 'Payments' },
  { key:'SOCIAL', to: '/social',        icon: MessageSquare,    label: 'Social Inbox' },
  { key:'USERS', to: '/users',         icon: UserPlus,         label: 'Users & Access' },
  { key:'AUDIT_LOG', to: '/audit-logs',    icon: ScrollText,       label: 'Audit Log' },
  { key:'REPORTS', to: '/reports',       icon: BarChart3,        label: 'Reports' },
  { key:'DOWNLOADS', to: '/downloads',     icon: Download,         label: 'Download Apps' },
  { key:'SETTINGS', to: '/settings',     icon: Settings,         label: 'Settings' },
]

const navSectionDefinitions = [
  { key: 'OPERATIONS', label: 'OPERATIONS', itemKeys: ['HOTEL', 'HOSPITALITY', 'HOSPITALITY_OPS', 'OPEN_TABS', 'SERVICES', 'INVENTORY', 'PURCHASES', 'ORDERS', 'CUSTOMERS'] },
  { key: 'FINANCE', label: 'FINANCE', itemKeys: ['EXPENSES', 'PAYMENTS'] },
  { key: 'ENGAGEMENT', label: 'ENGAGEMENT', itemKeys: ['SOCIAL', 'REPORTS', 'DOWNLOADS'] },
  { key: 'ADMINISTRATION', label: 'ADMINISTRATION', itemKeys: ['USERS', 'AUDIT_LOG', 'SETTINGS'] },
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
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [selectedBranch, setSelectedBranch] = useState<BranchResponse | null>(null)
  const [showBranchMenu, setShowBranchMenu] = useState(false)
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
      const enabled = (event as CustomEvent<{ enabled: boolean }>).detail?.enabled
      if (typeof enabled === 'boolean') setServicesEnabled(enabled)
      accessApi.me().then(result => {
        if (result.success && result.data) setAllowedMenus(new Set(result.data.enabledMenus))
      }).catch(() => {})
    }
    window.addEventListener('services-mode-changed', handleServicesChange)
    window.addEventListener('access-updated', handleServicesChange)
    window.addEventListener('hospitality-mode-changed', handleModeChange)
    return () => { window.removeEventListener('hospitality-mode-changed', handleModeChange); window.removeEventListener('services-mode-changed', handleServicesChange); window.removeEventListener('access-updated', handleServicesChange) }
  }, [user?.id])

  useEffect(() => {
    if (!user?.businessId) return
    const loadBranches = () => {
      branchApi.getAll().then(res => {
        if (res.success && res.data && res.data.length > 0) {
          setBranches(res.data)
          const savedBranchId = localStorage.getItem('selectedBranchId')
          const found = res.data.find(b => b.id === savedBranchId) || res.data.find(b => b.isHeadOffice) || res.data[0]
          if (found) {
            setSelectedBranch(found)
            localStorage.setItem('selectedBranchId', found.id)
            localStorage.setItem('selectedBranchName', found.name)
          }
        }
      }).catch(() => {})
    }
    loadBranches()
    const handleBranchRefresh = () => loadBranches()
    window.addEventListener('branch-updated', handleBranchRefresh)
    return () => window.removeEventListener('branch-updated', handleBranchRefresh)
  }, [user?.businessId])

  const handleBranchSelect = (branch: BranchResponse) => {
    setSelectedBranch(branch)
    localStorage.setItem('selectedBranchId', branch.id)
    localStorage.setItem('selectedBranchName', branch.name)
    setShowBranchMenu(false)
    window.dispatchEvent(new CustomEvent('branch-changed', { detail: branch }))
  }
  const isStaff = (user?.role || '').toUpperCase() === 'STAFF'
  const visibleNavItems = navItems.filter(item => {
    if (item.key === 'SERVICES' && !servicesEnabled) return false
    const accessKeys = [item.key]
    if (allowedMenus && !accessKeys.some(key => allowedMenus.has(key) || (key === 'PAYMENTS' && allowedMenus.has('CARD_PAYMENTS')))) return false
    const isHospitalityNav = item.key === 'HOSPITALITY' || item.key === 'HOSPITALITY_OPS' || item.key === 'OPEN_TABS' || item.to === '/kitchen-display'
    if (isHospitalityNav && hospitalityEnabled !== true) return false
    if (!isStaff) return true
    if (item.key === 'AUDIT_LOG') return allowedMenus?.has('AUDIT_LOG') ?? false
    if (item.key === 'USERS') return allowedMenus?.has('USERS') ?? false
    return item.to !== '/users' && item.to !== '/settings' && item.to !== '/business' && item.to !== '/cybersource-settings' && item.to !== '/audit-logs'
  })

  const isSuperAdmin = (user?.role || '').toUpperCase() === 'SUPERADMIN'
  const superAdminNavItems = [
    { key: 'BUSINESSES', to: '/business', icon: Building2, label: 'Tenants & Platform' },
    { key: 'USERS', to: '/users', icon: UserPlus, label: 'Platform Users' },
    { key: 'AUDIT_LOG', to: '/audit-logs', icon: ScrollText, label: 'Platform Audit Log' },
    { key: 'DOWNLOADS', to: '/downloads', icon: Download, label: 'App Releases' },
    { key: 'SETTINGS', to: '/settings', icon: Settings, label: 'System Settings' },
  ]

  const visibleTopNavItems = visibleNavItems.filter(item => item.key === 'DASHBOARD' || item.key === 'POS')
  const visibleNavSections = navSectionDefinitions.map(section => ({
    ...section,
    items: section.itemKeys.flatMap(key => visibleNavItems.filter(item => item.key === key))
  })).filter(section => section.items.length > 0)

  const renderNavItem = (item: { to?: string; icon: any; label: string; key?: string }) => {
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
                <div className={styles.logoSub}>{isSuperAdmin ? 'Platform Management' : 'Business Management'}</div>
              </div>
            )}
          </div>
        </div>

        <nav className={styles.nav}>
          {isSuperAdmin ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {!collapsed && (
                <div style={{ padding: '8px 12px', fontSize: 11, fontWeight: 800, color: 'var(--b360-text-secondary)', letterSpacing: '0.05em' }}>
                  PLATFORM MANAGEMENT
                </div>
              )}
              {superAdminNavItems.map(renderNavItem)}
            </div>
          ) : (
            <>
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
            </>
          )}
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
              aria-label="Search orders, tables, menu items"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            <span className={styles.searchShortcut}>Ctrl + K</span>
          </div>

          <div className={styles.topbarRight}>
            {user?.businessId && branches.length > 0 && (
              <div style={{ position: 'relative' }}>
                <div
                  className={styles.branchSelector}
                  onClick={() => setShowBranchMenu(!showBranchMenu)}
                  title="Switch Branch Location"
                >
                  <Building2 size={15} style={{ color: 'var(--b360-green)' }} />
                  <span style={{ fontSize: 13, fontWeight: 600, maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {selectedBranch?.name || 'Main Branch'}
                  </span>
                  {selectedBranch?.isHeadOffice && (
                    <span className={styles.branchBadgeHq}>HQ</span>
                  )}
                  <ChevronDown size={13} style={{ color: '#64748B' }} />
                </div>

                {showBranchMenu && (
                  <>
                    <div className={styles.dropdownOverlay} onClick={() => setShowBranchMenu(false)} />
                    <div className={styles.branchDropdown}>
                      <div style={{ padding: '8px 12px 6px', borderBottom: '1px solid var(--b360-border)', marginBottom: 4 }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--b360-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                          Locations & Branches ({branches.length})
                        </div>
                      </div>
                      <div style={{ maxHeight: 240, overflowY: 'auto' }}>
                        {branches.map(b => {
                          const isCurrent = selectedBranch?.id === b.id
                          return (
                            <button
                              key={b.id}
                              type="button"
                              className={`${styles.branchItem} ${isCurrent ? styles.branchItemActive : ''}`}
                              onClick={() => handleBranchSelect(b)}
                            >
                              <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                  <span style={{ fontSize: 13, fontWeight: isCurrent ? 700 : 500, color: isCurrent ? 'var(--b360-green)' : 'var(--b360-text)' }}>
                                    {b.name}
                                  </span>
                                  {b.isHeadOffice && <span className={styles.branchBadgeHq}>HQ</span>}
                                </div>
                                <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>
                                  Code: {b.code}{b.city ? ` • ${b.city}` : (b.county ? ` • ${b.county}` : '')}
                                </span>
                              </div>
                              {isCurrent && <Check size={15} style={{ color: 'var(--b360-green)', flexShrink: 0 }} />}
                            </button>
                          )
                        })}
                      </div>
                      {!isStaff && (
                        <div style={{ borderTop: '1px solid var(--b360-border)', marginTop: 4, paddingTop: 4 }}>
                          <button
                            type="button"
                            className={styles.dropdownItem}
                            style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-green)' }}
                            onClick={() => {
                              setShowBranchMenu(false)
                              navigate('/settings?tab=branches')
                            }}
                          >
                            <Plus size={14} />
                            <span>Manage Branches</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </>
                )}
              </div>
            )}

            {user?.businessId && <PortalOrdersInbox key={`${user.businessId}:${user.id}`} />}
            {user?.businessId && <OrderSoundAlerts key={`${user.businessId}:${user.id}`} hospitalityEnabled={hospitalityEnabled === true} />}

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
