import React, { useState } from 'react'
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Plus, Download, Share2, FileText, Table } from 'lucide-react'
import { PageHeader, Card, Btn, DataTable, StatusBadge, ProgressBar, KpiCard, AlertBanner } from '../components/ui'

// ── Expenses ──────────────────────────────────────────────────────────────────
const expenses = [
  { id:1, description:'Facebook & Instagram Ads', category:'ADVERTISING',    amount:5000,  date:'Mar 7' },
  { id:2, description:'Packaging materials',       category:'PACKAGING',      amount:1200,  date:'Mar 6' },
  { id:3, description:'Rider delivery fees',        category:'DELIVERY',       amount:800,   date:'Mar 6' },
  { id:4, description:'Monthly shop rent',          category:'RENT',           amount:15000, date:'Mar 1' },
  { id:5, description:'Fabric & stock purchase',   category:'STOCK_PURCHASE', amount:45000, date:'Mar 1' },
]

const catColors: Record<string, string> = {
  ADVERTISING:'var(--b360-blue)', RENT:'var(--b360-red)',
  STOCK_PURCHASE:'var(--b360-green)', DELIVERY:'var(--b360-amber)', PACKAGING:'#9E9E9E'
}

export function ExpensesPage() {
  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Expenses & Profit"
        action={<Btn icon={<Plus size={14}/>}>Add Expense</Btn>} />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Total This Month"  value="KES 65,500" change="All categories"    icon={<FileText size={18}/>} color="var(--b360-red)" />
        <KpiCard title="Stock Purchase"    value="KES 45,000" change="68.7% of spend"    icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Advertising"       value="KES 5,000"  change="Facebook + Insta"  icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Operations"        value="KES 17,000" change="Rent + Packaging"  icon={<FileText size={18}/>} color="var(--b360-amber)" />
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>Expense Breakdown</h3>
          {expenses.map(e => (
            <div key={e.id} style={{ marginBottom:14 }}>
              <div style={{ display:'flex', justifyContent:'space-between', marginBottom:5 }}>
                <div>
                  <span style={{ fontWeight:500, fontSize:13 }}>{e.description}</span>
                  <span style={{ marginLeft:8, fontSize:11, color:catColors[e.category], fontWeight:600 }}>
                    {e.category.replace('_',' ')}
                  </span>
                </div>
                <span style={{ fontWeight:700, color:'var(--b360-red)', fontSize:13 }}>KES {e.amount.toLocaleString()}</span>
              </div>
              <ProgressBar value={e.amount / 45000} color={catColors[e.category]} />
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
            <span style={{ color:catColors[e.category], fontWeight:600, fontSize:12 }}>{e.category.replace('_',' ')}</span>,
            <span style={{ fontWeight:700, color:'var(--b360-red)' }}>KES {e.amount.toLocaleString()}</span>,
            e.date,
            <Btn variant="danger" small>Delete</Btn>
          ])}
        />
      </Card>
    </div>
  )
}

// ── Payments ──────────────────────────────────────────────────────────────────
const payments = [
  { id:1, code:'RGK71HXYZ', payer:'Amina Hassan', phone:'0712345678', amount:4500, method:'Mpesa', date:'Today 2:30PM', reconciled:true  },
  { id:2, code:'PLM23NQRS', payer:'David Kamau',  phone:'0745678901', amount:6800, method:'Mpesa', date:'Yesterday',    reconciled:true  },
  { id:3, code:'QWE45RTYU', payer:'Sarah Wangui', phone:'0767890123', amount:1200, method:'Airtel', date:'Yesterday',    reconciled:false },
  { id:4, code:'ZXC89VBNM', payer:'Tom Mutua',    phone:'0778901234', amount:2300, method:'Mpesa', date:'Mon',          reconciled:false },
]

