import React, { useState, useEffect } from 'react'
import {
  Clock,
  Mail,
  MessageSquare,
  Send,
  CheckCircle,
  AlertTriangle,
  Trash2,
  Edit3,
  Plus,
  Play,
  Pause,
  Calendar,
  RefreshCw,
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  Check,
  X
} from 'lucide-react'
import { Modal, Btn, Card, StatusBadge, Input, Select } from '../ui'
import {
  reportScheduleApi,
  branchApi,
  ReportScheduleResponse,
  ReportScheduleLogResponse,
  CreateReportScheduleRequest,
  UpdateReportScheduleRequest,
  BranchResponse
} from '../../services/api'

interface ReportSchedulerModalProps {
  onClose: () => void
  businessName?: string
}

const REPORT_TYPES = [
  { value: 'SALES_SUMMARY', label: '📊 Sales Summary (End-of-day sales, paid orders & payment methods)' },
  { value: 'PAYMENTS', label: '💳 Payments & Collections (M-Pesa, Cash, Card, Reconciled amounts)' },
  { value: 'PROFIT_LOSS', label: '📈 Profit & Loss (Revenue, COGS, Gross Profit, Expenses, Net Profit)' },
  { value: 'LOW_STOCK', label: '⚠️ Low Stock & Reorder Alert (Products requiring immediate reorder)' },
]

const FREQUENCIES = [
  { value: 'DAILY', label: 'Daily (Every day at set time)' },
  { value: 'WEEKLY', label: 'Weekly (Once per week on specific day)' },
  { value: 'MONTHLY', label: 'Monthly (Once per month on specific day)' },
]

const DAYS_OF_WEEK = [
  { value: '1', label: 'Monday' },
  { value: '2', label: 'Tuesday' },
  { value: '3', label: 'Wednesday' },
  { value: '4', label: 'Thursday' },
  { value: '5', label: 'Friday' },
  { value: '6', label: 'Saturday' },
  { value: '7', label: 'Sunday' },
]

const CHANNELS = [
  { value: 'EMAIL,WHATSAPP', label: '✉️ Email & 💬 WhatsApp (Recommended)' },
  { value: 'EMAIL', label: '✉️ Email Only (Branded Executive HTML Report)' },
  { value: 'WHATSAPP', label: '💬 WhatsApp Only (Formatted KPI Text Message)' },
]

