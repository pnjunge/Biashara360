import React, { useState } from 'react'
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { TrendingUp, ShoppingCart, Clock, AlertTriangle, Plus, Search, Filter, Eye, Edit, Package } from 'lucide-react'
import { KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Avatar } from '../components/ui'

// ── Mock data ─────────────────────────────────────────────────────────────────
const revenueData = [
  { day:'Mon', revenue:18000, profit:6000 },
  { day:'Tue', revenue:24000, profit:8500 },
  { day:'Wed', revenue:19000, profit:7000 },
  { day:'Thu', revenue:31000, profit:11000 },
  { day:'Fri', revenue:27000, profit:9500 },
  { day:'Sat', revenue:22000, profit:7800 },
  { day:'Sun', revenue:34000, profit:12200 },
]

const products = [
  { id:1, name:'Black Dress Size M',  sku:'SKU-001', category:'Clothing',    buy:800,  sell:1500, stock:2,  threshold:5 },
  { id:2, name:'Ankara Print Fabric', sku:'SKU-002', category:'Fabric',      buy:350,  sell:700,  stock:12, threshold:5 },
  { id:3, name:'Gold Hoop Earrings',  sku:'SKU-003', category:'Accessories', buy:150,  sell:450,  stock:3,  threshold:5 },
  { id:4, name:'White Sneakers 38',   sku:'SKU-004', category:'Shoes',       buy:1200, sell:2200, stock:0,  threshold:3 },
  { id:5, name:'Silk Blouse Pink',    sku:'SKU-005', category:'Clothing',    buy:600,  sell:1200, stock:8,  threshold:5 },
  { id:6, name:'Beaded Necklace',     sku:'SKU-006', category:'Accessories', buy:200,  sell:600,  stock:15, threshold:5 },
]

const orders = [
  { id:1, number:'B360-0042', customer:'Amina Hassan',  phone:'0712345678', amount:4500, payment:'PAID',    delivery:'DELIVERED',  date:'Today 2:30PM',  items:2 },
  { id:2, number:'B360-0041', customer:'Brian Otieno',  phone:'0723456789', amount:1500, payment:'PENDING', delivery:'PROCESSING', date:'Today 11:00AM', items:1 },
  { id:3, number:'B360-0040', customer:'Grace Njeri',   phone:'0734567890', amount:3200, payment:'COD',     delivery:'SHIPPED',    date:'Yesterday',     items:3 },
  { id:4, number:'B360-0039', customer:'David Kamau',   phone:'0745678901', amount:6800, payment:'PAID',    delivery:'DELIVERED',  date:'Yesterday',     items:4 },
  { id:5, number:'B360-0038', customer:'Mary Akinyi',   phone:'0756789012', amount:700,  payment:'PENDING', delivery:'PENDING',    date:'Mon Mar 4',     items:1 },
]

const customers = [
  { id:1, name:'Amina Hassan', phone:'0712345678', location:'Westlands',  orders:12, spent:54000, points:540 },
  { id:2, name:'Brian Otieno', phone:'0723456789', location:'Eastlands',  orders:5,  spent:18500, points:185 },
  { id:3, name:'Grace Njeri',  phone:'0734567890', location:'Karen',      orders:8,  spent:31200, points:312 },
  { id:4, name:'David Kamau',  phone:'0745678901', location:'Kiambu',     orders:4,  spent:16800, points:168 },
  { id:5, name:'Mary Akinyi',  phone:'0756789012', location:'Umoja',      orders:3,  spent:9800,  points:98  },
]

