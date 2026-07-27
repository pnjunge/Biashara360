import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Download, Share2, FileText, Table, Building2 } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, ProgressBar, KpiCard, Modal, Input, Select } from '../components/ui'
import { expenseApi, paymentApi, orderApi, reportApi, ExpenseResponse, PaymentResponse, OrderResponse, ProfitSummaryResponse, userApi, superAdminApi, businessApi, BusinessResponse, BusinessProfileRequest, BusinessProfileResponse, UserResponse, InviteUserRequest } from '../services/api'
import { useAuth } from '../App'

function getCurrentMonthRange() {
  const now = new Date()
  const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

// ── Expenses ──────────────────────────────────────────────────────────────────
const catColors: Record<string, string> = {
  ADVERTISING:'var(--b360-blue)', RENT:'var(--b360-red)',
  STOCK_PURCHASE:'var(--b360-green)', DELIVERY:'var(--b360-amber)', PACKAGING:'#9E9E9E'
}

const EXPENSE_CATEGORIES = [
  { value:'ADVERTISING', label:'Advertising' },
  { value:'RENT', label:'Rent' },
  { value:'STOCK_PURCHASE', label:'Stock Purchase' },
  { value:'DELIVERY', label:'Delivery' },
  { value:'PACKAGING', label:'Packaging' },
  { value:'OTHER', label:'Other' },
]

const emptyExpense = { category:'ADVERTISING', amount:'', description:'', expenseDate:new Date().toISOString().slice(0,10) }

export function ExpensesPage() {
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState(emptyExpense)
  const [error, setError] = useState('')

  const loadExpenses = () => {
    setLoading(true)
    expenseApi.list().then(res => {
      if (res.success && res.data) setExpenses(res.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadExpenses() }, [])

  async function handleDelete(id: string) {
    if (!window.confirm('Delete this expense? This cannot be undone.')) return
    try {
      await expenseApi.delete(id)
      setExpenses(prev => prev.filter(e => e.id !== id))
    } catch (_) {
      alert('Failed to delete expense. Please try again.')
    }
  }

  const handleAddExpense = async () => {
    if (!form.amount || !form.description || !form.expenseDate) { setError('All fields are required.'); return }
    setSaving(true); setError('')
    try {
      const res = await expenseApi.create({
        category: form.category,
        amount: Number(form.amount),
        description: form.description,
        expenseDate: form.expenseDate,
      })
      if (res.success) { setShowAdd(false); loadExpenses() }
      else setError(res.message || 'Failed to add expense.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const f = (k: keyof typeof emptyExpense) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const total = expenses.reduce((s, e) => s + e.amount, 0)
  const maxAmount = expenses.length > 0 ? Math.max(...expenses.map(e => e.amount)) : 1

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {showAdd && (
        <Modal title="Add Expense" onClose={() => setShowAdd(false)}
          footer={<><Btn variant="secondary" onClick={() => setShowAdd(false)}>Cancel</Btn><Btn onClick={handleAddExpense} disabled={saving}>{saving ? 'Saving...' : 'Add Expense'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <Select label="Category" value={form.category} onChange={f('category')} options={EXPENSE_CATEGORIES} />
            <Input label="Amount (KES) *" value={form.amount} onChange={f('amount')} type="number" placeholder="0" />
            <Input label="Description *" value={form.description} onChange={f('description')} placeholder="e.g. Facebook Ads April" />
            <Input label="Date *" value={form.expenseDate} onChange={f('expenseDate')} type="date" />
          </div>
        </Modal>
      )}

      <PageHeader title="Expenses & Profit"
        action={<Btn icon={<Plus size={14}/>} onClick={() => { setForm(emptyExpense); setError(''); setShowAdd(true) }}>Add Expense</Btn>} />

      <div className="responsive-grid responsive-grid-4" style={{ gap:12 }}>
        <KpiCard title="Total This Month"  value={`KES ${total.toLocaleString()}`} change="All categories"    icon={<FileText size={18}/>} color="var(--b360-red)" />
        <KpiCard title="Stock Purchase"    value={`KES ${expenses.filter(e=>e.category==='STOCK_PURCHASE').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Stock purchases" icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Advertising"       value={`KES ${expenses.filter(e=>e.category==='ADVERTISING').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Marketing spend" icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Operations"        value={`KES ${expenses.filter(e=>e.category==='RENT'||e.category==='DELIVERY'||e.category==='PACKAGING').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Rent + Ops" icon={<FileText size={18}/>} color="var(--b360-amber)" />
      </div>

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : expenses.length === 0 ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No expenses yet. Click "Add Expense" to record one.</div>
      ) : (
        <>
          <div className="responsive-grid responsive-grid-2" style={{ gap:16 }}>
            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:16 }}>Expense Breakdown</h3>
              {expenses.map(e => (
                <div key={e.id} style={{ marginBottom:14 }}>
                  <div style={{ display:'flex', justifyContent:'space-between', marginBottom:5 }}>
                    <div>
                      <span style={{ fontWeight:500, fontSize:13 }}>{e.description}</span>
                      <span style={{ marginLeft:8, fontSize:11, color:catColors[e.category] || '#9E9E9E', fontWeight:600 }}>
                        {e.category.replace('_',' ')}
                      </span>
                    </div>
                    <span style={{ fontWeight:700, color:'var(--b360-red)', fontSize:13 }}>KES {e.amount.toLocaleString()}</span>
                  </div>
                  <ProgressBar value={e.amount / maxAmount} color={catColors[e.category] || '#9E9E9E'} />
                </div>
              ))}
            </Card>

            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:16 }}>Monthly Expense Chart</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={expenses}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="category" tick={{ fontSize:9 }} tickFormatter={v => v.slice(0,4)} />
                  <YAxis tick={{ fontSize:11 }} tickFormatter={v => `${v/1000}K`} />
                  <Tooltip formatter={(v:number) => `KES ${v.toLocaleString()}`} />
                  <Bar dataKey="amount" radius={[4,4,0,0]}>
                    {expenses.map((e,i) => <Cell key={i} fill={catColors[e.category] || '#ccc'} />)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Card>
          </div>

          <Card>
            <DataTable
              headers={['Description', 'Category', 'Amount', 'Date', 'Actions']}
              rows={expenses.map(e => [
                <span style={{ fontWeight:500 }}>{e.description}</span>,
                <span style={{ color:catColors[e.category] || '#9E9E9E', fontWeight:600, fontSize:12 }}>{e.category.replace('_',' ')}</span>,
                <span style={{ fontWeight:700, color:'var(--b360-red)' }}>KES {e.amount.toLocaleString()}</span>,
                e.expenseDate,
                <Btn variant="danger" small onClick={() => handleDelete(e.id)}>Delete</Btn>
              ])}
            />
          </Card>
        </>
      )}
    </div>
  )
}

// ── Payments ──────────────────────────────────────────────────────────────────
export function PaymentsPage() {
  const [payments, setPayments] = useState<PaymentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [matchPayment, setMatchPayment] = useState<PaymentResponse | null>(null)
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [selectedOrderId, setSelectedOrderId] = useState('')
  const [matching, setMatching] = useState(false)
  const [matchError, setMatchError] = useState('')
  const [queryingId, setQueryingId] = useState<string | null>(null)
  const [queryMessage, setQueryMessage] = useState('')

  const loadPayments = () => {
    setLoading(true)
    paymentApi.list().then(res => {
      if (res.success && res.data) setPayments(res.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadPayments() }, [])

  const openMatch = (p: PaymentResponse) => {
    setMatchPayment(p); setSelectedOrderId(''); setMatchError('')
    orderApi.list('PENDING').then(res => {
      if (res.success && res.data) setOrders(res.data.data)
    })
  }

  const handleMatch = async () => {
    if (!matchPayment || !selectedOrderId) { setMatchError('Please select an order.'); return }
    setMatching(true); setMatchError('')
    try {
      const res = await paymentApi.reconcile(matchPayment.id, { orderId: selectedOrderId })
      if (res.success) { setMatchPayment(null); loadPayments() }
      else setMatchError(res.message || 'Failed to match payment.')
    } catch (e: any) {
      setMatchError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setMatching(false) }
  }

  const queryTransaction = async (transactionId: string) => {
    setQueryingId(transactionId); setQueryMessage('')
    try {
      const res = await paymentApi.transactionQuery(transactionId)
      setQueryMessage(res.success ? 'Transaction status query submitted.' : (res.message || 'Transaction query failed.'))
    } catch (e: any) {
      setQueryMessage(e.response?.data?.message || 'Transaction query failed.')
    } finally { setQueryingId(null) }
  }

  const unreconciled = payments.filter(p => !p.isReconciled)
  const total = payments.filter(p => p.isReconciled).reduce((s, p) => s + p.amount, 0)

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {matchPayment && (
        <Modal title="Match Payment to Order" onClose={() => setMatchPayment(null)}
          footer={<><Btn variant="secondary" onClick={() => setMatchPayment(null)}>Cancel</Btn><Btn onClick={handleMatch} disabled={matching || !selectedOrderId}>{matching ? 'Matching...' : 'Confirm Match'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {matchError && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{matchError}</p>}
            <div style={{ background:'var(--b360-surface)', borderRadius:8, padding:12 }}>
              <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Mpesa Transaction</div>
              <div style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)' }}>{matchPayment.mpesaTransactionCode}</div>
              <div style={{ fontWeight:600 }}>{matchPayment.payerName} · KES {matchPayment.amount.toLocaleString()}</div>
            </div>
            <div>
              <label style={{ fontSize:12, fontWeight:500, color:'var(--b360-text-secondary)', display:'block', marginBottom:5 }}>Select Order to Match</label>
              <select value={selectedOrderId} onChange={e => setSelectedOrderId(e.target.value)}
                style={{ width:'100%', padding:'9px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit', background:'white' }}>
                <option value="">Select an order...</option>
                {orders.map(o => (
                  <option key={o.id} value={o.id}>{o.orderNumber} — {o.customerName} — KES {o.subtotal.toLocaleString()}</option>
                ))}
              </select>
              {orders.length === 0 && <p style={{ fontSize:12, color:'var(--b360-text-secondary)', marginTop:6 }}>No pending orders found.</p>}
            </div>
          </div>
        </Modal>
      )}

      <PageHeader title="Payments / Mpesa" />
      {queryMessage && <div style={{ padding:12, background:'var(--b360-surface)', borderRadius:8, fontSize:13 }}>{queryMessage}</div>}

      <div className="responsive-grid responsive-grid-4" style={{ gap:12 }}>
        <KpiCard title="Total Collected"    value={`KES ${total.toLocaleString()}`} change="Reconciled payments" icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Unreconciled"       value={`${unreconciled.length} txns`}   change="Need matching"      icon={<FileText size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Mpesa Transactions" value={`${payments.length}`}             change="All time"           icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Pending"            value={`${unreconciled.length}`}         change="Awaiting reconciliation" icon={<FileText size={18}/>} color="var(--b360-red)" />
      </div>

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : payments.length === 0 ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No payments yet</div>
      ) : (
        <>
          {unreconciled.length > 0 && (
            <div>
              <h3 style={{ fontWeight:700, marginBottom:10, fontSize:14 }}>⚠️ Needs Reconciliation</h3>
              <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
                {unreconciled.map(p => (
                  <div key={p.id} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'12px 16px', background:'var(--b360-amber-bg)', borderRadius:10, border:'1px solid var(--b360-amber)' }}>
                    <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                      <div style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:13 }}>{p.mpesaTransactionCode}</div>
                      <div>
                        <div style={{ fontWeight:600 }}>{p.payerName}</div>
                        <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>{p.phoneNumber} · Mpesa</div>
                      </div>
                    </div>
                    <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                      <span style={{ fontWeight:800, fontSize:15 }}>KES {p.amount.toLocaleString()}</span>
                      <Btn onClick={() => openMatch(p)}>Match to Order</Btn>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <Card>
            <DataTable
              headers={['Mpesa Code', 'Customer', 'Phone', 'Amount', 'Status', 'Date', 'Action']}
              rows={payments.map(p => [
                <span style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{p.mpesaTransactionCode}</span>,
                <span style={{ fontWeight:600 }}>{p.payerName}</span>,
                p.phoneNumber,
                <span style={{ fontWeight:700 }}>KES {p.amount.toLocaleString()}</span>,
                <StatusBadge status={p.isReconciled ? 'MATCHED' : 'PENDING'} />,
                new Date(p.createdAt).toLocaleDateString('en-KE'),
                <Btn variant="secondary" small disabled={!p.mpesaTransactionCode || queryingId === p.mpesaTransactionCode}
                  onClick={() => p.mpesaTransactionCode && queryTransaction(p.mpesaTransactionCode)}>
                  {queryingId === p.mpesaTransactionCode ? 'Querying…' : 'Query status'}
                </Btn>
              ])}
            />
          </Card>
        </>
      )}
    </div>
  )
}

function getPeriodRange(period: string) {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const fmtDate = (d: Date) => d.toISOString().slice(0, 10)

  if (period === 'Today') {
    const todayStr = fmtDate(now)
    return { startDate: todayStr, endDate: todayStr }
  } else if (period === 'This Week') {
    const day = now.getDay()
    const diffToMon = now.getDate() - day + (day === 0 ? -6 : 1)
    const start = new Date(now.setDate(diffToMon))
    return { startDate: fmtDate(start), endDate: fmtDate(new Date()) }
  } else if (period === 'This Quarter') {
    const qMonth = Math.floor(month / 3) * 3
    const start = new Date(year, qMonth, 1)
    return { startDate: fmtDate(start), endDate: fmtDate(now) }
  } else if (period === 'This Year') {
    const start = new Date(year, 0, 1)
    return { startDate: fmtDate(start), endDate: fmtDate(now) }
  } else {
    return getCurrentMonthRange()
  }
}

// ── Reports ───────────────────────────────────────────────────────────────────
export function ReportsPage() {
  const [profitSummary, setProfitSummary] = useState<ProfitSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [period, setPeriod] = useState<string>('This Month')

  const loadReport = (selectedPeriod: string) => {
    setLoading(true)
    const { startDate, endDate } = getPeriodRange(selectedPeriod)
    reportApi.profitSummary(startDate, endDate).then(res => {
      if (res.success && res.data) setProfitSummary(res.data)
      else setProfitSummary(null)
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    loadReport(period)
  }, [period])

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Reports"
        action={
          <div style={{ display:'flex', gap:8, alignItems:'center' }}>
            <select
              value={period}
              onChange={e => setPeriod(e.target.value)}
              style={{
                padding: '8px 12px',
                border: '1px solid var(--b360-border)',
                borderRadius: 8,
                fontSize: 13,
                fontFamily: 'inherit',
                background: 'white',
                fontWeight: 600,
                color: 'var(--b360-green)',
                cursor: 'pointer'
              }}
            >
              <option value="Today">Today</option>
              <option value="This Week">This Week</option>
              <option value="This Month">This Month</option>
              <option value="This Quarter">This Quarter</option>
              <option value="This Year">This Year</option>
            </select>
            <Btn variant="secondary" icon={<Download size={14}/>}
              onClick={() => alert('PDF export will be available once data is available from the backend.')}>Export PDF</Btn>
            <Btn variant="secondary" icon={<Table size={14}/>}
              onClick={() => alert('Excel export will be available once data is available from the backend.')}>Export Excel</Btn>
            <Btn variant="secondary" icon={<Share2 size={14}/>}
              onClick={() => window.open('https://wa.me/?text=Biashara360+Report', '_blank')}>WhatsApp</Btn>
          </div>
        }
      />

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : (
        <div className="responsive-grid responsive-grid-2" style={{ gap:16 }}>
          {/* P&L */}
          <Card style={{ padding:20 }}>
            <h3 style={{ fontWeight:700, marginBottom:16 }}>Profit & Loss — {period}</h3>
            {profitSummary ? (
              <>
                {[
                  ['Total Revenue',     profitSummary.totalRevenue,      'var(--b360-green)', false],
                  ['Cost of Goods',     profitSummary.totalCostOfGoods,  'var(--b360-red)',   false],
                  ['Gross Profit',      profitSummary.grossProfit,       'var(--b360-green)', true],
                  ['Total Expenses',    profitSummary.totalExpenses,     'var(--b360-red)',   false],
                ].map(([l, v, c, bold]) => (
                  <div key={l as string} style={{ display:'flex', justifyContent:'space-between', padding:'8px 0', borderBottom:'1px solid var(--b360-border)', fontWeight: bold ? 700 : 400 }}>
                    <span style={{ fontSize:13 }}>{l}</span>
                    <span style={{ color: c as string, fontWeight: bold ? 700 : 600, fontSize:13 }}>KES {(v as number).toLocaleString()}</span>
                  </div>
                ))}
                <div style={{ display:'flex', justifyContent:'space-between', padding:'12px 0 0', marginTop:4 }}>
                  <span style={{ fontWeight:800, fontSize:15 }}>Net Profit</span>
                  <span style={{ color:'var(--b360-green)', fontWeight:800, fontSize:15 }}>KES {profitSummary.netProfit.toLocaleString()}</span>
                </div>
                <div style={{ display:'flex', justifyContent:'space-between' }}>
                  <span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Net Margin</span>
                  <span style={{ color:'var(--b360-blue)', fontWeight:600, fontSize:12 }}>{(profitSummary.netMargin * 100).toFixed(1)}%</span>
                </div>
              </>
            ) : (
              <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>
                No data yet — data will appear as orders are recorded
              </div>
            )}
          </Card>

          {/* Summary KPIs */}
          <Card style={{ padding:20 }}>
            <h3 style={{ fontWeight:700, marginBottom:16 }}>{period} Summary</h3>
            {profitSummary ? (
              <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
                {[
                  ['Gross Profit',   profitSummary.grossProfit,  `${(profitSummary.grossMargin * 100).toFixed(1)}% margin`, 'var(--b360-green)'],
                  ['Total Expenses', profitSummary.totalExpenses, 'Operating costs', 'var(--b360-red)'],
                  ['Cash In',        profitSummary.cashflowIn,   'Revenue received', 'var(--b360-blue)'],
                  ['Cash Out',       profitSummary.cashflowOut,  'Expenses paid', 'var(--b360-amber)'],
                ].map(([label, value, sub, color]) => (
                  <div key={label as string} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 16px', background:'var(--b360-surface)', borderRadius:10 }}>
                    <div>
                      <div style={{ fontWeight:600, fontSize:14 }}>{label}</div>
                      <div style={{ fontSize:11, color:'var(--b360-text-secondary)' }}>{sub}</div>
                    </div>
                    <span style={{ fontWeight:800, fontSize:16, color: color as string }}>KES {(value as number).toLocaleString()}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>
                No data yet
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}

// ── Settings ──────────────────────────────────────────────────────────────────
// ── User Creation ─────────────────────────────────────────────────────────────
const emptyBusinessAdmin = { businessName: '', businessType: '', adminName: '', adminEmail: '', adminPhone: '', adminPassword: '' }
const emptyUser: InviteUserRequest = { name: '', email: '', phone: '', role: 'STAFF', password: '' }

export function UserCreationPage() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.role === 'SUPERADMIN'

  // ── Businesses list (SUPERADMIN only) ──
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [bizLoading, setBizLoading] = useState(false)
  const [bizError, setBizError] = useState('')

  // ── Create admin modal (SUPERADMIN only) ──
  const [showCreateAdmin, setShowCreateAdmin] = useState(false)
  const [adminForm, setAdminForm] = useState(emptyBusinessAdmin)
  const [adminError, setAdminError] = useState('')
  const [adminSaving, setAdminSaving] = useState(false)

  // ── Regular users (all admins) ──
  const [users, setUsers] = useState<UserResponse[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState<InviteUserRequest>(emptyUser)
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const loadUsers = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setUsers([])
      return
    }
    setUsersLoading(true)
    userApi.list(isSuperAdmin ? selectedBusinessId : undefined).then(res => {
      if (res.success && res.data) setUsers(res.data)
    }).catch(() => {}).finally(() => setUsersLoading(false))
  }

  const loadBusinesses = () => {
    if (!isSuperAdmin) return
    setBizLoading(true)
    setBizError('')
    superAdminApi.listBusinesses().then(res => {
      if (res.success && res.data) {
        setBusinesses(res.data)
        if (!selectedBusinessId && res.data.length > 0) {
          setSelectedBusinessId(res.data[0].id)
        }
      }
      else setBizError(res.message || 'Failed to load businesses.')
    }).catch(() => setBizError('Network error. Could not load businesses.')).finally(() => setBizLoading(false))
  }

  useEffect(() => {
    loadBusinesses()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSuperAdmin])

  useEffect(() => {
    loadUsers()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSuperAdmin, selectedBusinessId])

  // ── Handlers ──

  const af = (k: keyof typeof emptyBusinessAdmin) => (v: string) =>
    setAdminForm(prev => ({ ...prev, [k]: v }))

  const handleCreateAdmin = async () => {
    const { businessName, businessType, adminName, adminEmail, adminPhone, adminPassword } = adminForm
    if (!businessName || !businessType || !adminName || !adminEmail || !adminPhone || !adminPassword) {
      setAdminError('All fields are required.')
      return
    }
    setAdminSaving(true); setAdminError('')
    try {
      const res = await superAdminApi.createBusinessWithAdmin(adminForm)
      if (res.success) {
        setShowCreateAdmin(false)
        setAdminForm(emptyBusinessAdmin)
        loadBusinesses()
      } else {
        setAdminError(res.message || 'Failed to create admin.')
      }
    } catch (e: any) {
      setAdminError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setAdminSaving(false) }
  }

  const f = (k: keyof InviteUserRequest) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const handleAdd = async () => {
    if (!form.name || !form.email || !form.phone || !form.password) { setError('All fields are required.'); return }
    if (isSuperAdmin && !selectedBusinessId) { setError('Please select a business.'); return }
    setSaving(true); setError('')
    try {
      const res = await userApi.invite(form, isSuperAdmin ? selectedBusinessId : undefined)
      if (res.success && res.data) {
        setShowAdd(false)
        setForm(emptyUser)
        if (!isSuperAdmin) setSelectedBusinessId('')
        loadUsers()
      }
      else setError(res.message || 'Failed to create user.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm('Disable this user?')) return
    await userApi.setStatus(id, false, isSuperAdmin ? selectedBusinessId : undefined)
    setUsers(prev => prev.map(u => (u.id === id ? { ...u, isActive: false } : u)))
  }

  const handleToggleUserStatus = async (id: string, isActive: boolean) => {
    await userApi.setStatus(id, isActive, isSuperAdmin ? selectedBusinessId : undefined)
    setUsers(prev => prev.map(u => (u.id === id ? { ...u, isActive } : u)))
  }

  const handleToggleBusinessStatus = async (id: string, isActive: boolean) => {
    const res = await superAdminApi.setBusinessStatus(id, { isActive })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === id ? res.data! : b)))
    }
  }

  const ROLES = [{ value: 'ADMIN', label: 'Admin' }, { value: 'MANAGER', label: 'Manager' }, { value: 'STAFF', label: 'Staff' }]
  const roleColor = (role: string) => role === 'ADMIN' ? 'PAID' : role === 'MANAGER' ? 'PENDING' : 'COD'

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* ── Create Admin Modal (SUPERADMIN) ── */}
      {showCreateAdmin && (
        <Modal
          title="Create Business & Admin"
          onClose={() => { setShowCreateAdmin(false); setAdminForm(emptyBusinessAdmin); setAdminError('') }}
          footer={
            <>
              <Btn variant="secondary" onClick={() => { setShowCreateAdmin(false); setAdminForm(emptyBusinessAdmin); setAdminError('') }}>Cancel</Btn>
              <Btn onClick={handleCreateAdmin} disabled={adminSaving}>{adminSaving ? 'Creating...' : 'Create Admin'}</Btn>
            </>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {adminError && <div style={{ color: 'var(--b360-red)', fontSize: 13 }}>{adminError}</div>}
            <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--b360-text-secondary)', borderBottom: '1px solid var(--b360-border)', paddingBottom: 6 }}>Business Details</div>
            <Input label="Business Name *" value={adminForm.businessName} onChange={af('businessName')} placeholder="e.g. Kamau Supplies" />
            <Input label="Business Type *" value={adminForm.businessType} onChange={af('businessType')} placeholder="e.g. Retail" />
            <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--b360-text-secondary)', borderBottom: '1px solid var(--b360-border)', paddingBottom: 6, marginTop: 4 }}>Admin User Details</div>
            <Input label="Admin Full Name *" value={adminForm.adminName} onChange={af('adminName')} placeholder="e.g. Jane Mwangi" />
            <Input label="Admin Email *" value={adminForm.adminEmail} onChange={af('adminEmail')} placeholder="jane@example.com" />
            <Input label="Admin Phone *" value={adminForm.adminPhone} onChange={af('adminPhone')} placeholder="+254 7XX XXX XXX" />
            <Input label="Temporary Password *" value={adminForm.adminPassword} onChange={af('adminPassword')} placeholder="Min 6 characters" />
          </div>
        </Modal>
      )}

      {/* ── Add User Modal (regular admin) ── */}
      {showAdd && (
        <Modal title="Add New User" onClose={() => { setShowAdd(false); setForm(emptyUser); setSelectedBusinessId(''); setError('') }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Input label="Full Name" value={form.name} onChange={f('name')} placeholder="e.g. Jane Mwangi" />
            <Input label="Email" value={form.email} onChange={f('email')} placeholder="jane@example.com" />
            <Input label="Phone" value={form.phone} onChange={f('phone')} placeholder="+254 7XX XXX XXX" />
            <Input label="Password" value={form.password} onChange={f('password')} placeholder="Temporary password" />
            <Select label="Role" value={form.role ?? 'STAFF'} onChange={f('role')} options={ROLES} />
            {isSuperAdmin && (
              <Select
                label="Business *"
                value={selectedBusinessId}
                onChange={setSelectedBusinessId}
                options={businesses.map(b => ({ value: b.id, label: b.name }))}
                placeholder={bizLoading ? 'Loading businesses…' : 'Select business'}
              />
            )}
            {error && <div style={{ color: 'var(--b360-red)', fontSize: 13 }}>{error}</div>}
            <Btn onClick={handleAdd} disabled={saving}>{saving ? 'Creating...' : 'Create User'}</Btn>
          </div>
        </Modal>
      )}

      {/* ── Businesses list (SUPERADMIN only) ── */}
      {isSuperAdmin && (
        <>
          <PageHeader
            title="Businesses"
            action={<Btn icon={<Building2 size={14} />} onClick={() => { setShowCreateAdmin(true); setAdminError('') }}>Add Business & Admin</Btn>}
          />
          <Card style={{ padding: 0 }}>
            {bizLoading ? (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading businesses…</div>
            ) : bizError ? (
              <div style={{ padding: 24, color: 'var(--b360-red)' }}>{bizError}</div>
            ) : businesses.length === 0 ? (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No businesses yet. Create one above.</div>
            ) : (
              <DataTable
                headers={['Business Name', 'Type', 'Owner Email', 'Owner Phone', 'Tier', 'Status', 'Created', 'Action']}
                rows={businesses.map(b => [
                  b.name,
                  b.type,
                  b.ownerEmail,
                  b.ownerPhone,
                  <StatusBadge key="tier" status={b.subscriptionTier === 'FREEMIUM' ? 'COD' : 'PAID'} />,
                  <StatusBadge key="status" status={b.isActive ? 'PAID' : 'FAILED'} />,
                  new Date(b.createdAt).toLocaleDateString(),
                  <Btn key="biz-status" variant={b.isActive ? 'danger' : 'secondary'} small onClick={() => handleToggleBusinessStatus(b.id, !b.isActive)}>
                    {b.isActive ? 'Disable' : 'Enable'}
                  </Btn>,
                ])}
              />
            )}
          </Card>
        </>
      )}

      {/* ── User Management ── */}
      <PageHeader title="User Management" action={<Btn onClick={() => { setSelectedBusinessId(businesses[0]?.id ?? ''); setShowAdd(true) }} icon={<Plus size={14} />}>Add User</Btn>} />
      <Card style={{ padding: 0 }}>
        {usersLoading ? (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading users…</div>
        ) : (
          <DataTable
            headers={['Name', 'Email', 'Phone', 'Role', 'Status', '']}
            rows={users.map(u => [
              u.name,
              u.email,
              u.phone,
              <StatusBadge key="role" status={u.role} />,
              <StatusBadge key="status" status={u.isActive === false ? 'INACTIVE' : 'ACTIVE'} />,
              <Btn key="del" variant={u.isActive === false ? 'secondary' : 'danger'} small onClick={() => handleToggleUserStatus(u.id, u.isActive === false)}>
                {u.isActive === false ? 'Enable' : 'Disable'}
              </Btn>,
            ])}
          />
        )}
      </Card>
    </div>
  )
}

// ── Business Profile ──────────────────────────────────────────────────────────

const emptyProfile: BusinessProfileRequest = {
  name: '', owner: '', phone: '', email: '',
  type: '', county: '', address: '',
  kraPin: '', paybillNumber: '', accountNumber: '',
}

const BizSection = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <Card style={{ padding: 20, marginBottom: 16 }}>
    <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
    <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
  </Card>
)

const BizField = ({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <span style={{ fontSize: 13, color: 'var(--b360-text-secondary)', width: 160 }}>{label}</span>
    <input
      value={value || ''}
      onChange={e => onChange(e.target.value)}
      style={{ flex: 1, maxWidth: 320, padding: '8px 12px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 13, fontFamily: 'inherit', outline: 'none' }}
    />
  </div>
)

export function BusinessPage() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.role === 'SUPERADMIN'

  // ── SuperAdmin: business list ──
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [bizLoading, setBizLoading] = useState(false)
  const [bizError, setBizError] = useState('')

  // ── SuperAdmin: create business modal (name + type only) ──
  const [showCreateBiz, setShowCreateBiz] = useState(false)
  const [bizForm, setBizForm] = useState({ businessName: '', businessType: 'RETAIL' })
  const [bizFormError, setBizFormError] = useState('')
  const [bizFormSaving, setBizFormSaving] = useState(false)

  // ── Admin: business profile ──
  const [form, setForm] = useState<BusinessProfileRequest>(emptyProfile)
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveMsg, setSaveMsg] = useState<{ ok: boolean; text: string } | null>(null)

  const handleToggleBusinessStatus = async (id: string, isActive: boolean) => {
    const res = await superAdminApi.setBusinessStatus(id, { isActive })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === id ? res.data! : b)))
    }
  }

  const handleCreateBusiness = async () => {
    if (!bizForm.businessName.trim() || !bizForm.businessType.trim()) {
      setBizFormError('Business name and type are required.')
      return
    }
    setBizFormSaving(true); setBizFormError('')
    try {
      const res = await superAdminApi.createBusiness(bizForm)
      if (res.success) {
        setShowCreateBiz(false)
        setBizForm({ businessName: '', businessType: 'RETAIL' })
        superAdminApi.listBusinesses().then(r => { if (r.success && r.data) setBusinesses(r.data) })
      } else {
        setBizFormError(res.message || 'Failed to create business.')
      }
    } catch (e: any) {
      setBizFormError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setBizFormSaving(false) }
  }

  useEffect(() => {
    if (isSuperAdmin) {
      setBizLoading(true)
      setBizError('')
      superAdminApi.listBusinesses()
        .then(res => {
          if (res.success && res.data) setBusinesses(res.data)
          else setBizError(res.message || 'Failed to load businesses.')
        })
        .catch(() => setBizError('Network error. Could not load businesses.'))
        .finally(() => setBizLoading(false))
    } else {
      setProfileLoading(true)
      setProfileError('')
      businessApi.getProfile()
        .then(res => {
          if (res.success && res.data) {
            const d = res.data
            setForm({ name: d.name, owner: d.owner, phone: d.phone, email: d.email, type: d.type, county: d.county, address: d.address, kraPin: d.kraPin, paybillNumber: d.paybillNumber, accountNumber: d.accountNumber })
          } else {
            setProfileError(res.message || 'Failed to load business profile.')
          }
        })
        .catch(() => setProfileError('Network error. Could not load business profile.'))
        .finally(() => setProfileLoading(false))
    }
  }, [isSuperAdmin])

  const f = (k: keyof BusinessProfileRequest) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const handleSave = async () => {
    setSaving(true)
    setSaveMsg(null)
    try {
      const res = await businessApi.updateProfile(form)
      setSaveMsg({ ok: res.success, text: res.message || (res.success ? 'Saved' : 'Failed to save') })
    } catch (e: any) {
      setSaveMsg({ ok: false, text: e.response?.data?.message || 'Network error. Please try again.' })
    } finally {
      setSaving(false)
    }
  }

  // ── SuperAdmin view: list of all businesses ──
  if (isSuperAdmin) {
    const BIZ_TYPES = [
      { value: 'RETAIL', label: 'Retail' },
      { value: 'SERVICE', label: 'Service' },
      { value: 'HYBRID', label: 'Hybrid' },
      { value: 'ONLINE_SELLER', label: 'Online Seller' },
    ]
    return (
      <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

        {/* Create Business Modal — name & type only */}
        {showCreateBiz && (
          <Modal
            title="Add Business"
            onClose={() => { setShowCreateBiz(false); setBizForm({ businessName: '', businessType: 'RETAIL' }); setBizFormError('') }}
            footer={
              <>
                <Btn variant="secondary" onClick={() => { setShowCreateBiz(false); setBizFormError('') }}>Cancel</Btn>
                <Btn onClick={handleCreateBusiness} disabled={bizFormSaving}>{bizFormSaving ? 'Creating...' : 'Add Business'}</Btn>
              </>
            }
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {bizFormError && <div style={{ color: 'var(--b360-red)', fontSize: 13 }}>{bizFormError}</div>}
              <Input
                label="Business Name *"
                value={bizForm.businessName}
                onChange={v => setBizForm(p => ({ ...p, businessName: v }))}
                placeholder="e.g. Kamau Supplies"
              />
              <Select
                label="Business Type *"
                value={bizForm.businessType}
                onChange={v => setBizForm(p => ({ ...p, businessType: v }))}
                options={BIZ_TYPES}
              />
              <p style={{ fontSize: 12, color: 'var(--b360-text-secondary)', margin: 0 }}>
                💡 You can add admin users to this business from the <strong>Users</strong> menu after creation.
              </p>
            </div>
          </Modal>
        )}

        <PageHeader
          title="Businesses"
          action={<Btn icon={<Building2 size={14} />} onClick={() => { setBizFormError(''); setShowCreateBiz(true) }}>Add Business</Btn>}
        />
        <Card style={{ padding: 0 }}>
          {bizLoading ? (
            <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading businesses…</div>
          ) : bizError ? (
            <div style={{ padding: 24, color: 'var(--b360-red)' }}>{bizError}</div>
          ) : businesses.length === 0 ? (
            <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>No businesses yet. Click "Add Business" to create one.</div>
          ) : (
            <DataTable
              headers={['Business Name', 'Type', 'Owner Email', 'Owner Phone', 'Tier', 'Status', 'Created', 'Action']}
              rows={businesses.map(b => [
                b.name,
                b.type,
                b.ownerEmail,
                b.ownerPhone,
                <StatusBadge key="tier" status={b.subscriptionTier === 'FREEMIUM' ? 'COD' : 'PAID'} />,
                <StatusBadge key="status" status={b.isActive ? 'PAID' : 'FAILED'} />,
                new Date(b.createdAt).toLocaleDateString(),
                <Btn key="biz-status" variant={b.isActive ? 'danger' : 'secondary'} small onClick={() => handleToggleBusinessStatus(b.id, !b.isActive)}>
                  {b.isActive ? 'Disable' : 'Enable'}
                </Btn>,
              ])}
            />
          )}
        </Card>
      </div>
    )
  }

  // ── Admin view: editable business profile ──
  if (profileLoading) {
    return <div style={{ padding: 32, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading business profile…</div>
  }

  if (profileError) {
    return <div style={{ padding: 32, color: 'var(--b360-red)' }}>{profileError}</div>
  }

  return (
    <div className="fade-in" style={{ maxWidth: 680 }}>
      <PageHeader title="Business Profile" action={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {saveMsg && (
            <span style={{ fontSize: 12, color: saveMsg.ok ? 'var(--b360-green)' : 'var(--b360-red)', fontWeight: 600 }}>
              {saveMsg.ok ? '✓ ' : '✗ '}{saveMsg.text}
            </span>
          )}
          <Btn onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Changes'}</Btn>
        </div>
      } />
      <BizSection title="General Information">
        <BizField label="Business Name" value={form.name} onChange={f('name')} />
        <BizField label="Owner Name"    value={form.owner} onChange={f('owner')} />
        <BizField label="Phone Number"  value={form.phone} onChange={f('phone')} />
        <BizField label="Email Address" value={form.email} onChange={f('email')} />
        <BizField label="Business Type" value={form.type} onChange={f('type')} />
        <BizField label="County"        value={form.county} onChange={f('county')} />
        <BizField label="Address"       value={form.address} onChange={f('address')} />
      </BizSection>
      <BizSection title="Tax & Compliance">
        <BizField label="KRA PIN" value={form.kraPin} onChange={f('kraPin')} />
      </BizSection>
    </div>
  )
}

export function SettingsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMerchantAdmin = (user?.role || '').toUpperCase() === 'ADMIN'

  const [twoFA, setTwoFA] = useState(true)
  const [sms, setSms] = useState(true)
  const [email, setEmail] = useState(false)
  const [saved, setSaved] = useState(false)


  const handleSave = () => {
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <Card style={{ padding:20, marginBottom:16 }}>
      <h3 style={{ fontWeight:700, marginBottom:16, fontSize:15 }}>{title}</h3>
      <div style={{ borderTop:'1px solid var(--b360-border)', paddingTop:16, display:'flex', flexDirection:'column', gap:14 }}>{children}</div>
    </Card>
  )

  const Toggle = ({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v:boolean) => void }) => (
    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
      <span style={{ fontSize:13 }}>{label}</span>
      <div onClick={() => onChange(!checked)} style={{
        width:44, height:24, borderRadius:12, cursor:'pointer', transition:'background 0.2s',
        background: checked ? 'var(--b360-green)' : '#D1D5DB', position:'relative'
      }}>
        <div style={{ position:'absolute', top:2, left: checked ? 22 : 2, width:20, height:20, borderRadius:'50%', background:'white', transition:'left 0.2s', boxShadow:'0 1px 3px rgba(0,0,0,0.2)' }} />
      </div>
    </div>
  )

  return (
    <div className="fade-in" style={{ maxWidth:640 }}>
      <PageHeader title="Settings" action={
        <div style={{ display:'flex', alignItems:'center', gap:10 }}>
          {saved && <span style={{ fontSize:12, color:'var(--b360-green)', fontWeight:600 }}>✓ Saved</span>}
          <Btn onClick={handleSave}>Save Settings</Btn>
        </div>
      } />

      <Section title="Store Configurations">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Business Profile</span>
              <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Manage store details, contact info, and tax settings</span>
            </div>
            <Btn onClick={() => navigate('/business')} variant="secondary" small>Manage</Btn>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--b360-border)', paddingTop: 12 }}>
            <div>
              <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Receipt Template</span>
              <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Customize customer thermal receipt headers and footers</span>
            </div>
            <Btn onClick={() => navigate('/receipt-template')} variant="secondary" small>Manage</Btn>
          </div>
        </div>
      </Section>

      <Section title="Integrations & Gateways">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>M-Pesa Setup (Daraja)</span>
              <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Configure shortcode, merchant passkey, and checkout settings</span>
            </div>
            <Btn onClick={() => navigate('/mpesa-settings')} variant="secondary" small>Configure</Btn>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--b360-border)', paddingTop: 12 }}>
            <div>
              <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>CyberSource API Gateways</span>
              <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Configure card payment checkout and merchant keys</span>
            </div>
            <Btn onClick={() => navigate('/cybersource-settings')} variant="secondary" small>Configure</Btn>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--b360-border)', paddingTop: 12 }}>
            <div>
              <span style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>Social Media Commerce Suite</span>
              <span style={{ fontSize: 11, color: 'var(--b360-text-secondary)' }}>Launch onboarding wizard to integrate WhatsApp, Instagram, Facebook, and TikTok</span>
            </div>
            <Btn onClick={() => navigate('/social-onboarding')} variant="secondary" small>Launch Wizard</Btn>
          </div>
        </div>
      </Section>
      <Section title="Security">
        {isMerchantAdmin && (
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', borderBottom:'1px solid var(--b360-border)', paddingBottom:14 }}>
            <div>
              <span style={{ fontSize:13, fontWeight:600, display:'block' }}>Session Timeout Policy</span>
              <span style={{ fontSize:11, color:'var(--b360-text-secondary)' }}>Set idle timeouts for web, Android, and desktop users</span>
            </div>
            <Btn onClick={() => navigate('/session-timeouts')} variant="secondary" small>Manage</Btn>
          </div>
        )}
        <Toggle label="Two-Factor Authentication (2FA)" checked={twoFA} onChange={setTwoFA} />
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <span style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>2FA Method</span>
          <select style={{ padding:'7px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit' }}>
            <option>SMS OTP</option><option>Email OTP</option><option>Authenticator App</option>
          </select>
        </div>
      </Section>
      <Section title="Notifications">
        <Toggle label="SMS Alerts (low stock, payments)" checked={sms} onChange={setSms} />
        <Toggle label="Email Notifications"              checked={email} onChange={setEmail} />
      </Section>
      <Section title="Subscription">
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <div>
            <div style={{ fontWeight:600 }}>Freemium Plan</div>
            <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Up to 100 products, 50 orders/month</div>
          </div>
          <Btn onClick={() => window.open('mailto:sales@biashara360.co.ke?subject=Premium Plan Enquiry', '_blank')}>Upgrade to Premium →</Btn>
        </div>
      </Section>
    </div>
  )
}
