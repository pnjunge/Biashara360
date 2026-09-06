import React, { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Share2, FileText, Table, Building2, Copy, ExternalLink, Mail, Printer } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, ProgressBar, KpiCard, Modal, Input, Select } from '../components/ui'
import { expenseApi, paymentApi, orderApi, reportApi, customerApi, ExpenseResponse, PaymentResponse, OrderResponse, ProfitSummaryResponse, PaymentReportResponse, OrderReportResponse, CustomerResponse, userApi, superAdminApi, businessApi, accessApi, AccessConfig, AuditLogResponse, BusinessResponse, BusinessProfileRequest, BusinessProfileResponse, UserResponse, InviteUserRequest } from '../services/api'
import { useAuth } from '../App'
import { ShareableReport, downloadReportCsv, emailReport, printReport, whatsappReport } from '../utils/reportShare'

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
  const initialRange = getCurrentMonthRange()
  const [startDate, setStartDate] = useState(initialRange.startDate)
  const [endDate, setEndDate] = useState(initialRange.endDate)
  const [methodFilter, setMethodFilter] = useState('ALL')
  const [channelFilter, setChannelFilter] = useState('ALL')

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

  const paymentMethods = ['ALL', ...Array.from(new Set(payments.map(p => p.method).filter(Boolean))).sort()]
  const paymentChannels = ['ALL', ...Array.from(new Set(payments.map(p => p.channel).filter(Boolean))).sort()]
  const filteredPayments = payments.filter(p => {
    const date = p.transactionDate?.slice(0, 10) || ''
    return (!startDate || date >= startDate) && (!endDate || date <= endDate)
      && (methodFilter === 'ALL' || p.method === methodFilter)
      && (channelFilter === 'ALL' || p.channel === channelFilter)
  })
  const unreconciled = filteredPayments.filter(p => !p.reconciled)
  const total = filteredPayments.filter(p => ['SUCCESS','COMPLETED','PAID'].includes(p.status)).reduce((s, p) => s + p.amount, 0)

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {matchPayment && (
        <Modal title="Match Payment to Order" onClose={() => setMatchPayment(null)}
          footer={<><Btn variant="secondary" onClick={() => setMatchPayment(null)}>Cancel</Btn><Btn onClick={handleMatch} disabled={matching || !selectedOrderId}>{matching ? 'Matching...' : 'Confirm Match'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {matchError && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{matchError}</p>}
            <div style={{ background:'var(--b360-surface)', borderRadius:8, padding:12 }}>
              <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Mpesa Transaction</div>
              <div style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)' }}>{matchPayment.transactionCode}</div>
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

      <PageHeader title="Payments" />
      {queryMessage && <div style={{ padding:12, background:'var(--b360-surface)', borderRadius:8, fontSize:13 }}>{queryMessage}</div>}

      <Card style={{padding:16}}>
        <div className="responsive-grid responsive-grid-4" style={{gap:12,alignItems:'end'}}>
          <label style={{display:'grid',gap:5,fontSize:12,color:'var(--b360-text-secondary)'}}>From
            <input type="date" value={startDate} max={endDate || undefined} onChange={e=>setStartDate(e.target.value)} style={{padding:'9px 10px',border:'1px solid var(--b360-border)',borderRadius:8}} />
          </label>
          <label style={{display:'grid',gap:5,fontSize:12,color:'var(--b360-text-secondary)'}}>To
            <input type="date" value={endDate} min={startDate || undefined} onChange={e=>setEndDate(e.target.value)} style={{padding:'9px 10px',border:'1px solid var(--b360-border)',borderRadius:8}} />
          </label>
          <Select label="Payment type" value={methodFilter} onChange={setMethodFilter} options={paymentMethods.map(value=>({value,label:value === 'ALL' ? 'All payment types' : value.replace(/_/g,' ')}))} />
          <Select label="Channel" value={channelFilter} onChange={setChannelFilter} options={paymentChannels.map(value=>({value,label:value === 'ALL' ? 'All channels' : value.replace(/_/g,' ')}))} />
        </div>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginTop:12,fontSize:12,color:'var(--b360-text-secondary)'}}>
          <span>{filteredPayments.length} of {payments.length} payments</span>
          <Btn small variant="secondary" onClick={()=>{const range=getCurrentMonthRange();setStartDate(range.startDate);setEndDate(range.endDate);setMethodFilter('ALL');setChannelFilter('ALL')}}>Reset filters</Btn>
        </div>
      </Card>

      <div className="responsive-grid responsive-grid-4" style={{ gap:12 }}>
        <KpiCard title="Total Collected"    value={`KES ${total.toLocaleString()}`} change="Reconciled payments" icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Unreconciled"       value={`${unreconciled.length} txns`}   change="Need matching"      icon={<FileText size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Transactions"       value={`${filteredPayments.length}`}     change="Filtered period"    icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Pending"            value={`${unreconciled.length}`}         change="Awaiting reconciliation" icon={<FileText size={18}/>} color="var(--b360-red)" />
      </div>

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : filteredPayments.length === 0 ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No payments match the selected filters</div>
      ) : (
        <>
          {unreconciled.length > 0 && (
            <div>
              <h3 style={{ fontWeight:700, marginBottom:10, fontSize:14 }}>⚠️ Needs Reconciliation</h3>
              <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
                {unreconciled.map(p => (
                  <div key={p.id} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'12px 16px', background:'var(--b360-amber-bg)', borderRadius:10, border:'1px solid var(--b360-amber)' }}>
                    <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                      <div style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:13 }}>{p.transactionCode}</div>
                      <div>
                        <div style={{ fontWeight:600 }}>{p.payerName}</div>
                        <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>{p.payerPhone || 'Phone unavailable'} · {p.method}</div>
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
              headers={['Transaction', 'Customer', 'Phone', 'Type', 'Channel', 'Amount', 'Payment Status', 'Reconciliation', 'Date', 'Action']}
              rows={filteredPayments.map(p => [
                <span style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{p.transactionCode || '—'}</span>,
                <span style={{ fontWeight:600 }}>{p.payerName || 'Customer unavailable'}</span>,
                p.payerPhone || '—',
                <span style={{fontWeight:700}}>{p.method || '—'}</span>,
                (p.channel || '—').replace(/_/g,' '),
                <span style={{ fontWeight:700 }}>KES {p.amount.toLocaleString()}</span>,
                <StatusBadge status={p.status} />,
                <StatusBadge status={p.reconciled ? 'MATCHED' : 'PENDING'} />,
                Number.isNaN(Date.parse(p.transactionDate)) ? 'Date unavailable' : new Date(p.transactionDate).toLocaleString('en-KE'),
                p.method === 'MPESA' ? (
                  <Btn variant="secondary" small disabled={!p.transactionCode || queryingId === p.transactionCode}
                    onClick={() => p.transactionCode && queryTransaction(p.transactionCode)}>
                    {queryingId === p.transactionCode ? 'Querying…' : 'Query M-Pesa'}
                  </Btn>
                ) : <span style={{ color:'var(--b360-text-secondary)' }}>—</span>
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
  const [paymentReport, setPaymentReport] = useState<PaymentReportResponse | null>(null)
  const [orderReport, setOrderReport] = useState<OrderReportResponse | null>(null)
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([])
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [businessName, setBusinessName] = useState('Biashara360 Business')
  const [loading, setLoading] = useState(true)
  const [period, setPeriod] = useState<string>('This Month')
  const [reportType, setReportType] = useState('SALES')

  const loadReport = (selectedPeriod: string) => {
    setLoading(true)
    const { startDate, endDate } = getPeriodRange(selectedPeriod)
    Promise.allSettled([
      reportApi.profitSummary(startDate, endDate),
      reportApi.payments(startDate, endDate),
      reportApi.orders(startDate, endDate),
      expenseApi.list(undefined, startDate, endDate),
      customerApi.list(),
      businessApi.getProfile(),
    ]).then(([profitResult, paymentResult, orderResult, expenseResult, customerResult, profileResult]) => {
      const profit = profitResult.status === 'fulfilled' ? profitResult.value : null
      const payments = paymentResult.status === 'fulfilled' ? paymentResult.value : null
      const orders = orderResult.status === 'fulfilled' ? orderResult.value : null
      const expenseResponse = expenseResult.status === 'fulfilled' ? expenseResult.value : null
      const customerResponse = customerResult.status === 'fulfilled' ? customerResult.value : null
      const profileResponse = profileResult.status === 'fulfilled' ? profileResult.value : null
      setProfitSummary(profit?.success ? profit.data : null)
      setPaymentReport(payments?.success ? payments.data : null)
      setOrderReport(orders?.success ? orders.data : null)
      setExpenses(expenseResponse?.success && expenseResponse.data ? expenseResponse.data : [])
      setCustomers(customerResponse?.success && customerResponse.data ? customerResponse.data : [])
      if (profileResponse?.success && profileResponse.data) setBusinessName(profileResponse.data.name)
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    loadReport(period)
  }, [period])

  const selectedReport = useMemo<ShareableReport | null>(() => {
    const money = (value:number) => `KES ${value.toLocaleString('en-KE', {minimumFractionDigits:2,maximumFractionDigits:2})}`
    const base = { period, businessName }
    const paymentRows = (method:string) => paymentReport?.payments.filter(payment => payment.method.toUpperCase() === method) ?? []
    const paymentDocument = (method:string,title:string):ShareableReport => {
      const rows=paymentRows(method); const amount=rows.filter(row=>row.status==='SUCCESS').reduce((sum,row)=>sum+row.amount,0)
      return {...base,title,summary:[['Transactions',String(rows.length)],['Successful amount',money(amount)]],columns:['Transaction','Payer','Phone','Channel','Status','Amount','Date'],rows:rows.map(row=>[row.transactionCode,row.payerName,row.payerPhone,row.channel,row.status,row.amount,row.transactionDate])}
    }
    if(reportType==='MPESA')return paymentReport ? paymentDocument('MPESA','M-Pesa Payment Report') : null
    if(reportType==='CARD')return paymentReport ? paymentDocument('CARD','Card Payment Report') : null
    if(reportType==='CASH')return paymentReport ? paymentDocument('CASH','Cash Payment Report') : null
    if(reportType==='EXPENSES')return {...base,title:'Expense Report',summary:[['Expenses',String(expenses.length)],['Total',money(expenses.reduce((sum,item)=>sum+item.amount,0))]],columns:['Date','Category','Description','Amount'],rows:expenses.map(item=>[item.expenseDate,item.category,item.description,item.amount])}
    if(reportType==='CUSTOMERS'){
      const {startDate,endDate}=getPeriodRange(period); const rows=customers.filter(customer=>customer.createdAt.slice(0,10)>=startDate&&customer.createdAt.slice(0,10)<=endDate)
      return {...base,title:'Customer Report',summary:[['New customers',String(rows.length)],['Total spent',money(rows.reduce((sum,item)=>sum+item.totalSpent,0))],['Repeat customers',String(rows.filter(item=>item.isRepeatCustomer).length)]],columns:['Customer','Phone','Email','Location','Orders','Total Spent','Loyalty Points','Joined'],rows:rows.map(item=>[item.name,item.phone,item.email,item.location,item.totalOrders,item.totalSpent,item.loyaltyPoints,item.createdAt])}
    }
    if(reportType==='ORDERS')return orderReport ? {...base,title:'Order Report',summary:[['Orders',String(orderReport.totalOrders)],['Order value',money(orderReport.totalValue)],['Paid value',money(orderReport.paidValue)]],columns:['Order','Customer','Method','Channel','Payment','Fulfilment / Tab','Value','Date'],rows:orderReport.orders.map(item=>[item.orderNumber,item.customerName,item.paymentMethod,item.salesChannel,item.paymentStatus,item.serviceType === 'RETAIL' ? item.deliveryStatus : item.tabStatus,item.subtotal,item.createdAt])} : null
    if(reportType==='REVENUE')return profitSummary ? {...base,title:'Revenue & Profit Report',summary:[['Revenue',money(profitSummary.totalRevenue)],['Gross profit',money(profitSummary.grossProfit)],['Expenses',money(profitSummary.totalExpenses)],['Net profit',money(profitSummary.netProfit)]],columns:['Metric','Value'],rows:[['Revenue',profitSummary.totalRevenue],['Cost of goods',profitSummary.totalCostOfGoods],['Gross profit',profitSummary.grossProfit],['Expenses',profitSummary.totalExpenses],['Net profit',profitSummary.netProfit],['Net margin',`${profitSummary.netMargin.toFixed(1)}%`]]} : null
    if (!orderReport) return null
    const sales=orderReport.orders.filter(item=>item.paymentStatus==='PAID')
    return {...base,title:'Sales Report',summary:[['Paid sales',String(sales.length)],['Sales revenue',money(sales.reduce((sum,item)=>sum+item.subtotal,0))]],columns:['Sale','Customer','Method','Channel','Amount','Date'],rows:sales.map(item=>[item.orderNumber,item.customerName,item.paymentMethod,item.salesChannel,item.subtotal,item.createdAt])}
  },[reportType,period,businessName,profitSummary,paymentReport,orderReport,expenses,customers])

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Reports"
        action={
          <div style={{ display:'flex', gap:8, alignItems:'center', flexWrap:'wrap' }}>
            <select value={reportType} onChange={e=>setReportType(e.target.value)} aria-label="Report type" style={{padding:'8px 12px',border:'1px solid var(--b360-border)',borderRadius:8,fontSize:13,fontWeight:600,background:'white'}}>
              <option value="SALES">Sales</option><option value="EXPENSES">Expenses</option><option value="CUSTOMERS">Customers</option><option value="MPESA">M-Pesa</option><option value="CARD">Card</option><option value="CASH">Cash</option><option value="ORDERS">Orders</option><option value="REVENUE">Revenue</option>
            </select>
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
            <Btn variant="secondary" icon={<Printer size={14}/>} disabled={!selectedReport||loading} onClick={()=>selectedReport&&printReport(selectedReport)}>Print</Btn>
            <Btn variant="secondary" icon={<Table size={14}/>} disabled={!selectedReport||loading} onClick={()=>selectedReport&&downloadReportCsv(selectedReport)}>CSV</Btn>
            <Btn variant="secondary" icon={<Mail size={14}/>} disabled={!selectedReport||loading} onClick={()=>selectedReport&&emailReport(selectedReport)}>Email</Btn>
            <Btn variant="secondary" icon={<Share2 size={14}/>} disabled={!selectedReport||loading} onClick={()=>selectedReport&&whatsappReport(selectedReport)}>WhatsApp</Btn>
          </div>
        }
      />

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : selectedReport ? (
        <Card style={{ padding:20 }}>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'start', gap:12, flexWrap:'wrap', marginBottom:16 }}>
            <div>
              <h3 style={{ margin:0, fontWeight:700 }}>{selectedReport.title}</h3>
              <div style={{ color:'var(--b360-text-secondary)', fontSize:12, marginTop:4 }}>{period} · {selectedReport.rows.length} detailed row{selectedReport.rows.length === 1 ? '' : 's'}</div>
            </div>
            <div style={{ display:'flex', gap:8, flexWrap:'wrap' }}>
              {selectedReport.summary.map(([label, value]) => (
                <div key={label} style={{ minWidth:120, padding:'8px 12px', background:'var(--b360-surface)', borderRadius:8 }}>
                  <div style={{ color:'var(--b360-text-secondary)', fontSize:11 }}>{label}</div>
                  <strong style={{ fontSize:13 }}>{value}</strong>
                </div>
              ))}
            </div>
          </div>
          {selectedReport.rows.length ? (
            <DataTable headers={selectedReport.columns} rows={selectedReport.rows.map(row => row.map((value, index) => {
              const column = selectedReport.columns[index].toLowerCase()
              if (typeof value === 'number' && (column.includes('amount') || column.includes('value') || column.includes('spent'))) return `KES ${value.toLocaleString('en-KE')}`
              if (typeof value === 'string' && (column === 'date' || column === 'joined') && !Number.isNaN(Date.parse(value))) return new Date(value).toLocaleString('en-KE')
              return String(value ?? '—')
            }))} />
          ) : <div style={{ padding:32, textAlign:'center', color:'var(--b360-text-secondary)' }}>No {selectedReport.title.toLowerCase()} data for {period.toLowerCase()}.</div>}
        </Card>
      ) : (
        <Card style={{ padding:32, textAlign:'center', color:'var(--b360-text-secondary)' }}>
          This report could not be loaded. Refresh the page or choose another period.
        </Card>
      )}

      {!loading && reportType === 'REVENUE' && (
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
                  <span style={{ color:'var(--b360-blue)', fontWeight:600, fontSize:12 }}>{profitSummary.netMargin.toFixed(1)}%</span>
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
                  ['Gross Profit',   profitSummary.grossProfit,  `${profitSummary.grossMargin.toFixed(1)}% margin`, 'var(--b360-green)'],
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

      {!loading && ['MPESA','CARD','CASH'].includes(reportType) && paymentReport && (
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, margin:'0 0 16px' }}>Payment Report — {period}</h3>
          <div className="responsive-grid responsive-grid-3" style={{ gap:12, marginBottom:18 }}>
            <KpiCard title="Transactions" value={String(paymentReport.totalTransactions)} change="Recorded payments" icon={<FileText size={18} />} color="var(--b360-blue)" />
            <KpiCard title="Collected" value={`KES ${paymentReport.totalAmount.toLocaleString()}`} change="Successful payments" icon={<FileText size={18} />} color="var(--b360-green)" />
            <KpiCard title="Reconciled" value={`KES ${paymentReport.reconciledAmount.toLocaleString()}`} change="Matched to orders" icon={<FileText size={18} />} color="var(--b360-amber)" />
          </div>
          <div className="responsive-grid responsive-grid-2" style={{ gap:16, marginBottom:18 }}>
            {[['By payment method', paymentReport.byMethod], ['By payment channel', paymentReport.byChannel]].map(([title, values]) => (
              <div key={title as string} style={{ background:'var(--b360-surface)', padding:14, borderRadius:10 }}>
                <strong style={{ fontSize:13 }}>{title as string}</strong>
                {(values as PaymentReportResponse['byMethod']).map(value => <div key={value.label} style={{ display:'flex', justifyContent:'space-between', marginTop:9, fontSize:12 }}><span>{value.label} ({value.count})</span><b>KES {value.amount.toLocaleString()}</b></div>)}
              </div>
            ))}
          </div>
          {paymentReport.payments.length ? <DataTable headers={['Transaction', 'Payer', 'Method', 'Channel', 'Status', 'Amount', 'Date']} rows={paymentReport.payments.map(payment => [
            <span style={{ fontFamily:'monospace', fontWeight:700 }}>{payment.transactionCode}</span>,
            <div><strong>{payment.payerName}</strong><div style={{ fontSize:10, color:'var(--b360-text-secondary)' }}>{payment.payerPhone}</div></div>,
            payment.method,
            payment.channel,
            <StatusBadge status={payment.status} />,
            <strong>KES {payment.amount.toLocaleString()}</strong>,
            new Date(payment.transactionDate).toLocaleString('en-KE'),
          ])} /> : <div style={{ color:'var(--b360-text-secondary)', fontSize:13 }}>No payments in this period.</div>}
        </Card>
      )}

      {!loading && reportType === 'ORDERS' && orderReport && (
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, margin:'0 0 16px' }}>Order Report — {period}</h3>
          <div className="responsive-grid responsive-grid-3" style={{ gap:12, marginBottom:18 }}>
            <KpiCard title="Orders" value={String(orderReport.totalOrders)} change="Created in period" icon={<FileText size={18} />} color="var(--b360-blue)" />
            <KpiCard title="Order Value" value={`KES ${orderReport.totalValue.toLocaleString()}`} change="All payment statuses" icon={<FileText size={18} />} color="var(--b360-amber)" />
            <KpiCard title="Paid Value" value={`KES ${orderReport.paidValue.toLocaleString()}`} change="Paid orders" icon={<FileText size={18} />} color="var(--b360-green)" />
          </div>
          <div className="responsive-grid responsive-grid-2" style={{ gap:16, marginBottom:18 }}>
            {[['By payment method', orderReport.byPaymentMethod], ['By order channel', orderReport.byChannel]].map(([title, values]) => (
              <div key={title as string} style={{ background:'var(--b360-surface)', padding:14, borderRadius:10 }}>
                <strong style={{ fontSize:13 }}>{title as string}</strong>
                {(values as OrderReportResponse['byChannel']).map(value => <div key={value.label} style={{ display:'flex', justifyContent:'space-between', marginTop:9, fontSize:12 }}><span>{value.label} ({value.count})</span><b>KES {value.amount.toLocaleString()}</b></div>)}
              </div>
            ))}
          </div>
          {orderReport.orders.length ? <DataTable headers={['Order', 'Customer', 'Payment Method', 'Order Channel', 'Payment', 'Fulfilment / Tab', 'Value', 'Date']} rows={orderReport.orders.map(order => [
            <span style={{ fontFamily:'monospace', fontWeight:700 }}>{order.orderNumber}</span>,
            order.customerName,
            order.paymentMethod,
            order.salesChannel,
            <StatusBadge status={order.paymentStatus} />,
            <StatusBadge status={order.serviceType === 'RETAIL' ? order.deliveryStatus : order.tabStatus} />,
            <strong>KES {order.subtotal.toLocaleString()}</strong>,
            new Date(order.createdAt).toLocaleString('en-KE'),
          ])} /> : <div style={{ color:'var(--b360-text-secondary)', fontSize:13 }}>No orders in this period.</div>}
        </Card>
      )}
    </div>
  )
}

