import React, { useState, useEffect } from 'react'
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Download, Share2, FileText, Table } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, ProgressBar, KpiCard, Modal, Input, Select } from '../components/ui'
import { expenseApi, paymentApi, orderApi, reportApi, ExpenseResponse, PaymentResponse, OrderResponse, ProfitSummaryResponse } from '../services/api'

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

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
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
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
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

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
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
              headers={['Mpesa Code', 'Customer', 'Phone', 'Amount', 'Status', 'Date']}
              rows={payments.map(p => [
                <span style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{p.mpesaTransactionCode}</span>,
                <span style={{ fontWeight:600 }}>{p.payerName}</span>,
                p.phoneNumber,
                <span style={{ fontWeight:700 }}>KES {p.amount.toLocaleString()}</span>,
                <StatusBadge status={p.isReconciled ? 'MATCHED' : 'PENDING'} />,
                new Date(p.createdAt).toLocaleDateString('en-KE')
              ])}
            />
          </Card>
        </>
      )}
    </div>
  )
}

// ── Reports ───────────────────────────────────────────────────────────────────
export function ReportsPage() {
  const [profitSummary, setProfitSummary] = useState<ProfitSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const { startDate, endDate } = getCurrentMonthRange()
    reportApi.profitSummary(startDate, endDate).then(res => {
      if (res.success && res.data) setProfitSummary(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const now = new Date()
  const monthLabel = now.toLocaleString('en-KE', { month: 'long', year: 'numeric' })

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Reports"
        action={
          <div style={{ display:'flex', gap:8 }}>
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
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
          {/* P&L */}
          <Card style={{ padding:20 }}>
            <h3 style={{ fontWeight:700, marginBottom:16 }}>Profit & Loss — {monthLabel}</h3>
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
            <h3 style={{ fontWeight:700, marginBottom:16 }}>Current Month Summary</h3>
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
export function UserCreationPage() {
  const emptyUser = { name: '', email: '', phone: '', role: 'STAFF', password: '' }
  const [users, setUsers] = useState([
    { id: '1', name: 'Wanjiru Kamau', email: 'wanjiru@example.com', phone: '+254712345678', role: 'ADMIN' },
    { id: '2', name: 'James Otieno', email: 'james@example.com', phone: '+254723456789', role: 'STAFF' },
  ])
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState(emptyUser)
  const [error, setError] = useState('')

  const f = (k: keyof typeof emptyUser) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const handleAdd = () => {
    if (!form.name || !form.email || !form.phone || !form.password) { setError('All fields are required.'); return }
    setUsers(prev => [...prev, { id: String(Date.now()), ...form }])
    setShowAdd(false); setForm(emptyUser); setError('')
  }

  const handleDelete = (id: string) => {
    if (!window.confirm('Remove this user?')) return
    setUsers(prev => prev.filter(u => u.id !== id))
  }

  const ROLES = [{ value: 'ADMIN', label: 'Admin' }, { value: 'MANAGER', label: 'Manager' }, { value: 'STAFF', label: 'Staff' }]

  const roleColor = (role: string) => role === 'ADMIN' ? 'PAID' : role === 'MANAGER' ? 'PENDING' : 'COD'

  return (
    <div className="fade-in">
      <PageHeader title="User Management" action={<Btn onClick={() => setShowAdd(true)} icon={<Plus size={14} />}>Add User</Btn>} />
      <Card style={{ padding: 0 }}>
        <DataTable
          headers={['Name', 'Email', 'Phone', 'Role', '']}
          rows={users.map(u => [
            u.name,
            u.email,
            u.phone,
            <StatusBadge key="role" status={roleColor(u.role)} />,
            <Btn key="del" variant="danger" small onClick={() => handleDelete(u.id)}>Remove</Btn>,
          ])}
        />
      </Card>
      {showAdd && (
        <Modal title="Add New User" onClose={() => { setShowAdd(false); setForm(emptyUser); setError('') }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Input label="Full Name" value={form.name} onChange={f('name')} placeholder="e.g. Jane Mwangi" />
            <Input label="Email" value={form.email} onChange={f('email')} placeholder="jane@example.com" />
            <Input label="Phone" value={form.phone} onChange={f('phone')} placeholder="+254 7XX XXX XXX" />
            <Input label="Password" value={form.password} onChange={f('password')} placeholder="Temporary password" />
            <Select label="Role" value={form.role} onChange={f('role')} options={ROLES} />
            {error && <div style={{ color: 'var(--b360-red)', fontSize: 13 }}>{error}</div>}
            <Btn onClick={handleAdd}>Create User</Btn>
          </div>
        </Modal>
      )}
    </div>
  )
}

// ── Business Profile ──────────────────────────────────────────────────────────
export function BusinessPage() {
  const [saved, setSaved] = useState(false)
  const [form, setForm] = useState({
    name: "Wanjiru's Fashion",
    owner: 'Wanjiru Kamau',
    phone: '+254 712 345 678',
    email: 'wanjiru@biashara360.co.ke',
    type: 'Retail',
    county: 'Nairobi',
    address: 'Tom Mboya Street, CBD',
    pin: 'P051234567A',
    paybill: '174379',
    accountNumber: '0745678',
  })

  const f = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(prev => ({ ...prev, [k]: e.target.value }))

  const handleSave = () => { setSaved(true); setTimeout(() => setSaved(false), 3000) }

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <h3 style={{ fontWeight: 700, marginBottom: 16, fontSize: 15 }}>{title}</h3>
      <div style={{ borderTop: '1px solid var(--b360-border)', paddingTop: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>{children}</div>
    </Card>
  )

  const Field = ({ label, field }: { label: string; field: keyof typeof form }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <span style={{ fontSize: 13, color: 'var(--b360-text-secondary)', width: 160 }}>{label}</span>
      <input
        value={form[field]}
        onChange={f(field)}
        style={{ flex: 1, maxWidth: 320, padding: '8px 12px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 13, fontFamily: 'inherit', outline: 'none' }}
      />
    </div>
  )

  return (
    <div className="fade-in" style={{ maxWidth: 680 }}>
      <PageHeader title="Business Profile" action={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {saved && <span style={{ fontSize: 12, color: 'var(--b360-green)', fontWeight: 600 }}>✓ Saved</span>}
          <Btn onClick={handleSave}>Save Changes</Btn>
        </div>
      } />
      <Section title="General Information">
        <Field label="Business Name"  field="name" />
        <Field label="Owner Name"     field="owner" />
        <Field label="Phone Number"   field="phone" />
        <Field label="Email Address"  field="email" />
        <Field label="Business Type"  field="type" />
        <Field label="County"         field="county" />
        <Field label="Address"        field="address" />
      </Section>
      <Section title="Tax & Compliance">
        <Field label="KRA PIN"        field="pin" />
      </Section>
      <Section title="Mpesa Integration">
        <Field label="Paybill Number"   field="paybill" />
        <Field label="Account Number"   field="accountNumber" />
      </Section>
    </div>
  )
}

export function SettingsPage() {
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

  const Field = ({ label, value }: { label: string; value: string }) => (
    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
      <span style={{ fontSize:13, color:'var(--b360-text-secondary)', width:160 }}>{label}</span>
      <input defaultValue={value} style={{ flex:1, maxWidth:300, padding:'8px 12px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, fontFamily:'inherit', outline:'none' }} />
    </div>
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
      <Section title="Business Profile">
        <Field label="Business Name"  value="Wanjiru's Fashion" />
        <Field label="Owner Phone"    value="+254 712 345 678" />
        <Field label="Business Type"  value="Retail" />
        <Field label="Mpesa Paybill"  value="174379" />
      </Section>
      <Section title="Security">
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
