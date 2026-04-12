import React, { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../App'
import {
  LayoutDashboard, Package, ShoppingCart, Users, Receipt,
  CreditCard, BarChart3, Settings, LogOut, Bell, Search,
  ChevronLeft, Menu, Shield, FileCheck, MessageSquare, UserPlus, Building2
} from 'lucide-react'
import styles from './AppShell.module.css'

const navItems = [
  { to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/inventory',     icon: Package,         label: 'Inventory' },
  { to: '/orders',        icon: ShoppingCart,    label: 'Orders' },
  { to: '/customers',     icon: Users,           label: 'Customers' },
  { to: '/expenses',      icon: Receipt,         label: 'Expenses' },
  { to: '/payments',      icon: CreditCard,      label: 'Mpesa Payments' },
  { to: '/card-payments', icon: Shield,          label: 'Card / CyberSource' },
  { to: '/tax',           icon: Receipt,          label: 'Tax' },
  { to: '/kra',           icon: FileCheck,        label: 'KRA iTax' },
  { to: '/social',        icon: MessageSquare,    label: 'Social Inbox' },
  { to: '/users',         icon: UserPlus,         label: 'User Creation' },
  { to: '/business',      icon: Building2,        label: 'Business' },
  { to: '/reports',       icon: BarChart3,        label: 'Reports' },
]

export default function AppShell() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [search, setSearch] = useState('')

  return (
    <div className={styles.shell}>
      {/* ── Sidebar ── */}
      <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
        <div className={styles.sidebarTop}>
          <div className={styles.logo}>
            <div className={styles.logoIcon}>B360</div>
            {!collapsed && (
              <div>
                <div className={styles.logoName}>Biashara360</div>
                <div className={styles.logoSub}>Business Management</div>
              </div>
            )}
          </div>
          <button className={styles.collapseBtn} onClick={() => setCollapsed(c => !c)}>
            {collapsed ? <Menu size={16} /> : <ChevronLeft size={16} />}
          </button>
        </div>

        <nav className={styles.nav}>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to} to={to}
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              title={collapsed ? label : undefined}
            >
              <Icon size={18} />
              {!collapsed && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className={styles.sidebarBottom}>
          <NavLink to="/settings" className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}>
            <Settings size={18} />
            {!collapsed && <span>Settings</span>}
          </NavLink>
          <button className={styles.navItem} onClick={() => { logout(); navigate('/login') }}>
            <LogOut size={18} />
            {!collapsed && <span>Sign Out</span>}
          </button>
          {!collapsed && (
            <div className={styles.userCard}>
              <div className={styles.avatar}>{user?.name?.[0]?.toUpperCase() ?? 'U'}</div>
              <div>
                <div className={styles.userName}>{user?.name ?? 'User'}</div>
                <div className={styles.userRole}>{user?.role ?? ''}</div>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ── Main ── */}
      <div className={styles.main}>
        <header className={styles.topbar}>
          <div className={styles.searchWrap}>
            <Search size={15} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Search products, orders, customers..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className={styles.topbarRight}>
            <button className={styles.iconBtn}>
              <Bell size={18} />
              <span className={styles.notifDot} />
            </button>
          </div>
        </header>
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
