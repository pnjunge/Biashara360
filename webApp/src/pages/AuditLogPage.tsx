import React, { useEffect, useMemo, useState } from 'react'
import {
  Activity, Users, ShoppingCart, Receipt, Search, RefreshCw, Download, Printer,
  Eye, Filter, Calendar, ShieldCheck, ArrowUpDown, ChevronRight, Laptop
} from 'lucide-react'
import { PageHeader, Card, Btn, Modal, Input, Select, StatusBadge } from '../components/ui'
import { auditLogApi, AuditLogResponse, superAdminApi, BusinessResponse } from '../services/api'
import { useAuth } from '../App'

type ActionCategory = 'ALL' | 'USERS' | 'ORDERS' | 'INVENTORY' | 'FINANCE' | 'ACCESS'

export default function AuditLogPage() {
  const { user } = useAuth()
  const isSuperAdmin = user?.role === 'SUPERADMIN'

  // Superadmin business selection
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState('')

  // State
  const [logs, setLogs] = useState<AuditLogResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [selectedLog, setSelectedLog] = useState<AuditLogResponse | null>(null)

  // Filters
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState<ActionCategory>('ALL')
  const [datePreset, setDatePreset] = useState<'ALL' | 'TODAY' | '7D' | '30D' | 'CUSTOM'>('ALL')
  const [customStart, setCustomStart] = useState('')
  const [customEnd, setCustomEnd] = useState('')

  // Load businesses for superadmin
  useEffect(() => {
    if (!isSuperAdmin) return
    superAdminApi.listBusinesses().then(res => {
      if (res.success && res.data) {
        setBusinesses(res.data)
        if (res.data.length > 0 && !selectedBusinessId) {
          setSelectedBusinessId(res.data[0].id)
        }
      }
    }).catch(() => {})
  }, [isSuperAdmin])

  // Calculate ISO dates for date presets
  const getDateRange = () => {
    const now = new Date()
    if (datePreset === 'TODAY') {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate()).toISOString()
      const end = now.toISOString()
      return { startDate: start, endDate: end }
    } else if (datePreset === '7D') {
      const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
      return { startDate: start, endDate: now.toISOString() }
    } else if (datePreset === '30D') {
      const start = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString()
      return { startDate: start, endDate: now.toISOString() }
    } else if (datePreset === 'CUSTOM') {
      const start = customStart ? new Date(customStart).toISOString() : undefined
      const end = customEnd ? new Date(customEnd + 'T23:59:59.999Z').toISOString() : undefined
      return { startDate: start, endDate: end }
    }
    return { startDate: undefined, endDate: undefined }
  }

  // Fetch audit logs
  const fetchLogs = async () => {
    if (isSuperAdmin && !selectedBusinessId) return
    setLoading(true)
    setError('')
    try {
      const { startDate, endDate } = getDateRange()
      const res = await auditLogApi.list({
        limit: 300,
        businessId: isSuperAdmin ? selectedBusinessId : undefined,
        startDate,
        endDate,
      })
      if (res.success && res.data) {
        setLogs(res.data)
      } else {
        setError(res.message || 'Failed to load audit logs')
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Network error fetching audit logs')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLogs()
  }, [selectedBusinessId, datePreset, customStart, customEnd])

  // Auto-refresh interval
  useEffect(() => {
    if (!autoRefresh) return
    const interval = setInterval(fetchLogs, 15000)
    return () => clearInterval(interval)
  }, [autoRefresh, selectedBusinessId, datePreset, customStart, customEnd])

  // Filter categorization
  const isMatchCategory = (action: string, cat: ActionCategory) => {
    const act = action.toUpperCase()
    if (cat === 'ALL') return true
    if (cat === 'USERS') {
      return act.includes('USER') || act.includes('PIN') || act.includes('ROLE')
    }
    if (cat === 'ORDERS') {
      return act.includes('ORDER')
    }
    if (cat === 'INVENTORY') {
      return act.includes('PRODUCT') || act.includes('STOCK')
    }
    if (cat === 'FINANCE') {
      return act.includes('EXPENSE') || act.includes('PAYMENT') || act.includes('INVOICE')
    }
    if (cat === 'ACCESS') {
      return act.includes('ACCESS') || act.includes('GROUP') || act.includes('MENU')
    }
    return true
  }

  // Filtered logs
  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      if (!isMatchCategory(log.action, category)) return false
      if (search.trim()) {
        const q = search.toLowerCase()
        const text = [
          log.action,
          log.actorName,
          log.targetName,
          log.details,
          log.ipAddress,
        ].filter(Boolean).join(' ').toLowerCase()
        if (!text.includes(q)) return false
      }
      return true
    })
  }, [logs, category, search])

  // Metrics
  const metrics = useMemo(() => {
    const total = logs.length
    const userEvents = logs.filter(l => isMatchCategory(l.action, 'USERS')).length
    const orderEvents = logs.filter(l => isMatchCategory(l.action, 'ORDERS')).length
    const financeEvents = logs.filter(l => isMatchCategory(l.action, 'FINANCE') || isMatchCategory(l.action, 'INVENTORY')).length
    return { total, userEvents, orderEvents, financeEvents }
  }, [logs])

  // Export handlers
  const exportCsv = () => {
    if (filteredLogs.length === 0) return
    const headers = ['Timestamp', 'Action', 'Actor Name', 'Actor ID', 'Target Name', 'Target ID', 'IP Address', 'Details']
    const rows = filteredLogs.map(l => [
      `"${l.createdAt}"`,
      `"${l.action}"`,
      `"${(l.actorName || '').replace(/"/g, '""')}"`,
      `"${l.actorUserId || ''}"`,
      `"${(l.targetName || '').replace(/"/g, '""')}"`,
      `"${l.targetUserId || ''}"`,
      `"${l.ipAddress || ''}"`,
      `"${(l.details || '').replace(/"/g, '""')}"`,
    ])
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', `biashara_audit_logs_${new Date().toISOString().slice(0, 10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  const exportJson = () => {
    if (filteredLogs.length === 0) return
    const blob = new Blob([JSON.stringify(filteredLogs, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `biashara_audit_logs_${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  const getActionColor = (action: string) => {
    const act = action.toUpperCase()
    if (act.includes('CREATE') || act.includes('ACTIVATED') || act.includes('ENABLE')) {
      return { bg: '#ecfdf5', color: '#059669', border: '#a7f3d0' }
    }
    if (act.includes('DELETE') || act.includes('DEACTIVATED') || act.includes('CANCEL') || act.includes('VOID') || act.includes('REMOVE')) {
      return { bg: '#fef2f2', color: '#dc2626', border: '#fecaca' }
    }
    if (act.includes('REASSIGN') || act.includes('ROLE') || act.includes('GROUP') || act.includes('STOCK')) {
      return { bg: '#fffbeb', color: '#d97706', border: '#fde68a' }
    }
    return { bg: '#eff6ff', color: '#2563eb', border: '#bfdbfe' }
  }

  const formatRelativeTime = (isoString: string) => {
    const diffMs = Date.now() - new Date(isoString).getTime()
    const diffSec = Math.floor(diffMs / 1000)
    if (diffSec < 60) return `${diffSec}s ago`
    const diffMin = Math.floor(diffSec / 60)
    if (diffMin < 60) return `${diffMin}m ago`
    const diffHours = Math.floor(diffMin / 60)
    if (diffHours < 24) return `${diffHours}h ago`
    const diffDays = Math.floor(diffHours / 24)
    return `${diffDays}d ago`
  }

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* ── Page Header ── */}
      <PageHeader
        title="Audit Logs & Activity Trail"
        action={
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <label
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                fontSize: 12,
                cursor: 'pointer',
                padding: '6px 12px',
                background: autoRefresh ? 'rgba(16, 185, 129, 0.1)' : 'var(--b360-surface)',
                border: `1px solid ${autoRefresh ? 'var(--b360-green)' : 'var(--b360-border)'}`,
                borderRadius: 'var(--radius-sm)',
                fontWeight: 600,
                color: autoRefresh ? 'var(--b360-green)' : 'var(--b360-text-secondary)',
              }}
            >
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={e => setAutoRefresh(e.target.checked)}
                style={{ accentColor: 'var(--b360-green)' }}
              />
              <span style={{ display: 'inline-block', width: 6, height: 6, borderRadius: '50%', background: autoRefresh ? 'var(--b360-green)' : '#94a3b8' }} />
              Live Refresh
            </label>

            <Btn small variant="secondary" icon={<RefreshCw size={14} className={loading ? 'spin' : ''} />} onClick={fetchLogs}>
              Refresh
            </Btn>
            <Btn small variant="secondary" icon={<Download size={14} />} onClick={exportCsv}>
              Export CSV
            </Btn>
            <Btn small variant="secondary" icon={<Printer size={14} />} onClick={() => window.print()}>
              Print
            </Btn>
          </div>
        }
      />

      {/* Superadmin Business Picker */}
      {isSuperAdmin && businesses.length > 0 && (
        <Card style={{ padding: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>Viewing audit logs for:</div>
            <select
              value={selectedBusinessId}
              onChange={e => setSelectedBusinessId(e.target.value)}
              style={{
                padding: '6px 12px',
                borderRadius: 8,
                border: '1px solid var(--b360-border)',
                background: 'var(--b360-surface)',
                fontSize: 13,
                fontWeight: 600,
                color: 'var(--b360-text)',
                minWidth: 240,
              }}
            >
              {businesses.map(b => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.type})
                </option>
              ))}
            </select>
          </div>
        </Card>
      )}

      {/* ── Metric Summary Cards ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16 }}>
        <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: 18, border: '1px solid var(--b360-border)', boxShadow: 'var(--shadow-sm)', display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(59, 130, 246, 0.1)', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Activity size={22} />
          </div>
          <div>
            <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Total Activities</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--b360-text)' }}>{metrics.total}</div>
          </div>
        </div>

        <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: 18, border: '1px solid var(--b360-border)', boxShadow: 'var(--shadow-sm)', display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(147, 51, 234, 0.1)', color: '#9333ea', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Users size={22} />
          </div>
          <div>
            <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>User & Access Events</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--b360-text)' }}>{metrics.userEvents}</div>
          </div>
        </div>

        <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: 18, border: '1px solid var(--b360-border)', boxShadow: 'var(--shadow-sm)', display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(16, 185, 129, 0.1)', color: '#059669', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <ShoppingCart size={22} />
          </div>
          <div>
            <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Order Operations</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--b360-text)' }}>{metrics.orderEvents}</div>
          </div>
        </div>

        <div style={{ background: 'white', borderRadius: 'var(--radius-md)', padding: 18, border: '1px solid var(--b360-border)', boxShadow: 'var(--shadow-sm)', display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(245, 158, 11, 0.1)', color: '#d97706', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Receipt size={22} />
          </div>
          <div>
            <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Inventory & Finance</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--b360-text)' }}>{metrics.financeEvents}</div>
          </div>
        </div>
      </div>

      {/* ── Filter Toolbar ── */}
      <Card style={{ padding: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
            {/* Search Input */}
            <div style={{ position: 'relative', flex: '1 1 280px', maxWidth: 400 }}>
              <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--b360-text-secondary)' }} />
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search actor, target, details, IP, action…"
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 36px',
                  borderRadius: 8,
                  border: '1px solid var(--b360-border)',
                  background: 'var(--b360-surface)',
                  fontSize: 13,
                  color: 'var(--b360-text)',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            {/* Category Filter Pills */}
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {(['ALL', 'USERS', 'ORDERS', 'INVENTORY', 'FINANCE', 'ACCESS'] as ActionCategory[]).map(cat => {
                const isSelected = category === cat
                const labels: Record<ActionCategory, string> = {
                  ALL: 'All Events',
                  USERS: 'Users & Roles',
                  ORDERS: 'Orders & POS',
                  INVENTORY: 'Products & Stock',
                  FINANCE: 'Expenses & Payments',
                  ACCESS: 'Groups & Menus',
                }
                return (
                  <button
                    key={cat}
                    onClick={() => setCategory(cat)}
                    style={{
                      padding: '6px 12px',
                      borderRadius: 20,
                      fontSize: 12,
                      fontWeight: 600,
                      border: `1px solid ${isSelected ? 'var(--b360-blue)' : 'var(--b360-border)'}`,
                      background: isSelected ? 'var(--b360-blue)' : 'var(--b360-surface)',
                      color: isSelected ? 'white' : 'var(--b360-text-secondary)',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    {labels[cat]}
                  </button>
                )
              })}
            </div>

            {/* Date Preset Filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Calendar size={15} style={{ color: 'var(--b360-text-secondary)' }} />
              <select
                value={datePreset}
                onChange={e => setDatePreset(e.target.value as any)}
                style={{
                  padding: '7px 10px',
                  borderRadius: 8,
                  border: '1px solid var(--b360-border)',
                  background: 'var(--b360-surface)',
                  fontSize: 12,
                  fontWeight: 600,
                  color: 'var(--b360-text)',
                }}
              >
                <option value="ALL">All Time</option>
                <option value="TODAY">Today</option>
                <option value="7D">Past 7 Days</option>
                <option value="30D">Past 30 Days</option>
                <option value="CUSTOM">Custom Range</option>
              </select>
            </div>
          </div>

          {/* Custom Date Pickers */}
          {datePreset === 'CUSTOM' && (
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', paddingTop: 10, borderTop: '1px solid var(--b360-border)', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>From:</span>
                <input
                  type="date"
                  value={customStart}
                  onChange={e => setCustomStart(e.target.value)}
                  style={{ padding: '6px 10px', borderRadius: 6, border: '1px solid var(--b360-border)', fontSize: 12 }}
                />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--b360-text-secondary)' }}>To:</span>
                <input
                  type="date"
                  value={customEnd}
                  onChange={e => setCustomEnd(e.target.value)}
                  style={{ padding: '6px 10px', borderRadius: 6, border: '1px solid var(--b360-border)', fontSize: 12 }}
                />
              </div>
              {(customStart || customEnd) && (
                <button
                  onClick={() => { setCustomStart(''); setCustomEnd('') }}
                  style={{ background: 'none', border: 'none', color: 'var(--b360-red)', fontSize: 12, cursor: 'pointer', textDecoration: 'underline' }}
                >
                  Clear dates
                </button>
              )}
            </div>
          )}

          <div style={{ fontSize: 12, color: 'var(--b360-text-secondary)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Showing <strong>{filteredLogs.length}</strong> of {logs.length} logged events</span>
            {(search || category !== 'ALL' || datePreset !== 'ALL') && (
              <button
                onClick={() => { setSearch(''); setCategory('ALL'); setDatePreset('ALL') }}
                style={{ background: 'none', border: 'none', color: 'var(--b360-blue)', fontSize: 12, cursor: 'pointer', fontWeight: 600 }}
              >
                Reset all filters
              </button>
            )}
          </div>
        </div>
      </Card>

      {/* ── Table Card ── */}
      <Card style={{ padding: 0, overflow: 'hidden' }}>
        {error && (
          <div style={{ padding: 16, background: '#fef2f2', borderBottom: '1px solid #fecaca', color: '#dc2626', fontSize: 13 }}>
            {error}
          </div>
        )}

        {loading ? (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--b360-text-secondary)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <RefreshCw size={24} className="spin" style={{ color: 'var(--b360-blue)' }} />
            <span>Loading audit log entries…</span>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--b360-text-secondary)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
            <ShieldCheck size={36} style={{ color: '#cbd5e1' }} />
            <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--b360-text)' }}>No activities found</div>
            <div style={{ fontSize: 13 }}>No audit records match the current filter criteria or date range.</div>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13, textAlign: 'left' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid var(--b360-border)', color: 'var(--b360-text-secondary)', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  <th style={{ padding: '12px 16px' }}>Timestamp</th>
                  <th style={{ padding: '12px 16px' }}>Actor</th>
                  <th style={{ padding: '12px 16px' }}>Action</th>
                  <th style={{ padding: '12px 16px' }}>Target</th>
                  <th style={{ padding: '12px 16px' }}>Details / Activity</th>
                  <th style={{ padding: '12px 16px' }}>IP / Device</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Inspect</th>
                </tr>
              </thead>
              <tbody>
                {filteredLogs.map(log => {
                  const actionStyle = getActionColor(log.action)
                  const dateObj = new Date(log.createdAt)
                  const formattedDate = dateObj.toLocaleDateString('en-KE', { month: 'short', day: 'numeric', year: 'numeric' })
                  const formattedTime = dateObj.toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
                  const relativeTime = formatRelativeTime(log.createdAt)

                  return (
                    <tr
                      key={log.id}
                      onClick={() => setSelectedLog(log)}
                      style={{
                        borderBottom: '1px solid var(--b360-border)',
                        cursor: 'pointer',
                        transition: 'background 0.15s ease',
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = '#f8fafc')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      {/* Timestamp */}
                      <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>
                        <div style={{ fontWeight: 600, color: 'var(--b360-text)' }}>{formattedDate} {formattedTime}</div>
                        <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{relativeTime}</div>
                      </td>

                      {/* Actor */}
                      <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <div style={{ width: 26, height: 26, borderRadius: '50%', background: '#e2e8f0', color: '#475569', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 11 }}>
                            {(log.actorName || 'S').slice(0, 1).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, color: 'var(--b360-text)' }}>{log.actorName || 'System Service'}</div>
                            {log.actorUserId && <div style={{ fontSize: 10, color: 'var(--b360-text-secondary)' }}>ID: {log.actorUserId.slice(0, 8)}…</div>}
                          </div>
                        </div>
                      </td>

                      {/* Action Pill */}
                      <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>
                        <span
                          style={{
                            display: 'inline-block',
                            padding: '4px 10px',
                            borderRadius: 14,
                            fontSize: 11,
                            fontWeight: 700,
                            letterSpacing: '0.2px',
                            background: actionStyle.bg,
                            color: actionStyle.color,
                            border: `1px solid ${actionStyle.border}`,
                          }}
                        >
                          {log.action.replace(/_/g, ' ')}
                        </span>
                      </td>

                      {/* Target */}
                      <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>
                        {log.targetName ? (
                          <div style={{ fontWeight: 600, color: 'var(--b360-text)' }}>{log.targetName}</div>
                        ) : log.targetUserId ? (
                          <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Target: {log.targetUserId.slice(0, 8)}…</div>
                        ) : (
                          <span style={{ color: 'var(--b360-text-secondary)' }}>—</span>
                        )}
                      </td>

                      {/* Details / Activity */}
                      <td style={{ padding: '12px 16px', maxWidth: 360 }}>
                        <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--b360-text)' }} title={log.details || ''}>
                          {log.details || '—'}
                        </div>
                      </td>

                      {/* IP / Host */}
                      <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>
                        {log.ipAddress ? (
                          <span style={{ fontSize: 11, fontFamily: 'monospace', color: 'var(--b360-text-secondary)', background: '#f1f5f9', padding: '2px 6px', borderRadius: 4 }}>
                            {log.ipAddress}
                          </span>
                        ) : (
                          <span style={{ color: 'var(--b360-text-secondary)' }}>—</span>
                        )}
                      </td>

                      {/* Inspect Button */}
                      <td style={{ padding: '12px 16px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <button
                          onClick={e => {
                            e.stopPropagation()
                            setSelectedLog(log)
                          }}
                          style={{
                            background: 'transparent',
                            border: '1px solid var(--b360-border)',
                            borderRadius: 6,
                            padding: '4px 8px',
                            fontSize: 12,
                            color: 'var(--b360-blue)',
                            cursor: 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 4,
                          }}
                        >
                          <Eye size={13} />
                          Details
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* ── Inspection Modal ── */}
      {selectedLog && (
        <Modal
          title={`Audit Record · ${selectedLog.action.replace(/_/g, ' ')}`}
          onClose={() => setSelectedLog(null)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
              <Btn variant="secondary" onClick={() => setSelectedLog(null)}>
                Close
              </Btn>
            </div>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Action Header Banner */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '14px 16px',
                borderRadius: 8,
                background: getActionColor(selectedLog.action).bg,
                border: `1px solid ${getActionColor(selectedLog.action).border}`,
              }}
            >
              <div>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', color: getActionColor(selectedLog.action).color }}>
                  Action Event
                </div>
                <div style={{ fontSize: 16, fontWeight: 800, color: getActionColor(selectedLog.action).color }}>
                  {selectedLog.action.replace(/_/g, ' ')}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Event ID</div>
                <div style={{ fontSize: 11, fontFamily: 'monospace' }}>{selectedLog.id}</div>
              </div>
            </div>

            {/* Field Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, background: 'var(--b360-surface)', padding: 14, borderRadius: 8, border: '1px solid var(--b360-border)' }}>
              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Actor (Initiator)</div>
                <div style={{ fontSize: 13, fontWeight: 700 }}>{selectedLog.actorName || 'System'}</div>
                {selectedLog.actorUserId && <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontFamily: 'monospace' }}>ID: {selectedLog.actorUserId}</div>}
              </div>

              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Target Entity</div>
                <div style={{ fontSize: 13, fontWeight: 700 }}>{selectedLog.targetName || '—'}</div>
                {selectedLog.targetUserId && <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontFamily: 'monospace' }}>ID: {selectedLog.targetUserId}</div>}
              </div>

              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Timestamp</div>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{new Date(selectedLog.createdAt).toLocaleString('en-KE')}</div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>{formatRelativeTime(selectedLog.createdAt)}</div>
              </div>

              <div>
                <div style={{ fontSize: 11, color: 'var(--b360-text-secondary)', fontWeight: 600 }}>Client IP Address</div>
                <div style={{ fontSize: 13, fontFamily: 'monospace' }}>{selectedLog.ipAddress || '—'}</div>
              </div>
            </div>

            {/* Description & Payload Details */}
            <div>
              <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 6, color: 'var(--b360-text)' }}>Activity Details & Notes</div>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid var(--b360-border)',
                  borderRadius: 8,
                  padding: '12px 14px',
                  fontSize: 13,
                  lineHeight: 1.5,
                  color: 'var(--b360-text)',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                }}
              >
                {selectedLog.details || 'No additional details logged for this activity.'}
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
