import React, { useState, useEffect } from 'react'
import { TrendingUp, ShoppingCart, Clock, AlertTriangle, Plus, Search, Eye, Edit, Package, Users } from 'lucide-react'
import { KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Avatar } from '../components/ui'
import { productApi, orderApi, customerApi, reportApi, ProductResponse, OrderResponse, CustomerResponse, ProfitSummaryResponse } from '../services/api'

function getCurrentMonthRange() {
  const now = new Date()
  const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const [profitSummary, setProfitSummary] = useState<ProfitSummaryResponse | null>(null)
  const [recentOrders, setRecentOrders] = useState<OrderResponse[]>([])
  const [lowStockProducts, setLowStockProducts] = useState<ProductResponse[]>([])
  const [customerCount, setCustomerCount] = useState<number>(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const { startDate, endDate } = getCurrentMonthRange()
    Promise.all([
      reportApi.profitSummary(startDate, endDate),
      orderApi.list(undefined, undefined, 5),
      productApi.list(undefined, true),
      customerApi.list(),
    ]).then(([ps, ord, prods, custs]) => {
      if (ps.success && ps.data) setProfitSummary(ps.data)
      if (ord.success && ord.data) setRecentOrders(ord.data.data)
      if (prods.success && prods.data) setLowStockProducts(prods.data)
      if (custs.success && custs.data) setCustomerCount(custs.data.length)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const fmt = (v: number) => `KES ${v.toLocaleString()}`

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <PageHeader title="Dashboard" />

      {loading ? (
        <div style={{ textAlign:'center', padding:40, color:'var(--b360-text-secondary)' }}>Loading...</div>
      ) : (
        <>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(4, 1fr)', gap:16 }}>
            <KpiCard title="Monthly Revenue" value={profitSummary ? fmt(profitSummary.totalRevenue) : 'KES 0'} change={profitSummary ? `${(profitSummary.grossMargin * 100).toFixed(1)}% gross margin` : 'Current month'} icon={<TrendingUp size={18}/>} color="var(--b360-green)" />
            <KpiCard title="Net Profit"      value={profitSummary ? fmt(profitSummary.netProfit) : 'KES 0'} change={profitSummary ? `${(profitSummary.netMargin * 100).toFixed(1)}% net margin` : 'Current month'} icon={<TrendingUp size={18}/>} color="var(--b360-blue)" />
            <KpiCard title="Customers"       value={String(customerCount)} change="Total registered" icon={<Users size={18}/>} color="var(--b360-amber)" />
            <KpiCard title="Low Stock Items" value={String(lowStockProducts.length)} change={lowStockProducts.length > 0 ? 'Need restocking' : 'All items stocked'} icon={<AlertTriangle size={18}/>} color="var(--b360-red)" />
          </div>

          {lowStockProducts.length > 0 && (
            <div style={{ display:'flex', gap:16, flexWrap:'wrap' }}>
              <AlertBanner message={`${lowStockProducts.length} product${lowStockProducts.length > 1 ? 's are' : ' is'} running low on stock`} icon={<AlertTriangle size={15}/>} color="var(--b360-amber)" />
            </div>
          )}

          <div style={{ display:'grid', gridTemplateColumns:'1.6fr 1fr', gap:16 }}>
            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:4 }}>Current Month Summary</h3>
              <p style={{ fontSize:12, color:'var(--b360-text-secondary)', marginBottom:16 }}>Profit & Loss overview</p>
              {profitSummary ? (
                <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
                  {[
                    ['Total Revenue',   profitSummary.totalRevenue,   'var(--b360-green)'],
                    ['Cost of Goods',   profitSummary.totalCostOfGoods, 'var(--b360-red)'],
                    ['Gross Profit',    profitSummary.grossProfit,    'var(--b360-green)'],
                    ['Total Expenses',  profitSummary.totalExpenses,  'var(--b360-red)'],
                    ['Net Profit',      profitSummary.netProfit,      'var(--b360-blue)'],
                  ].map(([label, value, color], i, arr) => (
                    <div key={label as string} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0', borderBottom: i < arr.length - 1 ? '1px solid var(--b360-border)' : 'none' }}>
                      <span style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>{label}</span>
                      <span style={{ fontWeight: i === arr.length - 1 ? 800 : 600, fontSize: i === arr.length - 1 ? 15 : 13, color: color as string }}>
                        KES {(value as number).toLocaleString()}
                      </span>
                    </div>
                  ))}
                  <div style={{ marginTop:12, fontSize:12, color:'var(--b360-text-secondary)', textAlign:'center' }}>
                    {profitSummary.period}
                  </div>
                </div>
              ) : (
                <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>
                  Data will appear as orders are recorded
                </div>
              )}
            </Card>

            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, marginBottom:16 }}>Recent Orders</h3>
              {recentOrders.length === 0 ? (
                <div style={{ padding:30, textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>No orders yet</div>
              ) : (
                <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
                  {recentOrders.slice(0, 4).map((o, i) => (
                    <div key={o.id}>
                      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0' }}>
                        <div>
                          <div style={{ fontSize:12, fontWeight:700, color:'var(--b360-green)' }}>{o.orderNumber}</div>
                          <div style={{ fontSize:13, fontWeight:500 }}>{o.customerName}</div>
                        </div>
                        <div style={{ textAlign:'right' }}>
                          <div style={{ fontWeight:700, fontSize:13 }}>KES {o.subtotal.toLocaleString()}</div>
                          <StatusBadge status={o.paymentStatus} />
                        </div>
                      </div>
                      {i < Math.min(recentOrders.length, 4) - 1 && <div style={{ borderTop:'1px solid var(--b360-border)' }} />}
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}

// ── Inventory ─────────────────────────────────────────────────────────────────
export function InventoryPage() {
  const [search, setSearch] = useState('')
  const [lowOnly, setLowOnly] = useState(false)
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    productApi.list().then(res => {
      if (res.success && res.data) setProducts(res.data)
    }).finally(() => setLoading(false))
  }, [])

  const stockStatus = (p: ProductResponse) => p.isOutOfStock ? 'OUT' : p.isLowStock ? 'LOW' : 'OK'
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
        <KpiCard title="Total SKUs"      value={`${products.length}`}  change="Unique products"     icon={<Package size={18}/>} color="var(--b360-green)" />
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
        {loading ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
        ) : filtered.length === 0 ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No products yet</div>
        ) : (
          <DataTable
            headers={['Product', 'SKU', 'Category', 'Buy Price', 'Sell Price', 'Profit', 'Stock', 'Status', 'Actions']}
            rows={filtered.map(p => {
              const st = stockStatus(p)
              return [
                <strong>{p.name}</strong>,
                <span style={{ fontFamily:'monospace', fontSize:12, color:'var(--b360-text-secondary)' }}>{p.sku}</span>,
                p.category,
                `KES ${p.buyingPrice.toLocaleString()}`,
                <span style={{ fontWeight:600, color:'var(--b360-green)' }}>KES {p.sellingPrice.toLocaleString()}</span>,
                <span style={{ color:'var(--b360-blue)', fontWeight:600 }}>KES {p.profitPerItem.toLocaleString()}</span>,
                <span style={{ fontWeight:700, color:stockColor(st) }}>{p.currentStock}</span>,
                <StatusBadge status={st} />,
                <div style={{ display:'flex', gap:6 }}>
                  <Btn variant="secondary" small icon={<Edit size={12}/>}>Edit</Btn>
                  <Btn variant="secondary" small icon={<Plus size={12}/>}>Stock</Btn>
                </div>
              ]
            })}
          />
        )}
      </Card>
    </div>
  )
}

// ── Orders ────────────────────────────────────────────────────────────────────
export function OrdersPage() {
  const [filter, setFilter] = useState('All')
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const filters = ['All', 'PAID', 'PENDING', 'COD']

  useEffect(() => {
    orderApi.list().then(res => {
      if (res.success && res.data) setOrders(res.data.data)
    }).finally(() => setLoading(false))
  }, [])

  const filtered = filter === 'All' ? orders : orders.filter(o => o.paymentStatus === filter)

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
        {loading ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
        ) : filtered.length === 0 ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No orders yet</div>
        ) : (
          <DataTable
            headers={['Order #', 'Customer', 'Phone', 'Items', 'Amount', 'Payment', 'Delivery', 'Date', 'Actions']}
            rows={filtered.map(o => [
              <span style={{ fontWeight:700, color:'var(--b360-green)', fontSize:12 }}>{o.orderNumber}</span>,
              <span style={{ fontWeight:600 }}>{o.customerName}</span>,
              <span style={{ color:'var(--b360-text-secondary)' }}>{o.customerPhone}</span>,
              o.items.length,
              <span style={{ fontWeight:700 }}>KES {o.subtotal.toLocaleString()}</span>,
              <StatusBadge status={o.paymentStatus} />,
              <StatusBadge status={o.deliveryStatus} />,
              <span style={{ color:'var(--b360-text-secondary)', fontSize:12 }}>{new Date(o.createdAt).toLocaleDateString('en-KE')}</span>,
              <Btn variant="secondary" small icon={<Eye size={12}/>}>View</Btn>
            ])}
          />
        )}
      </Card>
    </div>
  )
}