export function ReportSchedulerModal({ onClose, businessName = 'Biashara360 Merchant' }: ReportSchedulerModalProps) {
  const [activeTab, setActiveTab] = useState<'schedules' | 'form' | 'logs'>('schedules')
  const [schedules, setSchedules] = useState<ReportScheduleResponse[]>([])
  const [logs, setLogs] = useState<ReportScheduleLogResponse[]>([])
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [executingId, setExecutingId] = useState<string | null>(null)
  const [notice, setNotice] = useState<{ type: 'success' | 'error' | 'info'; message: string; waUrls?: string[] } | null>(null)

  // Edit / Form state
  const [editingScheduleId, setEditingScheduleId] = useState<string | null>(null)
  const [formData, setFormData] = useState<CreateReportScheduleRequest>({
    name: 'Daily Closing Sales Report',
    reportType: 'SALES_SUMMARY',
    frequency: 'DAILY',
    timeOfDay: '20:00',
    dayOfWeek: 5,
    dayOfMonth: 1,
    channels: 'EMAIL,WHATSAPP',
    emailRecipients: '',
    whatsappRecipients: '',
    branchId: null,
    isActive: true,
  })

  // Selected schedule for log inspection
  const [selectedScheduleForLogs, setSelectedScheduleForLogs] = useState<string | null>(null)
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null)

  const loadData = async () => {
    setLoading(true)
    try {
      const [schedRes, branchRes] = await Promise.allSettled([
        reportScheduleApi.list(),
        branchApi.getAll(),
      ])
      if (schedRes.status === 'fulfilled' && schedRes.value.success && schedRes.value.data) {
        setSchedules(schedRes.value.data)
      }
      if (branchRes.status === 'fulfilled' && branchRes.value.success && branchRes.value.data) {
        setBranches(branchRes.value.data)
      }
    } finally {
      setLoading(false)
    }
  }

  const loadLogs = async (schedId?: string) => {
    try {
      const res = await reportScheduleApi.getLogs(schedId || undefined)
      if (res.success && res.data) {
        setLogs(res.data)
      }
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  useEffect(() => {
    if (activeTab === 'logs') {
      loadLogs(selectedScheduleForLogs || undefined)
    }
  }, [activeTab, selectedScheduleForLogs])

  const handleOpenCreate = () => {
    setEditingScheduleId(null)
    setFormData({
      name: 'Daily Closing Sales Report',
      reportType: 'SALES_SUMMARY',
      frequency: 'DAILY',
      timeOfDay: '20:00',
      dayOfWeek: 5,
      dayOfMonth: 1,
      channels: 'EMAIL,WHATSAPP',
      emailRecipients: '',
      whatsappRecipients: '',
      branchId: null,
      isActive: true,
    })
    setNotice(null)
    setActiveTab('form')
  }

  const handleOpenEdit = (sched: ReportScheduleResponse) => {
    setEditingScheduleId(sched.id)
    setFormData({
      name: sched.name,
      reportType: sched.reportType,
      frequency: sched.frequency,
      timeOfDay: sched.timeOfDay,
      dayOfWeek: sched.dayOfWeek ?? 5,
      dayOfMonth: sched.dayOfMonth ?? 1,
      channels: sched.channels as any,
      emailRecipients: sched.emailRecipients || '',
      whatsappRecipients: sched.whatsappRecipients || '',
      branchId: sched.branchId || null,
      isActive: sched.isActive,
    })
    setNotice(null)
    setActiveTab('form')
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setNotice(null)
    try {
      if (editingScheduleId) {
        const updatePayload: UpdateReportScheduleRequest = {
          name: formData.name,
          reportType: formData.reportType,
          frequency: formData.frequency,
          timeOfDay: formData.timeOfDay,
          dayOfWeek: formData.frequency === 'WEEKLY' ? Number(formData.dayOfWeek) : null,
          dayOfMonth: formData.frequency === 'MONTHLY' ? Number(formData.dayOfMonth) : null,
          channels: formData.channels,
          emailRecipients: formData.emailRecipients?.trim() || null,
          whatsappRecipients: formData.whatsappRecipients?.trim() || null,
          branchId: formData.branchId || null,
          isActive: formData.isActive,
        }
        const res = await reportScheduleApi.update(editingScheduleId, updatePayload)
        if (res.success) {
          setNotice({ type: 'success', message: 'Report schedule updated successfully!' })
          await loadData()
          setActiveTab('schedules')
        } else {
          setNotice({ type: 'error', message: res.message || 'Failed to update schedule' })
        }
      } else {
        const createPayload: CreateReportScheduleRequest = {
          ...formData,
          dayOfWeek: formData.frequency === 'WEEKLY' ? Number(formData.dayOfWeek) : null,
          dayOfMonth: formData.frequency === 'MONTHLY' ? Number(formData.dayOfMonth) : null,
          emailRecipients: formData.emailRecipients?.trim() || null,
          whatsappRecipients: formData.whatsappRecipients?.trim() || null,
          branchId: formData.branchId || null,
        }
        const res = await reportScheduleApi.create(createPayload)
        if (res.success) {
          setNotice({ type: 'success', message: 'New report schedule created successfully!' })
          await loadData()
          setActiveTab('schedules')
        } else {
          setNotice({ type: 'error', message: res.message || 'Failed to create schedule' })
        }
      }
    } catch (err: any) {
      setNotice({ type: 'error', message: err.response?.data?.message || err.message || 'An error occurred' })
    } finally {
      setSaving(false)
    }
  }

  const handleToggleActive = async (sched: ReportScheduleResponse) => {
    try {
      const res = await reportScheduleApi.toggleActive(sched.id, !sched.isActive)
      if (res.success) {
        setSchedules(prev => prev.map(s => s.id === sched.id ? { ...s, isActive: !sched.isActive } : s))
      }
    } catch {
      // ignore
    }
  }

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Are you sure you want to delete the schedule "${name}"?`)) return
    try {
      const res = await reportScheduleApi.delete(id)
      if (res.success) {
        setSchedules(prev => prev.filter(s => s.id !== id))
        setNotice({ type: 'info', message: `Schedule "${name}" was deleted.` })
      }
    } catch {
      // ignore
    }
  }

  const handleSendNow = async (sched: ReportScheduleResponse) => {
    setExecutingId(sched.id)
    setNotice(null)
    try {
      const res = await reportScheduleApi.sendNow(sched.id)
      if (res.success && res.data) {
        setNotice({
          type: 'success',
          message: res.message || 'Report generated and sent!',
          waUrls: res.data.whatsappUrls
        })
        loadData()
      } else {
        setNotice({ type: 'error', message: res.message || 'Failed to dispatch report' })
      }
    } catch (err: any) {
      setNotice({ type: 'error', message: err.response?.data?.message || err.message || 'Failed to send report' })
    } finally {
      setExecutingId(null)
    }
  }

  const formatScheduleTiming = (s: ReportScheduleResponse) => {
    if (s.frequency === 'DAILY') return `Daily at ${s.timeOfDay} EAT`
    if (s.frequency === 'WEEKLY') {
      const dayName = DAYS_OF_WEEK.find(d => Number(d.value) === s.dayOfWeek)?.label || 'Friday'
      return `Weekly on ${dayName} at ${s.timeOfDay} EAT`
    }
    if (s.frequency === 'MONTHLY') {
      return `Monthly on day ${s.dayOfMonth || 1} at ${s.timeOfDay} EAT`
    }
    return `${s.frequency} at ${s.timeOfDay}`
  }

  const renderReportTypeBadge = (type: string) => {
    switch (type) {
      case 'SALES_SUMMARY':
        return <span style={{ background:'#ECFDF5', color:'#065F46', padding:'3px 8px', borderRadius:6, fontSize:11, fontWeight:700 }}>📊 Sales Summary</span>
      case 'PAYMENTS':
        return <span style={{ background:'#EFF6FF', color:'#1E40AF', padding:'3px 8px', borderRadius:6, fontSize:11, fontWeight:700 }}>💳 Payments</span>
      case 'PROFIT_LOSS':
        return <span style={{ background:'#F5F3FF', color:'#5B21B6', padding:'3px 8px', borderRadius:6, fontSize:11, fontWeight:700 }}>📈 Profit & Loss</span>
      case 'LOW_STOCK':
        return <span style={{ background:'#FFFBEB', color:'#92400E', padding:'3px 8px', borderRadius:6, fontSize:11, fontWeight:700 }}>⚠️ Low Stock Alert</span>
      default:
        return <span style={{ background:'#F1F5F9', color:'#475569', padding:'3px 8px', borderRadius:6, fontSize:11, fontWeight:700 }}>{type}</span>
    }
  }

  return (
    <Modal
      title={
        <div style={{ display:'flex', alignItems:'center', gap:10 }}>
          <div style={{ background:'linear-gradient(135deg, #0F766E, #16A34A)', color:'white', width:32, height:32, borderRadius:8, display:'flex', alignItems:'center', justifyContent:'center' }}>
            <Clock size={18} />
          </div>
          <div>
            <div style={{ fontSize:16, fontWeight:800, color:'var(--b360-text)' }}>Report Scheduler</div>
            <div style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)' }}>Automated Email & WhatsApp Delivery</div>
          </div>
        </div>
      }
      onClose={onClose}
      wide
    >
      {/* Navigation Tabs */}
      <div style={{ display:'flex', gap:8, borderBottom:'1px solid var(--b360-border)', paddingBottom:12, marginBottom:16 }}>
        <button
          type="button"
          onClick={() => { setActiveTab('schedules'); setNotice(null) }}
          style={{
            padding:'8px 16px',
            borderRadius:8,
            fontSize:13,
            fontWeight:600,
            border:'none',
            cursor:'pointer',
            background: activeTab === 'schedules' ? '#0F766E' : 'var(--b360-surface)',
            color: activeTab === 'schedules' ? 'white' : 'var(--b360-text-secondary)',
            display:'flex',
            alignItems:'center',
            gap:6,
            transition:'all 0.15s ease'
          }}
        >
          <Clock size={15} /> Active Schedules ({schedules.length})
        </button>
        <button
          type="button"
          onClick={handleOpenCreate}
          style={{
            padding:'8px 16px',
            borderRadius:8,
            fontSize:13,
            fontWeight:600,
            border:'none',
            cursor:'pointer',
            background: activeTab === 'form' ? '#0F766E' : 'var(--b360-surface)',
            color: activeTab === 'form' ? 'white' : 'var(--b360-text-secondary)',
            display:'flex',
            alignItems:'center',
            gap:6,
            transition:'all 0.15s ease'
          }}
        >
          <Plus size={15} /> {editingScheduleId ? 'Edit Schedule' : 'New Schedule'}
        </button>
        <button
          type="button"
          onClick={() => { setActiveTab('logs'); setSelectedScheduleForLogs(null); setNotice(null) }}
          style={{
            padding:'8px 16px',
            borderRadius:8,
            fontSize:13,
            fontWeight:600,
            border:'none',
            cursor:'pointer',
            background: activeTab === 'logs' ? '#0F766E' : 'var(--b360-surface)',
            color: activeTab === 'logs' ? 'white' : 'var(--b360-text-secondary)',
            display:'flex',
            alignItems:'center',
            gap:6,
            transition:'all 0.15s ease'
          }}
        >
          <Calendar size={15} /> Execution History
        </button>
      </div>

      {/* Notifications / Alerts */}
      {notice && (
        <div style={{
          marginBottom:16,
          padding:'12px 16px',
          borderRadius:8,
          background: notice.type === 'success' ? '#ECFDF5' : notice.type === 'error' ? '#FEF2F2' : '#F0F9FF',
          border: `1px solid ${notice.type === 'success' ? '#A7F3D0' : notice.type === 'error' ? '#FECACA' : '#BAE6FD'}`,
          color: notice.type === 'success' ? '#065F46' : notice.type === 'error' ? '#991B1B' : '#0369A1',
          fontSize:13,
          display:'flex',
          flexDirection:'column',
          gap:8
        }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <div style={{ display:'flex', alignItems:'center', gap:8, fontWeight:600 }}>
              {notice.type === 'success' ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
              {notice.message}
            </div>
            <button onClick={() => setNotice(null)} style={{ border:'none', background:'none', cursor:'pointer', color:'inherit' }}>
              <X size={14} />
            </button>
          </div>
          {notice.waUrls && notice.waUrls.length > 0 && (
            <div style={{ display:'flex', gap:8, flexWrap:'wrap', marginTop:4 }}>
              {notice.waUrls.map((url, idx) => (
                <a
                  key={idx}
                  href={url}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    display:'inline-flex',
                    alignItems:'center',
                    gap:6,
                    padding:'6px 12px',
                    borderRadius:6,
                    background:'#25D366',
                    color:'white',
                    fontSize:12,
                    fontWeight:700,
                    textDecoration:'none'
                  }}
                >
                  <MessageSquare size={13} /> Open WhatsApp ({idx + 1}) <ExternalLink size={12} />
                </a>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── TAB 1: SCHEDULES LIST ────────────────────────────────────────── */}
      {activeTab === 'schedules' && (
        <div>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:12 }}>
            <div style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>
              Automated reports scheduled for <strong>{businessName}</strong>.
            </div>
            <Btn variant="primary" icon={<Plus size={14}/>} small onClick={handleOpenCreate}>
              Create Schedule
            </Btn>
          </div>

          {loading ? (
            <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>
              <RefreshCw size={24} className="spin" style={{ marginBottom:8 }} />
              <div>Loading report schedules...</div>
            </div>
          ) : schedules.length === 0 ? (
            <div style={{
              padding:'40px 24px',
              textAlign:'center',
              border:'2px dashed var(--b360-border)',
              borderRadius:12,
              background:'var(--b360-surface)'
            }}>
              <Clock size={36} style={{ color:'var(--b360-text-secondary)', marginBottom:12, opacity:0.6 }} />
              <div style={{ fontSize:15, fontWeight:700, color:'var(--b360-text)', marginBottom:6 }}>
                No automated report schedules yet
              </div>
              <p style={{ fontSize:13, color:'var(--b360-text-secondary)', maxWidth:420, margin:'0 auto 18px' }}>
                Set up automated End-of-Day Sales summaries, weekly Profit & Loss reports, or stock alerts sent directly to Email and WhatsApp.
              </p>
              <Btn variant="primary" icon={<Plus size={14} />} onClick={handleOpenCreate}>
                Configure First Schedule
              </Btn>
            </div>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
              {schedules.map(sched => (
                <div
                  key={sched.id}
                  style={{
                    background:'white',
                    border:'1px solid var(--b360-border)',
                    borderRadius:10,
                    padding:16,
                    boxShadow:'0 1px 3px rgba(0,0,0,0.02)',
                    display:'flex',
                    flexDirection:'column',
                    gap:12
                  }}
                >
                  <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', flexWrap:'wrap', gap:10 }}>
                    <div>
                      <div style={{ display:'flex', alignItems:'center', gap:8, flexWrap:'wrap', marginBottom:4 }}>
                        <h4 style={{ margin:0, fontSize:15, fontWeight:700, color:'var(--b360-text)' }}>
                          {sched.name}
                        </h4>
                        {renderReportTypeBadge(sched.reportType)}
                        <span style={{
                          padding:'2px 8px',
                          borderRadius:20,
                          fontSize:10,
                          fontWeight:700,
                          background: sched.isActive ? '#DCFCE7' : '#F1F5F9',
                          color: sched.isActive ? '#15803D' : '#64748B'
                        }}>
                          {sched.isActive ? 'ACTIVE' : 'PAUSED'}
                        </span>
                      </div>
                      <div style={{ fontSize:12, color:'var(--b360-text-secondary)', display:'flex', alignItems:'center', gap:6 }}>
                        <Clock size={12} /> {formatScheduleTiming(sched)}
                        {sched.branchName && (
                          <span>&bull; Branch: <strong>{sched.branchName}</strong></span>
                        )}
                      </div>
                    </div>

                    {/* Quick action buttons */}
                    <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                      <button
                        type="button"
                        onClick={() => handleSendNow(sched)}
                        disabled={executingId === sched.id}
                        style={{
                          display:'inline-flex',
                          alignItems:'center',
                          gap:5,
                          padding:'6px 12px',
                          borderRadius:6,
                          fontSize:12,
                          fontWeight:700,
                          background:'#0F766E',
                          color:'white',
                          border:'none',
                          cursor: executingId === sched.id ? 'not-allowed' : 'pointer',
                          opacity: executingId === sched.id ? 0.7 : 1
                        }}
                        title="Send report immediately to test delivery"
                      >
                        {executingId === sched.id ? (
                          <>
                            <RefreshCw size={12} className="spin" /> Sending...
                          </>
                        ) : (
                          <>
                            <Send size={12} /> Send Now
                          </>
                        )}
                      </button>

                      <button
                        type="button"
                        onClick={() => handleToggleActive(sched)}
                        style={{
                          padding:'6px 10px',
                          borderRadius:6,
                          fontSize:12,
                          fontWeight:600,
                          background:'var(--b360-surface)',
                          color:'var(--b360-text)',
                          border:'1px solid var(--b360-border)',
                          cursor:'pointer'
                        }}
                        title={sched.isActive ? 'Pause schedule' : 'Activate schedule'}
                      >
                        {sched.isActive ? <Pause size={12} /> : <Play size={12} />}
                      </button>

                      <button
                        type="button"
                        onClick={() => handleOpenEdit(sched)}
                        style={{
                          padding:'6px 10px',
                          borderRadius:6,
                          fontSize:12,
                          fontWeight:600,
                          background:'var(--b360-surface)',
                          color:'var(--b360-text)',
                          border:'1px solid var(--b360-border)',
                          cursor:'pointer'
                        }}
                        title="Edit schedule configuration"
                      >
                        <Edit3 size={12} />
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDelete(sched.id, sched.name)}
                        style={{
                          padding:'6px 10px',
                          borderRadius:6,
                          fontSize:12,
                          fontWeight:600,
                          background:'#FEF2F2',
                          color:'#DC2626',
                          border:'1px solid #FECACA',
                          cursor:'pointer'
                        }}
                        title="Delete schedule"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>

                  {/* Delivery channels and recipients preview */}
                  <div style={{
                    display:'grid',
                    gridTemplateColumns:'repeat(auto-fit, minmax(220px, 1fr))',
                    gap:8,
                    background:'var(--b360-surface)',
                    padding:'8px 12px',
                    borderRadius:6,
                    fontSize:12
                  }}>
                    <div>
                      <div style={{ color:'var(--b360-text-secondary)', fontSize:11, fontWeight:600, marginBottom:2 }}>
                        DELIVERY CHANNELS
                      </div>
                      <div style={{ display:'flex', gap:6, alignItems:'center' }}>
                        {sched.channels.includes('EMAIL') && (
                          <span style={{ display:'inline-flex', alignItems:'center', gap:4, color:'#0F766E', fontWeight:600 }}>
                            <Mail size={12} /> Email
                          </span>
                        )}
                        {sched.channels.includes('WHATSAPP') && (
                          <span style={{ display:'inline-flex', alignItems:'center', gap:4, color:'#16A34A', fontWeight:600 }}>
                            <MessageSquare size={12} /> WhatsApp
                          </span>
                        )}
                      </div>
                    </div>

                    <div>
                      <div style={{ color:'var(--b360-text-secondary)', fontSize:11, fontWeight:600, marginBottom:2 }}>
                        RECIPIENTS
                      </div>
                      <div style={{ color:'var(--b360-text)', wordBreak:'break-all' }}>
                        {sched.emailRecipients ? sched.emailRecipients.split(',').length + ' email(s)' : 'Owner email'}
                        {sched.whatsappRecipients ? ` & ${sched.whatsappRecipients.split(',').length} phone(s)` : ' & Owner phone'}
                      </div>
                    </div>

                    <div>
                      <div style={{ color:'var(--b360-text-secondary)', fontSize:11, fontWeight:600, marginBottom:2 }}>
                        LAST DISPATCH
                      </div>
                      <div style={{ color:'var(--b360-text)' }}>
                        {sched.lastRunAt ? (
                          <span style={{ display:'inline-flex', alignItems:'center', gap:4 }}>
                            {sched.lastStatus === 'SUCCESS' ? (
                              <CheckCircle size={12} style={{ color:'#16A34A' }} />
                            ) : (
                              <AlertTriangle size={12} style={{ color:'#DC2626' }} />
                            )}
                            {new Date(sched.lastRunAt).toLocaleString('en-KE', { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' })}
                          </span>
                        ) : (
                          <span style={{ color:'var(--b360-text-secondary)' }}>Pending first run</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── TAB 2: CREATE / EDIT FORM ────────────────────────────────────── */}
      {activeTab === 'form' && (
        <form onSubmit={handleSave} style={{ display:'flex', flexDirection:'column', gap:16 }}>
          <div style={{ fontSize:13, fontWeight:700, color:'var(--b360-text)', borderBottom:'1px solid var(--b360-border)', paddingBottom:6 }}>
            {editingScheduleId ? 'Edit Report Schedule' : 'Create Automated Report Schedule'}
          </div>

          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:14 }}>
            <div style={{ gridColumn:'1 / -1' }}>
              <Input
                label="Schedule Name"
                value={formData.name}
                onChange={v => setFormData(p => ({ ...p, name: v }))}
              />
            </div>

            <div>
              <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>
                Report Type
              </label>
              <select
                value={formData.reportType}
                onChange={e => setFormData(p => ({ ...p, reportType: e.target.value as any }))}
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:6, fontSize:13, background:'white' }}
              >
                {REPORT_TYPES.map(rt => (
                  <option key={rt.value} value={rt.value}>{rt.label}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>
                Branch (Optional)
              </label>
              <select
                value={formData.branchId || ''}
                onChange={e => setFormData(p => ({ ...p, branchId: e.target.value || null }))}
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:6, fontSize:13, background:'white' }}
              >
                <option value="">All Branches (Consolidated Business)</option>
                {branches.map(b => (
                  <option key={b.id} value={b.id}>{b.name} ({b.code})</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>
                Frequency
              </label>
              <select
                value={formData.frequency}
                onChange={e => setFormData(p => ({ ...p, frequency: e.target.value as any }))}
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:6, fontSize:13, background:'white' }}
              >
                {FREQUENCIES.map(f => (
                  <option key={f.value} value={f.value}>{f.label}</option>
                ))}
              </select>
            </div>

            <div>
              <Input
                label="Delivery Time (24-Hour EAT Nairobi)"
                type="text"
                value={formData.timeOfDay || '20:00'}
                onChange={v => setFormData(p => ({ ...p, timeOfDay: v }))}
              />
            </div>

            {formData.frequency === 'WEEKLY' && (
              <div>
                <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>
                  Day of the Week
                </label>
                <select
                  value={String(formData.dayOfWeek || 5)}
                  onChange={e => setFormData(p => ({ ...p, dayOfWeek: Number(e.target.value) }))}
                  style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:6, fontSize:13, background:'white' }}
                >
                  {DAYS_OF_WEEK.map(d => (
                    <option key={d.value} value={d.value}>{d.label}</option>
                  ))}
                </select>
              </div>
            )}

            {formData.frequency === 'MONTHLY' && (
              <div>
                <Input
                  label="Day of Month (1 - 28)"
                  type="number"
                  value={String(formData.dayOfMonth || 1)}
                  onChange={v => setFormData(p => ({ ...p, dayOfMonth: Math.min(28, Math.max(1, Number(v) || 1)) }))}
                />
              </div>
            )}

            <div style={{ gridColumn:'1 / -1' }}>
              <label style={{ fontSize:12, fontWeight:600, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>
                Delivery Channels
              </label>
              <select
                value={formData.channels}
                onChange={e => setFormData(p => ({ ...p, channels: e.target.value as any }))}
                style={{ width:'100%', padding:'10px 12px', border:'1px solid var(--b360-border)', borderRadius:6, fontSize:13, background:'white' }}
              >
                {CHANNELS.map(ch => (
                  <option key={ch.value} value={ch.value}>{ch.label}</option>
                ))}
              </select>
            </div>

            {formData.channels.includes('EMAIL') && (
              <div style={{ gridColumn:'1 / -1' }}>
                <Input
                  label="Email Recipients (Comma-separated)"
                  value={formData.emailRecipients || ''}
                  onChange={v => setFormData(p => ({ ...p, emailRecipients: v }))}
                />
                <span style={{ fontSize:11, color:'var(--b360-text-secondary)', marginTop:3, display:'block' }}>
                  A branded, responsive executive HTML email report with financial KPIs, tables, and login links will be sent.
                </span>
              </div>
            )}

            {formData.channels.includes('WHATSAPP') && (
              <div style={{ gridColumn:'1 / -1' }}>
                <Input
                  label="WhatsApp Phone Numbers (Comma-separated)"
                  value={formData.whatsappRecipients || ''}
                  onChange={v => setFormData(p => ({ ...p, whatsappRecipients: v }))}
                />
                <span style={{ fontSize:11, color:'var(--b360-text-secondary)', marginTop:3, display:'block' }}>
                  Formatted WhatsApp message with bold metrics, emojis, and dashboard links delivered via WhatsApp Cloud API or click-to-chat.
                </span>
              </div>
            )}

            <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:8 }}>
              <input
                type="checkbox"
                id="schedActive"
                checked={formData.isActive}
                onChange={e => setFormData(p => ({ ...p, isActive: e.target.checked }))}
                style={{ width:16, height:16, cursor:'pointer' }}
              />
              <label htmlFor="schedActive" style={{ fontSize:13, fontWeight:600, color:'var(--b360-text)', cursor:'pointer' }}>
                Schedule is active and will run automatically
              </label>
            </div>
          </div>

          <div style={{ display:'flex', justifyContent:'flex-end', gap:8, borderTop:'1px solid var(--b360-border)', paddingTop:14, marginTop:8 }}>
            <Btn variant="secondary" onClick={() => setActiveTab('schedules')} disabled={saving}>
              Cancel
            </Btn>
            <Btn variant="primary" type="submit" disabled={saving} icon={saving ? <RefreshCw size={14} className="spin" /> : <Check size={14} />}>
              {saving ? 'Saving...' : editingScheduleId ? 'Save Changes' : 'Create Schedule'}
            </Btn>
          </div>
        </form>
      )}

      {/* ─── TAB 3: EXECUTION LOGS ────────────────────────────────────────── */}
      {activeTab === 'logs' && (
        <div>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:12 }}>
            <div style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>
              Recent automated report dispatch runs & delivery statuses.
            </div>
            <button
              onClick={() => loadLogs(selectedScheduleForLogs || undefined)}
              style={{
                display:'inline-flex',
                alignItems:'center',
                gap:5,
                background:'none',
                border:'1px solid var(--b360-border)',
                borderRadius:6,
                padding:'5px 10px',
                fontSize:12,
                cursor:'pointer',
                color:'var(--b360-text)'
              }}
            >
              <RefreshCw size={12} /> Refresh Logs
            </button>
          </div>

          {logs.length === 0 ? (
            <div style={{ padding:32, textAlign:'center', color:'var(--b360-text-secondary)', background:'var(--b360-surface)', borderRadius:8 }}>
              No execution records yet. Click "Send Now" on any schedule to test immediate delivery!
            </div>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:8, maxHeight:'480px', overflowY:'auto' }}>
              {logs.map(log => (
                <div
                  key={log.id}
                  style={{
                    border:'1px solid var(--b360-border)',
                    borderRadius:8,
                    padding:'12px 16px',
                    background:'white'
                  }}
                >
                  <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:6 }}>
                    <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                      <span style={{
                        padding:'2px 8px',
                        borderRadius:20,
                        fontSize:10,
                        fontWeight:700,
                        background: log.status === 'SUCCESS' ? '#DCFCE7' : log.status === 'PARTIAL' ? '#FEF9C3' : '#FEE2E2',
                        color: log.status === 'SUCCESS' ? '#15803D' : log.status === 'PARTIAL' ? '#A16207' : '#B91C1C'
                      }}>
                        {log.status}
                      </span>
                      {renderReportTypeBadge(log.reportType)}
                      <span style={{ fontSize:12, fontWeight:600, color:'var(--b360-text)' }}>
                        {log.period}
                      </span>
                    </div>

                    <div style={{ fontSize:11, color:'var(--b360-text-secondary)' }}>
                      {new Date(log.createdAt).toLocaleString('en-KE', { dateStyle:'medium', timeStyle:'short' })}
                    </div>
                  </div>

                  <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', fontSize:12, color:'var(--b360-text-secondary)' }}>
                    <div>
                      Channels: <strong>{log.channels}</strong> &bull; Recipients: <strong>{log.recipientsCount}</strong>
                      {log.errorMessage && (
                        <span style={{ color:'#DC2626', marginLeft:8 }}>({log.errorMessage})</span>
                      )}
                    </div>
                    {log.summaryText && (
                      <button
                        onClick={() => setExpandedLogId(expandedLogId === log.id ? null : log.id)}
                        style={{
                          background:'none',
                          border:'none',
                          color:'#0F766E',
                          fontWeight:600,
                          fontSize:12,
                          cursor:'pointer'
                        }}
                      >
                        {expandedLogId === log.id ? 'Hide Preview' : 'View Message Preview'}
                      </button>
                    )}
                  </div>

                  {expandedLogId === log.id && log.summaryText && (
                    <div style={{
                      marginTop:10,
                      padding:12,
                      borderRadius:6,
                      background:'#0F172A',
                      color:'#E2E8F0',
                      fontFamily:'monospace',
                      fontSize:11,
                      whiteSpace:'pre-wrap',
                      maxHeight:220,
                      overflowY:'auto'
                    }}>
                      {log.summaryText}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </Modal>
  )
}