// ── Settings ──────────────────────────────────────────────────────────────────
// ── User Creation ─────────────────────────────────────────────────────────────
const emptyBusinessAdmin = { businessName: '', businessType: '', adminName: '', adminEmail: '', adminPhone: '', adminPassword: '' }
const emptyUser: InviteUserRequest = { name: '', email: '', phone: '', role: 'STAFF' }

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
  const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([])
  const [auditLoading, setAuditLoading] = useState(false)
  const [showAuditLog, setShowAuditLog] = useState(false)
  const [accessConfig, setAccessConfig] = useState<AccessConfig | null>(null)
  const [accessMessage, setAccessMessage] = useState('')
  const [roleDraft, setRoleDraft] = useState({ name:'', description:'', allowedMenus:[] as string[] })
  const [groupDraft, setGroupDraft] = useState({ name:'', description:'', roleIds:[] as string[] })
  const [editingRoleId, setEditingRoleId] = useState<string | null>(null)
  const [editingGroupId, setEditingGroupId] = useState<string | null>(null)
  const [inviteGroupId, setInviteGroupId] = useState('')
  const [accessSaving, setAccessSaving] = useState<'MENUS'|'ROLE'|'GROUP'|'INVITE_GROUP'|null>(null)

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

  const loadAccess = () => {
    if (isSuperAdmin) return
    return accessApi.config().then(res => { if (res.success && res.data) setAccessConfig(res.data) }).catch((e:any) => setAccessMessage(e.response?.data?.message || 'Could not load roles and groups.'))
  }

  const loadAuditLogs = () => {
    if (isSuperAdmin && !selectedBusinessId) {
      setAuditLogs([])
      return
    }
    setAuditLoading(true)
    userApi.auditLogs(100, isSuperAdmin ? selectedBusinessId : undefined)
      .then(res => { if (res.success && res.data) setAuditLogs(res.data) })
      .catch(() => {})
      .finally(() => setAuditLoading(false))
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
    loadAccess()
    loadAuditLogs()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSuperAdmin, selectedBusinessId])

  const toggleValue = (values: string[], value: string) => values.includes(value) ? values.filter(v => v !== value) : [...values, value]
  const saveMenus = async () => {
    if (!accessConfig) return
    setAccessSaving('MENUS'); setAccessMessage('')
    try {
      const res = await accessApi.updateMenus(accessConfig.enabledMenus)
      if (res.success && res.data) { setAccessConfig(res.data); setAccessMessage('Menu availability saved.') }
      else setAccessMessage(res.message || 'Could not save menu availability.')
    } catch (e:any) { setAccessMessage(e.response?.data?.message || 'Could not save menu availability.') }
    finally { setAccessSaving(null) }
  }
  const createAccessRole = async () => {
    if (!roleDraft.name.trim()) return setAccessMessage('Enter a role name.')
    if (!roleDraft.allowedMenus.length) return setAccessMessage('Select at least one menu for this role.')
    setAccessSaving('ROLE'); setAccessMessage('')
    try {
      const current = editingRoleId ? accessConfig?.roles.find(role => role.id === editingRoleId) : undefined
      const res = editingRoleId
        ? await accessApi.updateRole(editingRoleId, { ...roleDraft, isActive: current?.isActive ?? true })
        : await accessApi.createRole(roleDraft)
      if (res.success) {
        setRoleDraft({name:'',description:'',allowedMenus:[]})
        setEditingRoleId(null)
        await loadAccess()
        setAccessMessage(editingRoleId ? 'Role updated.' : 'Role created.')
      } else setAccessMessage(res.message || (editingRoleId ? 'Could not update role.' : 'Could not create role.'))
    } catch (e:any) { setAccessMessage(e.response?.data?.message || (editingRoleId ? 'Could not update role.' : 'Could not create role.')) }
    finally { setAccessSaving(null) }
  }
  const createAccessGroup = async () => {
    if (!groupDraft.name.trim()) return setAccessMessage('Enter a group name.')
    if (!groupDraft.roleIds.length) return setAccessMessage('Select at least one role for this group.')
    setAccessSaving('GROUP'); setAccessMessage('')
    try {
      const current = editingGroupId ? accessConfig?.groups.find(group => group.id === editingGroupId) : undefined
      const res = editingGroupId
        ? await accessApi.updateGroup(editingGroupId, { ...groupDraft, isActive: current?.isActive ?? true })
        : await accessApi.createGroup(groupDraft)
      if (res.success) {
        setGroupDraft({name:'',description:'',roleIds:[]})
        setEditingGroupId(null)
        await loadAccess()
        setAccessMessage(editingGroupId ? 'Group updated.' : 'Group created.')
      } else setAccessMessage(res.message || (editingGroupId ? 'Could not update group.' : 'Could not create group.'))
    } catch (e:any) { setAccessMessage(e.response?.data?.message || (editingGroupId ? 'Could not update group.' : 'Could not create group.')) }
    finally { setAccessSaving(null) }
  }
  const toggleGroupUser = async (groupId: string, current: string[], userId: string) => {
    setAccessMessage('')
    try {
      const res = await accessApi.assignUsers(groupId, toggleValue(current, userId))
      if (res.success) await loadAccess(); else setAccessMessage(res.message || 'Could not update group members.')
    } catch (e:any) { setAccessMessage(e.response?.data?.message || 'Could not update group members.') }
  }
  const updateAccessRole = async (role: AccessConfig['roles'][number], isActive = role.isActive) => {
    setAccessMessage('')
    try {
      const res = await accessApi.updateRole(role.id, {...role,isActive})
      if (res.success) await loadAccess(); else setAccessMessage(res.message || 'Could not update role.')
    } catch (e:any) { setAccessMessage(e.response?.data?.message || 'Could not update role.') }
  }
  const updateAccessGroup = async (group: AccessConfig['groups'][number], isActive = group.isActive) => {
    setAccessMessage('')
    try {
      const res = await accessApi.updateGroup(group.id, {...group,isActive})
      if (res.success) await loadAccess(); else setAccessMessage(res.message || 'Could not update group.')
    } catch (e:any) { setAccessMessage(e.response?.data?.message || 'Could not update group.') }
  }

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
    if (!form.name || !form.email || !form.phone) { setError('Name, email, and phone are required.'); return }
    if (isSuperAdmin && !selectedBusinessId) { setError('Please select a business.'); return }
    setSaving(true); setError('')
    try {
      const res = await userApi.invite(form, isSuperAdmin ? selectedBusinessId : undefined)
      if (res.success && res.data) {
        let inviteMessage = ''
        if (inviteGroupId && accessConfig) {
          const group = accessConfig.groups.find(item => item.id === inviteGroupId)
          if (group) {
            try {
              setAccessSaving('INVITE_GROUP')
              const groupRes = await accessApi.assignUsers(group.id, [...group.userIds, res.data.id])
              if (!groupRes.success) inviteMessage = `User created, but group assignment failed: ${groupRes.message || 'try again from Access groups.'}`
              await loadAccess()
            } catch (e:any) {
              inviteMessage = `User created, but group assignment failed: ${e.response?.data?.message || 'try again from Access groups.'}`
            } finally { setAccessSaving(null) }
          }
        }
        setShowAdd(false)
        setForm(emptyUser)
        setInviteGroupId('')
        if (!isSuperAdmin) setSelectedBusinessId('')
        loadUsers()
        if (inviteMessage) setError(inviteMessage)
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
    setError('')
    try {
      const res = await userApi.setStatus(id, isActive, isSuperAdmin ? selectedBusinessId : undefined)
      if (!res.success || !res.data) return setError(res.message || 'Could not update user status.')
      setUsers(prev => prev.map(u => (u.id === id ? res.data! : u)))
    } catch (e:any) { setError(e.response?.data?.message || 'Could not update user status.') }
  }

  const handleUserRoleChange = async (id: string, role: string) => {
    setError('')
    try {
      const res = await userApi.updateRole(id, role, isSuperAdmin ? selectedBusinessId : undefined)
      if (!res.success || !res.data) return setError(res.message || 'Could not update user role.')
      setUsers(prev => prev.map(u => (u.id === id ? res.data! : u)))
    } catch (e:any) { setError(e.response?.data?.message || 'Could not update user role.') }
  }

  const handleToggleBusinessStatus = async (id: string, isActive: boolean) => {
    if (!isActive && !window.confirm('Disable this business account? All merchant access will stop immediately.')) return
    const res = await superAdminApi.setBusinessStatus(id, { isActive })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === id ? res.data! : b)))
    }
  }

  const handleSubscriptionChange = async (business: BusinessResponse, enabled: boolean, tier = business.subscriptionTier) => {
    if (!enabled && !window.confirm(`Disable ${business.name}'s subscription? Existing sessions will stop working immediately.`)) return
    const res = await superAdminApi.updateSubscription(business.id, {
      enabled,
      tier: tier === 'PREMIUM' ? 'PREMIUM' : 'FREEMIUM',
    })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === business.id ? res.data! : b)))
    }
  }

  const ROLES = [{ value: 'ADMIN', label: 'Admin' }, { value: 'MANAGER', label: 'Manager' }, { value: 'STAFF', label: 'Staff' }]
  const activeGroups = accessConfig?.groups.filter(group => group.isActive) ?? []

  return (
    <div className="fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {showAuditLog && (
        <Modal title="User & access audit log" wide onClose={() => setShowAuditLog(false)}>
          {auditLoading ? (
            <div style={{padding:24,textAlign:'center',color:'var(--b360-text-secondary)'}}>Loading audit events…</div>
          ) : auditLogs.length === 0 ? (
            <div style={{padding:24,textAlign:'center',color:'var(--b360-text-secondary)'}}>No user or access changes have been recorded.</div>
          ) : (
            <DataTable
              headers={['When','Action','Actor','Target','Details']}
              rows={auditLogs.map(log => [
                new Date(log.createdAt).toLocaleString('en-KE'),
                <StatusBadge key="action" status={log.action.replace(/_/g, ' ')} />,
                log.actorName || 'System',
                log.targetName || '—',
                log.details || '—',
              ])}
            />
          )}
        </Modal>
      )}

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
        <Modal title="Add New User" onClose={() => { setShowAdd(false); setForm(emptyUser); setInviteGroupId(''); setSelectedBusinessId(''); setError('') }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Input label="Full Name" value={form.name} onChange={f('name')} placeholder="e.g. Jane Mwangi" />
            <Input label="Email" value={form.email} onChange={f('email')} placeholder="jane@example.com" />
            <Input label="Phone" value={form.phone} onChange={f('phone')} placeholder="+254 7XX XXX XXX" />
            <div style={{ color: 'var(--b360-text-secondary)', fontSize: 13 }}>
              The user will receive a one-time password setup code by email. It expires after 10 minutes.
            </div>
            <Select label="Role" value={form.role ?? 'STAFF'} onChange={f('role')} options={ROLES} />
            {!isSuperAdmin && activeGroups.length > 0 && (
              <Select
                label="Access group (optional)"
                value={inviteGroupId}
                onChange={setInviteGroupId}
                options={[{ value: '', label: 'No group — use default staff access' }, ...activeGroups.map(group => ({ value: group.id, label: group.name }))]}
              />
            )}
            {!isSuperAdmin && activeGroups.length > 0 && <div style={{ color: 'var(--b360-text-secondary)', fontSize: 12 }}>The account role controls sign-in authority. Access groups control which business areas the user can open.</div>}
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
            <Btn onClick={handleAdd} disabled={saving || accessSaving !== null}>{saving ? 'Sending...' : 'Send Invitation'}</Btn>
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
                headers={['Business Name', 'Type', 'Tier', 'Subscription', 'Business', 'Created', 'Actions']}
                rows={businesses.map(b => [
                  b.name,
                  b.type,
                  <select
                    key="tier"
                    value={b.subscriptionTier}
                    onChange={event => handleSubscriptionChange(b, b.subscriptionEnabled, event.target.value)}
                    style={{ padding: '6px 8px', borderRadius: 7, fontSize: 12 }}
                  >
                    <option value="FREEMIUM">Freemium</option>
                    <option value="PREMIUM">Premium</option>
                  </select>,
                  <StatusBadge key="subscription" status={b.subscriptionEnabled ? 'ACTIVE' : 'INACTIVE'} />,
                  <StatusBadge key="status" status={b.isActive ? 'ACTIVE' : 'INACTIVE'} />,
                  new Date(b.createdAt).toLocaleDateString(),
                  <div key="actions" style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    <Btn variant={b.subscriptionEnabled ? 'danger' : 'secondary'} small onClick={() => handleSubscriptionChange(b, !b.subscriptionEnabled)}>
                      {b.subscriptionEnabled ? 'Disable subscription' : 'Enable subscription'}
                    </Btn>
                    <Btn variant={b.isActive ? 'danger' : 'secondary'} small onClick={() => handleToggleBusinessStatus(b.id, !b.isActive)}>
                      {b.isActive ? 'Disable business' : 'Enable business'}
                    </Btn>
                  </div>,
                ])}
              />
            )}
          </Card>
        </>
      )}

      {!isSuperAdmin && accessConfig && (
        <>
          <PageHeader title="Menus, Roles & Groups" />
          {accessMessage && <div style={{fontSize:13,color:'var(--b360-blue)'}}>{accessMessage}</div>}
          <div style={{fontSize:12,color:'var(--b360-text-secondary)',background:'var(--b360-surface)',border:'1px solid var(--b360-border)',borderRadius:8,padding:'10px 12px'}}>Account roles control authentication and administrative authority. Custom access roles are bundled into groups, then assigned to users to control menu access.</div>
          <div className="responsive-grid responsive-grid-2" style={{gap:16}}>
            <Card style={{padding:20}}>
              <h3 style={{margin:'0 0 6px'}}>Business menus</h3>
              <p style={{fontSize:12,color:'var(--b360-text-secondary)'}}>Disabled menus are hidden for everyone in this business.</p>
              <div style={{display:'grid',gridTemplateColumns:'repeat(2,minmax(0,1fr))',gap:8,margin:'14px 0'}}>
                {accessConfig.menus.map(menu => <label key={menu.key} style={{fontSize:12,display:'flex',gap:7,alignItems:'center'}}><input type="checkbox" checked={accessConfig.enabledMenus.includes(menu.key)} onChange={() => setAccessConfig({...accessConfig,enabledMenus:toggleValue(accessConfig.enabledMenus,menu.key)})}/>{menu.label}</label>)}
              </div>
              <Btn small disabled={accessSaving!==null} onClick={saveMenus}>{accessSaving==='MENUS'?'Saving…':'Save menu availability'}</Btn>
            </Card>
            <Card style={{padding:20}}>
              <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',gap:12}}><h3 style={{margin:'0 0 12px'}}>{editingRoleId ? 'Edit role' : 'Create role'}</h3>{editingRoleId && <Btn small variant="secondary" onClick={()=>{setEditingRoleId(null);setRoleDraft({name:'',description:'',allowedMenus:[]})}}>Cancel</Btn>}</div>
              <Input label="Role name" value={roleDraft.name} onChange={v=>setRoleDraft({...roleDraft,name:v})} placeholder="e.g. Cashier" />
              <Input label="Description" value={roleDraft.description} onChange={v=>setRoleDraft({...roleDraft,description:v})} placeholder="What this role is for" />
              <div style={{display:'grid',gridTemplateColumns:'repeat(2,minmax(0,1fr))',gap:7,margin:'12px 0'}}>{accessConfig.menus.map(menu=><label key={menu.key} style={{fontSize:12}}><input type="checkbox" checked={roleDraft.allowedMenus.includes(menu.key)} onChange={()=>setRoleDraft({...roleDraft,allowedMenus:toggleValue(roleDraft.allowedMenus,menu.key)})}/> {menu.label}</label>)}</div>
              <Btn small disabled={accessSaving!==null} onClick={createAccessRole}>{accessSaving==='ROLE'?(editingRoleId?'Saving…':'Creating…'):(editingRoleId?'Save role':'Create role')}</Btn>
            </Card>
          </div>
          <Card style={{padding:20}}>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',gap:12}}><h3 style={{margin:'0 0 12px'}}>{editingGroupId ? 'Edit group' : 'Create group'}</h3>{editingGroupId && <Btn small variant="secondary" onClick={()=>{setEditingGroupId(null);setGroupDraft({name:'',description:'',roleIds:[]})}}>Cancel</Btn>}</div>
            <div className="responsive-grid responsive-grid-2" style={{gap:12}}><Input label="Group name" value={groupDraft.name} onChange={v=>setGroupDraft({...groupDraft,name:v})} placeholder="e.g. Front Desk"/><Input label="Description" value={groupDraft.description} onChange={v=>setGroupDraft({...groupDraft,description:v})} placeholder="Team description"/></div>
            <div style={{display:'flex',gap:14,flexWrap:'wrap',margin:'12px 0'}}>{accessConfig.roles.map(role=><label key={role.id} style={{fontSize:12}}><input type="checkbox" checked={groupDraft.roleIds.includes(role.id)} onChange={()=>setGroupDraft({...groupDraft,roleIds:toggleValue(groupDraft.roleIds,role.id)})}/> {role.name}</label>)}</div>
            <Btn small disabled={accessSaving!==null} onClick={createAccessGroup}>{accessSaving==='GROUP'?(editingGroupId?'Saving…':'Creating…'):(editingGroupId?'Save group':'Create group')}</Btn>
          </Card>
          <Card style={{padding:20}}><h3 style={{margin:'0 0 12px'}}>Existing roles</h3>{accessConfig.roles.map(role=><div key={role.id} style={{display:'flex',justifyContent:'space-between',gap:12,padding:'10px 0',borderTop:'1px solid var(--b360-border)'}}><div><b>{role.name}</b><div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>{role.allowedMenus.length} menus · {role.isActive?'Active':'Disabled'}</div></div><div style={{display:'flex',gap:6}}><Btn small variant="secondary" onClick={()=>{setEditingRoleId(role.id);setRoleDraft({name:role.name,description:role.description,allowedMenus:role.allowedMenus});window.scrollTo({top:0,behavior:'smooth'})}}>Edit</Btn><Btn small variant="secondary" onClick={()=>updateAccessRole(role,!role.isActive)}>{role.isActive?'Disable':'Enable'}</Btn></div></div>)}</Card>
          {accessConfig.groups.map(group => <Card key={group.id} style={{padding:16,opacity:group.isActive?1:.65}}><div style={{display:'flex',justifyContent:'space-between',gap:12}}><div><div style={{fontWeight:700}}>{group.name}</div><div style={{fontSize:12,color:'var(--b360-text-secondary)'}}>{group.description || 'No description'} · Roles: {accessConfig.roles.filter(r=>group.roleIds.includes(r.id)).map(r=>r.name).join(', ') || 'None'}</div></div><div style={{display:'flex',gap:6}}><Btn small variant="secondary" onClick={()=>{setEditingGroupId(group.id);setGroupDraft({name:group.name,description:group.description,roleIds:group.roleIds});window.scrollTo({top:0,behavior:'smooth'})}}>Edit</Btn><Btn small variant="secondary" onClick={()=>updateAccessGroup(group,!group.isActive)}>{group.isActive?'Disable':'Enable'}</Btn></div></div><div style={{display:'flex',gap:12,flexWrap:'wrap',marginTop:10}}>{users.map(member=><label key={member.id} style={{fontSize:12}}><input type="checkbox" disabled={!group.isActive} checked={group.userIds.includes(member.id)} onChange={()=>toggleGroupUser(group.id,group.userIds,member.id)}/> {member.name}</label>)}</div></Card>)}
        </>
      )}

      {/* ── User Management ── */}
      <PageHeader title="User Management" action={<div style={{display:'flex',gap:8}}><Btn variant="secondary" onClick={() => { loadAuditLogs(); setShowAuditLog(true) }} icon={<FileText size={14} />}>Audit log</Btn><Btn onClick={() => { setSelectedBusinessId(businesses[0]?.id ?? ''); setShowAdd(true) }} icon={<Plus size={14} />}>Add User</Btn></div>} />
      {error && <div style={{color:'var(--b360-red)',fontSize:13}}>{error}</div>}
      <Card style={{ padding: 0 }}>
        {usersLoading ? (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--b360-text-secondary)' }}>Loading users…</div>
        ) : (
          <DataTable
            headers={['Name', 'Email', 'Phone', 'Account role', 'Access groups', 'Status', '']}
            rows={users.map(u => [
              u.name,
              u.email,
              u.phone,
              <select key="role" value={u.role} disabled={u.id===currentUser?.id} onChange={event=>handleUserRoleChange(u.id,event.target.value)} style={{padding:'6px 8px',borderRadius:7}}>{ROLES.map(role=><option key={role.value} value={role.value}>{role.label}</option>)}</select>,
              <span key="groups" style={{fontSize:12,color:'var(--b360-text-secondary)'}}>{u.assignedGroups?.length ? u.assignedGroups.join(', ') : accessConfig?.groups.filter(group=>group.userIds.includes(u.id)).map(group=>group.name).join(', ') || 'Default access'}</span>,
              <StatusBadge key="status" status={u.isActive === false ? 'INACTIVE' : 'ACTIVE'} />,
              <Btn key="del" disabled={u.id===currentUser?.id} variant={u.isActive === false ? 'secondary' : 'danger'} small onClick={() => handleToggleUserStatus(u.id, u.isActive === false)}>
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
  storefrontThemeColor: '#0F766E', storefrontHeadline: 'Shop with us online',
  storefrontDescription: '', storefrontBannerUrl: null, storefrontLayout: 'GRID',
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
  const [storefrontSlug, setStorefrontSlug] = useState('')
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveMsg, setSaveMsg] = useState<{ ok: boolean; text: string } | null>(null)
  const [storeLinkCopied, setStoreLinkCopied] = useState(false)

  const handleToggleBusinessStatus = async (id: string, isActive: boolean) => {
    if (!isActive && !window.confirm('Disable this business account? All merchant access will stop immediately.')) return
    const res = await superAdminApi.setBusinessStatus(id, { isActive })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === id ? res.data! : b)))
    }
  }

  const handleSubscriptionChange = async (business: BusinessResponse, enabled: boolean, tier = business.subscriptionTier) => {
    if (!enabled && !window.confirm(`Disable ${business.name}'s subscription? Existing sessions will stop working immediately.`)) return
    const res = await superAdminApi.updateSubscription(business.id, {
      enabled,
      tier: tier === 'PREMIUM' ? 'PREMIUM' : 'FREEMIUM',
    })
    if (res.success && res.data) {
      setBusinesses(prev => prev.map(b => (b.id === business.id ? res.data! : b)))
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
            setStorefrontSlug(d.storefrontSlug)
            setForm({
              name: d.name, owner: d.owner, phone: d.phone, email: d.email, type: d.type, county: d.county, address: d.address,
              kraPin: d.kraPin, paybillNumber: d.paybillNumber, accountNumber: d.accountNumber,
              receiptHeader: d.receiptHeader, receiptFooter: d.receiptFooter, receiptLogo: d.receiptLogo,
              receiptShowTax: d.receiptShowTax, receiptShowCustomer: d.receiptShowCustomer,
              storefrontThemeColor: d.storefrontThemeColor || '#0F766E', storefrontHeadline: d.storefrontHeadline || 'Shop with us online',
              storefrontDescription: d.storefrontDescription || '', storefrontBannerUrl: d.storefrontBannerUrl || null,
              storefrontLayout: d.storefrontLayout || 'GRID',
            })
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
      if (res.success && res.data) setStorefrontSlug(res.data.storefrontSlug)
      setSaveMsg({ ok: res.success, text: res.message || (res.success ? 'Saved' : 'Failed to save') })
    } catch (e: any) {
      setSaveMsg({ ok: false, text: e.response?.data?.message || 'Network error. Please try again.' })
    } finally {
      setSaving(false)
    }
  }

  const storefrontUrl = storefrontSlug
    ? `${window.location.origin}/shop/${storefrontSlug}`
    : ''

  const copyStorefrontLink = async () => {
    if (!storefrontUrl) return
    try {
      await navigator.clipboard.writeText(storefrontUrl)
      setStoreLinkCopied(true)
      window.setTimeout(() => setStoreLinkCopied(false), 2_000)
    } catch {
      setSaveMsg({ ok: false, text: 'Clipboard access was blocked. Copy the URL manually.' })
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
              headers={['Business Name', 'Type', 'Tier', 'Subscription', 'Business', 'Created', 'Actions']}
              rows={businesses.map(b => [
                b.name,
                b.type,
                <select
                  key="tier"
                  value={b.subscriptionTier}
                  onChange={event => handleSubscriptionChange(b, b.subscriptionEnabled, event.target.value)}
                  style={{ padding: '6px 8px', borderRadius: 7, fontSize: 12 }}
                >
                  <option value="FREEMIUM">Freemium</option>
                  <option value="PREMIUM">Premium</option>
                </select>,
                <StatusBadge key="subscription" status={b.subscriptionEnabled ? 'ACTIVE' : 'INACTIVE'} />,
                <StatusBadge key="status" status={b.isActive ? 'ACTIVE' : 'INACTIVE'} />,
                new Date(b.createdAt).toLocaleDateString(),
                <div key="actions" style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  <Btn variant={b.subscriptionEnabled ? 'danger' : 'secondary'} small onClick={() => handleSubscriptionChange(b, !b.subscriptionEnabled)}>
                    {b.subscriptionEnabled ? 'Disable subscription' : 'Enable subscription'}
                  </Btn>
                  <Btn variant={b.isActive ? 'danger' : 'secondary'} small onClick={() => handleToggleBusinessStatus(b.id, !b.isActive)}>
                    {b.isActive ? 'Disable business' : 'Enable business'}
                  </Btn>
                </div>,
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
      <BizSection title="Online Store">
        <div style={{ fontSize: 13, color: 'var(--b360-text-secondary)', lineHeight: 1.5 }}>
          Share this public link with customers. Active products with available stock appear automatically, and customer orders are added to your Orders menu.
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <input readOnly value={storefrontUrl} style={{ flex: 1, minWidth: 240, padding: '9px 12px', border: '1px solid var(--b360-border)', borderRadius: 8, fontSize: 12 }} />
          <Btn variant="secondary" small icon={<Copy size={13} />} onClick={copyStorefrontLink}>{storeLinkCopied ? 'Copied' : 'Copy'}</Btn>
          <Btn variant="secondary" small icon={<ExternalLink size={13} />} onClick={() => window.open(storefrontUrl, '_blank', 'noopener,noreferrer')}>Open Store</Btn>
        </div>
        <div style={{ borderTop:'1px solid var(--b360-border)', paddingTop:14, display:'flex', flexDirection:'column', gap:12 }}>
          <strong style={{ fontSize:13 }}>Storefront appearance</strong>
          <BizField label="Welcome headline" value={form.storefrontHeadline || ''} onChange={f('storefrontHeadline')} />
          <BizField label="Store description" value={form.storefrontDescription || ''} onChange={f('storefrontDescription')} />
          <BizField label="Banner image URL" value={form.storefrontBannerUrl || ''} onChange={v => setForm(prev => ({...prev, storefrontBannerUrl:v || null}))} />
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
            <span style={{ fontSize:13, color:'var(--b360-text-secondary)', width:160 }}>Theme color</span>
            <div style={{ flex:1, maxWidth:320, display:'flex', gap:8 }}>
              <input type="color" value={form.storefrontThemeColor || '#0F766E'} onChange={e=>setForm(prev=>({...prev,storefrontThemeColor:e.target.value.toUpperCase()}))} style={{width:46,height:36,padding:2,border:'1px solid var(--b360-border)',borderRadius:8}} />
              <input value={form.storefrontThemeColor || '#0F766E'} onChange={e=>setForm(prev=>({...prev,storefrontThemeColor:e.target.value}))} maxLength={7} style={{flex:1,padding:'8px 12px',border:'1px solid var(--b360-border)',borderRadius:8}} />
            </div>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
            <span style={{ fontSize:13, color:'var(--b360-text-secondary)', width:160 }}>Product layout</span>
            <select value={form.storefrontLayout || 'GRID'} onChange={e=>setForm(prev=>({...prev,storefrontLayout:e.target.value as 'GRID'|'LIST'}))} style={{flex:1,maxWidth:320,padding:'8px 12px',border:'1px solid var(--b360-border)',borderRadius:8,background:'white'}}>
              <option value="GRID">Product grid</option><option value="LIST">Product list</option>
            </select>
          </div>
          <div style={{ padding:18, borderRadius:12, color:'white', background:form.storefrontThemeColor || '#0F766E', backgroundImage:form.storefrontBannerUrl ? `linear-gradient(#0007,#0007),url(${form.storefrontBannerUrl})` : undefined, backgroundSize:'cover', backgroundPosition:'center' }}>
            <div style={{fontSize:11,textTransform:'uppercase',letterSpacing:1}}>Preview</div>
            <h3 style={{margin:'5px 0'}}>{form.storefrontHeadline || 'Shop with us online'}</h3>
            <div style={{fontSize:12}}>{form.storefrontDescription || form.name || 'Your store description'}</div>
          </div>
        </div>
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