// ── Customers ─────────────────────────────────────────────────────────────────
export function CustomersPage() {
  const [search, setSearch] = useState('')
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    customerApi.list().then(res => {
      if (res.success && res.data) setCustomers(res.data)
    }).finally(() => setLoading(false))
  }, [])

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
        {loading ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>Loading...</div>
        ) : filtered.length === 0 ? (
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No customers yet</div>
        ) : (
          <DataTable
            headers={['Customer', 'Phone', 'Location', 'Orders', 'Total Spent', 'Loyalty Pts', 'Actions']}
            rows={filtered.map(c => [
              <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                <Avatar name={c.name} size={32} />
                <div>
                  <div style={{ fontWeight:600 }}>{c.name}</div>
                  {c.isRepeatCustomer && <div style={{ fontSize:11, color:'var(--b360-amber)' }}>⭐ Repeat customer</div>}
                </div>
              </div>,
              c.phone,
              c.location,
              <span style={{ fontWeight:700 }}>{c.totalOrders}</span>,
              <span style={{ fontWeight:700, color:'var(--b360-green)' }}>KES {c.totalSpent.toLocaleString()}</span>,
              <span style={{ color:'var(--b360-amber)', fontWeight:600 }}>⭐ {c.loyaltyPoints}</span>,
              <div style={{ display:'flex', gap:6 }}>
                <Btn variant="secondary" small icon={<Eye size={12}/>}>View</Btn>
                <Btn variant="secondary" small>WhatsApp</Btn>
              </div>
            ])}
          />
        )}
      </Card>
    </div>
  )
}