// ── Dashboard ─────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Dashboard" />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4, 1fr)', gap:16 }}>
        <KpiCard title="Monthly Revenue" value="KES 145,650" change="+12% vs last month" icon={<TrendingUp size={18}/>} color="var(--b360-green)" />
        <KpiCard title="Net Profit"      value="KES 38,200"  change="26.2% net margin"   icon={<TrendingUp size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Orders Today"    value="24"           change="+3 from yesterday"  icon={<ShoppingCart size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Unpaid Orders"   value="KES 12,300"  change="7 orders pending"   icon={<Clock size={18}/>} color="var(--b360-red)" />
      </div>

      <div style={{ display:'flex', gap:16, flexWrap:'wrap' }}>
        <AlertBanner message="3 products are running low on stock" icon={<AlertTriangle size={15}/>} color="var(--b360-amber)" />
        <AlertBanner message="2 Mpesa payments awaiting reconciliation" icon={<AlertTriangle size={15}/>} color="var(--b360-blue)" />
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'1.6fr 1fr', gap:16 }}>
        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:4 }}>Revenue & Profit Trend</h3>
          <p style={{ fontSize:12, color:'var(--b360-text-secondary)', marginBottom:16 }}>Last 7 days</p>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={revenueData}>
              <defs>
                <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#1B8B34" stopOpacity={0.15}/>
                  <stop offset="95%" stopColor="#1B8B34" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="day" tick={{ fontSize:11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize:11 }} axisLine={false} tickLine={false} tickFormatter={v=>`${v/1000}K`} />
              <Tooltip formatter={(v:number) => `KES ${v.toLocaleString()}`} />
              <Area type="monotone" dataKey="revenue" stroke="#1B8B34" strokeWidth={2} fill="url(#rev)" name="Revenue" />
              <Area type="monotone" dataKey="profit"  stroke="#1565C0" strokeWidth={2} fill="transparent" name="Profit" />
            </AreaChart>
          </ResponsiveContainer>
        </Card>

        <Card style={{ padding:20 }}>
          <h3 style={{ fontWeight:700, marginBottom:16 }}>Recent Orders</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
            {orders.slice(0,4).map((o,i) => (
              <div key={o.id}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0' }}>
                  <div>
                    <div style={{ fontSize:12, fontWeight:700, color:'var(--b360-green)' }}>{o.number}</div>
                    <div style={{ fontSize:13, fontWeight:500 }}>{o.customer}</div>
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontWeight:700, fontSize:13 }}>KES {o.amount.toLocaleString()}</div>
                    <StatusBadge status={o.payment} />
                  </div>
                </div>
                {i < 3 && <div style={{ borderTop:'1px solid var(--b360-border)' }} />}
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}

