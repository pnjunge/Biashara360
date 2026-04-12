import React, { useState, useEffect } from 'react'
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Download, Share2, FileText, Table } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, ProgressBar, KpiCard, AlertBanner } from '../components/ui'
import { expenseApi, paymentApi, reportApi, ExpenseResponse, PaymentResponse, ProfitSummaryResponse } from '../services/api'

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

export function ExpensesPage() {
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    expenseApi.list().then(res => {
      if (res.success && res.data) setExpenses(res.data)
    }).finally(() => setLoading(false))
  }, [])

  async function handleDelete(id: string) {
    await expenseApi.delete(id)
    setExpenses(prev => prev.filter(e => e.id !== id))
  }

  const total = expenses.reduce((s, e) => s + e.amount, 0)
  const maxAmount = expenses.length > 0 ? Math.max(...expenses.map(e => e.amount)) : 1

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Expenses & Profit"
        action={<Btn icon={<Plus size={14}/>}>Add Expense</Btn>} />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Total This Month"  value={`KES ${total.toLocaleString()}`} change="All categories"    icon={<FileText size={18}/>} color="var(--b360-red)" />
        <KpiCard title="Stock Purchase"    value={`KES ${expenses.filter(e=>e.category==='STOCK_PURCHASE').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Stock purchases" icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Advertising"       value={`KES ${expenses.filter(e=>e.category==='ADVERTISING').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Marketing spend" icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Operations"        value={`KES ${expenses.filter(e=>e.category==='RENT'||e.category==='DELIVERY'||e.category==='PACKAGING').reduce((s,e)=>s+e.amount,0).toLocaleString()}`} change="Rent + Ops" icon={<FileText size={18}/>} color="var(--b360-amber)" />
      </div>

      {loading ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : expenses.length === 0 ? (
        <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No expenses yet</div>
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

  useEffect(() => {
    paymentApi.list().then(res => {
      if (res.success && res.data) setPayments(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const unreconciled = payments.filter(p => !p.isReconciled)
  const total = payments.filter(p => p.isReconciled).reduce((s, p) => s + p.amount, 0)

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
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
                      <Btn>Match to Order</Btn>
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
            <Btn variant="secondary" icon={<Download size={14}/>}>Export PDF</Btn>
            <Btn variant="secondary" icon={<Table size={14}/>}>Export Excel</Btn>
            <Btn variant="secondary" icon={<Share2 size={14}/>}>WhatsApp</Btn>
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
export function SettingsPage() {
  const [twoFA, setTwoFA] = useState(true)
  const [sms, setSms] = useState(true)
  const [email, setEmail] = useState(false)

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
      <PageHeader title="Settings" />
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
          <Btn>Upgrade to Premium →</Btn>
        </div>
      </Section>
    </div>
  )
}