export function PaymentsPage() {
  const unreconciled = payments.filter(p => !p.reconciled)
  const total = payments.filter(p => p.reconciled).reduce((s,p) => s+p.amount, 0)

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Payments / Mpesa" />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Total Collected"    value={`KES ${total.toLocaleString()}`} change="Reconciled payments" icon={<FileText size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Unreconciled"       value={`${unreconciled.length} txns`}   change="Need matching"      icon={<FileText size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Mpesa Transactions" value="47"                               change="This month"         icon={<FileText size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Failed Payments"    value="2"                                change="Check with customer" icon={<FileText size={18}/>} color="var(--b360-red)" />
      </div>

      {unreconciled.length > 0 && (
        <div>
          <h3 style={{ fontWeight:700, marginBottom:10, fontSize:14 }}>⚠️ Needs Reconciliation</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
            {unreconciled.map(p => (
              <div key={p.id} style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'12px 16px', background:'var(--b360-amber-bg)', borderRadius:10, border:'1px solid var(--b360-amber)' }}>
                <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                  <div style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:13 }}>{p.code}</div>
                  <div>
                    <div style={{ fontWeight:600 }}>{p.payer}</div>
                    <div style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>{p.phone} · {p.method}</div>
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
          headers={['Mpesa Code', 'Customer', 'Phone', 'Amount', 'Method', 'Status', 'Date']}
          rows={payments.map(p => [
            <span style={{ fontFamily:'monospace', fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{p.code}</span>,
            <span style={{ fontWeight:600 }}>{p.payer}</span>,
            p.phone, 
            <span style={{ fontWeight:700 }}>KES {p.amount.toLocaleString()}</span>,
            p.method,
            <StatusBadge status={p.reconciled ? 'MATCHED' : 'PENDING'} />,
            p.date
          ])}
        />
      </Card>
    </div>
  )
}

// ── Reports ───────────────────────────────────────────────────────────────────
const monthlyData = [
  { month:'Nov', revenue:98000, expenses:52000, profit:46000 },
  { month:'Dec', revenue:132000, expenses:68000, profit:64000 },
  { month:'Jan', revenue:110000, expenses:58000, profit:52000 },
  { month:'Feb', revenue:124000, expenses:62000, profit:62000 },
  { month:'Mar', revenue:145650, expenses:65500, profit:80150 },
]

export function ReportsPage() {
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

      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
        {/* P&L */}
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>Profit & Loss — March 2025</h3>
          {[
            ['Total Revenue',     'KES 145,650', 'var(--b360-green)'],
            ['Cost of Goods',     'KES 72,000',  'var(--b360-red)'],
            ['Gross Profit',      'KES 73,650',  'var(--b360-green)', true],
            ['Total Expenses',    'KES 35,450',  'var(--b360-red)'],
          ].map(([l,v,c,bold]) => (
            <div key={l as string} style={{ display:'flex', justifyContent:'space-between', padding:'8px 0', borderBottom:'1px solid var(--b360-border)', fontWeight: bold ? 700 : 400 }}>
              <span style={{ fontSize:13 }}>{l}</span>
              <span style={{ color: c as string, fontWeight: bold ? 700 : 600, fontSize:13 }}>{v}</span>
            </div>
          ))}
          <div style={{ display:'flex', justifyContent:'space-between', padding:'12px 0 0', marginTop:4 }}>
            <span style={{ fontWeight:800, fontSize:15 }}>Net Profit</span>
            <span style={{ color:'var(--b360-green)', fontWeight:800, fontSize:15 }}>KES 38,200</span>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between' }}>
            <span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Net Margin</span>
            <span style={{ color:'var(--b360-blue)', fontWeight:600, fontSize:12 }}>26.2%</span>
          </div>
        </Card>

        {/* Monthly trend */}
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>5-Month Trend</h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={monthlyData} barSize={16}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="month" tick={{ fontSize:12 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize:11 }} tickFormatter={v => `${v/1000}K`} axisLine={false} tickLine={false} />
              <Tooltip formatter={(v:number) => `KES ${v.toLocaleString()}`} />
              <Bar dataKey="revenue"  fill="#1B8B34" radius={[4,4,0,0]} name="Revenue" />
              <Bar dataKey="expenses" fill="#D32F2F" radius={[4,4,0,0]} name="Expenses" />
              <Bar dataKey="profit"   fill="#1565C0" radius={[4,4,0,0]} name="Profit" />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      {/* Expense breakdown chart */}
      <Card style={{ padding:20 }}>
        <h3 style={{ fontWeight:700, marginBottom:16 }}>Expense Breakdown</h3>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, alignItems:'center' }}>
          <div>
            {[
              ['Stock Purchase', 45000, 'var(--b360-green)', 0.69],
              ['Rent',           15000, 'var(--b360-red)',   0.23],
              ['Advertising',    5000,  'var(--b360-blue)',  0.08],
              ['Delivery',       3200,  'var(--b360-amber)', 0.05],
              ['Packaging',      1200,  '#9E9E9E',           0.02],
            ].map(([cat, amt, color, frac]) => (
              <div key={cat as string} style={{ marginBottom:12 }}>
                <div style={{ display:'flex', justifyContent:'space-between', marginBottom:5 }}>
                  <span style={{ fontSize:13 }}>{cat}</span>
                  <span style={{ fontSize:13, fontWeight:600 }}>KES {(amt as number).toLocaleString()}</span>
                </div>
                <ProgressBar value={frac as number} color={color as string} />
              </div>
            ))}
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie data={[
                { name:'Stock', value:45000 },
                { name:'Rent', value:15000 },
                { name:'Ads', value:5000 },
                { name:'Delivery', value:3200 },
                { name:'Packaging', value:1200 },
              ]} cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }) => `${name} ${(percent*100).toFixed(0)}%`} labelLine={false} style={{ fontSize:10 }}>
                {['var(--b360-green)','var(--b360-red)','var(--b360-blue)','var(--b360-amber)','#9E9E9E'].map((c,i) => <Cell key={i} fill={c} />)}
              </Pie>
              <Tooltip formatter={(v:number) => `KES ${v.toLocaleString()}`} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </Card>
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