// ── Inventory ─────────────────────────────────────────────────────────────────
export function InventoryPage() {
  const [search, setSearch] = useState('')
  const [lowOnly, setLowOnly] = useState(false)

  const stockStatus = (p: typeof products[0]) => p.stock === 0 ? 'OUT' : p.stock <= p.threshold ? 'LOW' : 'OK'
  const stockColor = (s: string) => s === 'OUT' ? 'var(--b360-red)' : s === 'LOW' ? 'var(--b360-amber)' : 'var(--b360-green)'

  const filtered = products.filter(p =>
    (p.name.toLowerCase().includes(search.toLowerCase()) || p.sku.toLowerCase().includes(search.toLowerCase()))
    && (!lowOnly || stockStatus(p) !== 'OK')
  )

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Inventory"
        action={<Btn icon={<Plus size={14}/>}>Add Product</Btn>} />

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12 }}>
        <KpiCard title="Total Products"  value={`${products.length}`}  change="Active items"        icon={<Package size={18}/>} color="var(--b360-blue)" />
        <KpiCard title="Low Stock"       value={`${products.filter(p=>stockStatus(p)==='LOW').length}`} change="Need restocking"  icon={<AlertTriangle size={18}/>} color="var(--b360-amber)" />
        <KpiCard title="Out of Stock"    value={`${products.filter(p=>stockStatus(p)==='OUT').length}`} change="Unavailable"      icon={<AlertTriangle size={18}/>} color="var(--b360-red)" />
        <KpiCard title="Total SKUs"      value="6"                      change="Unique products"     icon={<Package size={18}/>} color="var(--b360-green)" />
      </div>

      <Card>
        <div style={{ padding:'16px 20px', borderBottom:'1px solid var(--b360-border)', display:'flex', gap:12, alignItems:'center' }}>
          <div style={{ position:'relative', flex:1, maxWidth:300 }}>
            <Search size={14} style={{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)', color:'var(--b360-text-secondary)' }} />
            <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search products or SKU..."
              style={{ width:'100%', padding:'8px 12px 8px 32px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, outline:'none', fontFamily:'inherit' }} />
          </div>
          <label style={{ display:'flex', alignItems:'center', gap:6, fontSize:13, cursor:'pointer' }}>
            <input type="checkbox" checked={lowOnly} onChange={e=>setLowOnly(e.target.checked)} />
            Low stock only
          </label>
        </div>
        <DataTable
          headers={['Product', 'SKU', 'Category', 'Buy Price', 'Sell Price', 'Profit', 'Stock', 'Status', 'Actions']}
          rows={filtered.map(p => {
            const st = stockStatus(p)
            return [
              <strong>{p.name}</strong>,
              <span style={{ fontFamily:'monospace', fontSize:12, color:'var(--b360-text-secondary)' }}>{p.sku}</span>,
              p.category,
              `KES ${p.buy.toLocaleString()}`,
              <span style={{ fontWeight:600, color:'var(--b360-green)' }}>KES {p.sell.toLocaleString()}</span>,
              <span style={{ color:'var(--b360-blue)', fontWeight:600 }}>KES {(p.sell-p.buy).toLocaleString()}</span>,
              <span style={{ fontWeight:700, color:stockColor(st) }}>{p.stock}</span>,
              <StatusBadge status={st} />,
              <div style={{ display:'flex', gap:6 }}>
                <Btn variant="secondary" small icon={<Edit size={12}/>}>Edit</Btn>
                <Btn variant="secondary" small icon={<Plus size={12}/>}>Stock</Btn>
              </div>
            ]
          })}
        />
      </Card>
    </div>
  )
}

// ── Orders ────────────────────────────────────────────────────────────────────
export function OrdersPage() {
  const [filter, setFilter] = useState('All')
  const filters = ['All', 'PAID', 'PENDING', 'COD']
  const filtered = filter === 'All' ? orders : orders.filter(o => o.payment === filter)

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Orders"
        action={<Btn icon={<Plus size={14}/>}>New Order</Btn>} />

      <div style={{ display:'flex', gap:8, alignItems:'center' }}>
        {filters.map(f => (
          <button key={f} onClick={() => setFilter(f)} style={{
            padding:'6px 14px', borderRadius:20, fontSize:12, fontWeight:600, cursor:'pointer', border:'1px solid',
            background: filter===f ? 'var(--b360-green)' : 'white',
            color: filter===f ? 'white' : 'var(--b360-text)',
            borderColor: filter===f ? 'var(--b360-green)' : 'var(--b360-border)'
          }}>{f}</button>
        ))}
      </div>

      <Card>
        <DataTable
          headers={['Order #', 'Customer', 'Phone', 'Items', 'Amount', 'Payment', 'Delivery', 'Date', 'Actions']}
          rows={filtered.map(o => [
            <span style={{ fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{o.number}</span>,
            <span style={{ fontWeight:600 }}>{o.customer}</span>,
            <span style={{ color:'var(--b360-text-secondary)' }}>{o.phone}</span>,
            o.items,
            <span style={{ fontWeight:700 }}>KES {o.amount.toLocaleString()}</span>,
            <StatusBadge status={o.payment} />,
            <StatusBadge status={o.delivery} />,
            <span style={{ color:'var(--b360-text-secondary)', fontSize:12 }}>{o.date}</span>,
            <Btn variant="secondary" small icon={<Eye size={12}/>}>View</Btn>
          ])}
        />
      </Card>
    </div>
  )
}

// ── Customers ─────────────────────────────────────────────────────────────────
export function CustomersPage() {
  const [search, setSearch] = useState('')
  const filtered = customers.filter(c =>
    c.name.toLowerCase().includes(search.toLowerCase()) || c.phone.includes(search)
  )

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Customers"
        action={<Btn icon={<Plus size={14}/>}>Add Customer</Btn>} />

      <div style={{ position:'relative', maxWidth:320 }}>
        <Search size={14} style={{ position:'absolute', left:10, top:'50%', transform:'translateY(-50%)', color:'var(--b360-text-secondary)' }} />
        <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search customers..."
          style={{ width:'100%', padding:'8px 12px 8px 32px', border:'1px solid var(--b360-border)', borderRadius:8, fontSize:13, outline:'none', fontFamily:'inherit' }} />
      </div>

      <Card>
        <DataTable
          headers={['Customer', 'Phone', 'Location', 'Orders', 'Total Spent', 'Loyalty Pts', 'Actions']}
          rows={filtered.map(c => [
            <div style={{ display:'flex', alignItems:'center', gap:10 }}>
              <Avatar name={c.name} size={32} />
              <div>
                <div style={{ fontWeight:600 }}>{c.name}</div>
                {c.orders > 1 && <div style={{ fontSize:11, color:'var(--b360-amber)' }}>⭐ Repeat customer</div>}
              </div>
            </div>,
            c.phone,
            c.location,
            <span style={{ fontWeight:700 }}>{c.orders}</span>,
            <span style={{ fontWeight:700, color:'var(--b360-green)' }}>KES {c.spent.toLocaleString()}</span>,
            <span style={{ color:'var(--b360-amber)', fontWeight:600 }}>⭐ {c.points}</span>,
            <div style={{ display:'flex', gap:6 }}>
              <Btn variant="secondary" small icon={<Eye size={12}/>}>View</Btn>
              <Btn variant="secondary" small>WhatsApp</Btn>
            </div>
          ])}
        />
      </Card>
    </div>
  )
}
