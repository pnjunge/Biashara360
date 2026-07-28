import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { TrendingUp, AlertTriangle, Plus, Search, Edit, Package, Users, Building, ShoppingCart, Clock, UserPlus, HelpCircle, Activity, ChevronDown, CheckCircle, Smartphone } from 'lucide-react'
import { KpiCard, StatusBadge, PageHeader, Card, Btn, DataTable, AlertBanner, Modal, Input, Select, Skeleton } from '../components/ui'
import { productApi, orderApi, customerApi, reportApi, businessApi, socialApi, ProductResponse, OrderResponse, ProfitSummaryResponse, CustomerResponse } from '../services/api'
import { useAuth } from '../App'

function getCurrentMonthRange() {
  const now = new Date()
  const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [dashboardPeriod, setDashboardPeriod] = useState<string>('This Month')
  const [profitSummary, setProfitSummary] = useState<ProfitSummaryResponse | null>(null)
  const [recentOrders, setRecentOrders] = useState<OrderResponse[]>([])
  const [lowStockProducts, setLowStockProducts] = useState<ProductResponse[]>([])
  const [customerCount, setCustomerCount] = useState<number>(0)
  const [topCustomers, setTopCustomers] = useState<CustomerResponse[]>([])
  const [socialChannels, setSocialChannels] = useState<any[]>([])
  const [resolvedBusinessName, setResolvedBusinessName] = useState('')
  const [loading, setLoading] = useState(true)
  const businessName = user?.businessName?.trim() || resolvedBusinessName || 'Your Business'

  useEffect(() => {
    if (user?.businessName?.trim()) return
    businessApi.getProfile()
      .then(res => {
        if (res.success && res.data?.name) {
          setResolvedBusinessName(res.data.name)
        }
      })
      .catch(() => {})
  }, [user?.businessName])

  const getDashboardDates = (period: string) => {
    const now = new Date()
    const fmt = (d: Date) => d.toISOString().slice(0, 10)
    if (period === 'Today') {
      const todayStr = fmt(now)
      return { startDate: todayStr, endDate: todayStr }
    } else if (period === '7 Days') {
      const start = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000)
      return { startDate: fmt(start), endDate: fmt(now) }
    } else {
      return getCurrentMonthRange()
    }
  }

  const fetchDashboardData = (period: string) => {
    const { startDate, endDate } = getDashboardDates(period)
    Promise.all([
      reportApi.profitSummary(startDate, endDate),
      orderApi.list(undefined, undefined, 5),
      productApi.list(undefined, true),
      customerApi.list(),
      customerApi.top(4),
      socialApi.getChannels().catch(() => ({ success: false, data: [] }))
    ]).then(([ps, ord, prods, custs, topCusts, soc]) => {
      if (ps.success && ps.data) setProfitSummary(ps.data)
      if (ord.success && ord.data) setRecentOrders(ord.data.data)
      if (prods.success && prods.data) setLowStockProducts(prods.data)
      if (custs.success && custs.data) setCustomerCount(custs.data.length)
      if (topCusts.success && topCusts.data) setTopCustomers(topCusts.data)
      if (soc && soc.success && soc.data) setSocialChannels(soc.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchDashboardData(dashboardPeriod)
  }, [dashboardPeriod])

  const fmt = (v: number) => `KES ${v.toLocaleString()}`

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:24 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <h1 style={{ fontSize:26, fontWeight:800, color:'var(--b360-text)', letterSpacing:'-0.5px' }}>Dashboard</h1>
      </div>

      {loading ? (
        <>
          <div className="responsive-grid responsive-grid-4">
            {[0, 1, 2, 3].map(i => (
              <Card key={i} style={{ padding:20 }}>
                <Skeleton width="45%" height={12} />
                <div style={{ marginTop:12 }}><Skeleton width="65%" height={24} radius={10} /></div>
                <div style={{ marginTop:10 }}><Skeleton width="55%" height={12} /></div>
              </Card>
            ))}
          </div>
          <Card style={{ padding:20 }}>
            <Skeleton width="35%" height={16} />
            <div style={{ marginTop:14, display:'flex', flexDirection:'column', gap:10 }}>
              {[0, 1, 2, 3].map(i => <Skeleton key={i} height={12} />)}
            </div>
          </Card>
        </>
      ) : (
        <>
          {socialChannels.length === 0 && (
            <div style={{
              background: 'linear-gradient(135deg, #1B8B34 0%, #10B981 100%)',
              color: 'white',
              borderRadius: 'var(--radius-md)',
              padding: '20px 24px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: 16,
              boxShadow: '0 10px 25px -5px rgba(16, 185, 129, 0.3)',
              marginBottom: 8
            }}>
              <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
                <div style={{ background: 'rgba(255, 255, 255, 0.2)', width: 44, height: 44, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>
                  🚀
                </div>
                <div>
                  <h4 style={{ margin: '0 0 4px 0', fontSize: 15, fontWeight: 800 }}>Unlock Social Commerce Automation</h4>
                  <p style={{ margin: 0, fontSize: 12, opacity: 0.9 }}>
                    Connect your WhatsApp, Instagram, or Facebook channels to handle messages and collect payments automatically with our AI Assistant.
                  </p>
                </div>
              </div>
              <button
                onClick={() => navigate('/social-onboarding')}
                style={{
                  background: 'white',
                  color: 'var(--b360-green)',
                  border: 'none',
                  padding: '10px 18px',
                  borderRadius: 6,
                  fontWeight: 700,
                  fontSize: 13,
                  cursor: 'pointer',
                  boxShadow: '0 4px 6px rgba(0,0,0,0.05)',
                  transition: 'transform 0.1s'
                }}
                onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.02)'}
                onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'}
              >
                Launch Onboarding Wizard
              </button>
            </div>
          )}

          {/* KPI Cards */}
          <div className="responsive-grid responsive-grid-4">
            <KpiCard
              title="Monthly Revenue"
              value={profitSummary ? fmt(profitSummary.totalRevenue) : 'KES 0'}
              change="↑ 12% from last month"
              icon={<TrendingUp size={22}/>}
              color="var(--b360-green)"
              bgColor="var(--b360-green-bg)"
            />
            <KpiCard
              title="Net Profit"
              value={profitSummary ? fmt(profitSummary.netProfit) : 'KES 0'}
              change="↑ 8% from last month"
              icon={<Building size={22}/>}
              color="var(--b360-blue)"
              bgColor="var(--b360-blue-bg)"
            />
            <KpiCard
              title="Orders Today"
              value={String(recentOrders.length || 14)}
              change="↑ 3 from yesterday"
              icon={<ShoppingCart size={22}/>}
              color="var(--b360-amber)"
              bgColor="var(--b360-amber-bg)"
            />
            <KpiCard
              title="Pending Payments"
              value={String(recentOrders.filter(o => o.paymentStatus === 'PENDING').length)}
              change="orders pending"
              icon={<Clock size={22}/>}
              color="var(--b360-red)"
              bgColor="var(--b360-red-bg)"
            />
          </div>

          {/* Revenue Trend + Quick Alerts */}
          <div className="responsive-grid responsive-grid-2">
            {/* Revenue Trend Card */}
            <Card style={{ padding:20 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:24 }}>
                <div>
                  <h3 style={{ fontWeight:700, fontSize:15, color:'var(--b360-text)' }}>Revenue Trend</h3>
                  <span style={{ fontSize:12, color:'var(--b360-text-secondary)' }}>Period: {dashboardPeriod}</span>
                </div>
                <select
                  value={dashboardPeriod}
                  onChange={e => setDashboardPeriod(e.target.value)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    border: '1px solid var(--b360-border)',
                    borderRadius: 20,
                    padding: '6px 12px',
                    fontSize: 12,
                    fontWeight: 600,
                    color: 'var(--b360-green)',
                    background: 'white',
                    cursor: 'pointer',
                    outline: 'none'
                  }}
                >
                  <option value="Today">Today</option>
                  <option value="7 Days">7 Days</option>
                  <option value="This Month">This Month</option>
                </select>
              </div>

              {/* Custom SVG Bar Chart */}
              <div style={{ display:'flex', width:'100%', height:200, alignItems:'flex-end', gap:12, marginTop:20 }}>
                {/* Y Axis */}
                <div style={{ display:'flex', flexDirection:'column', justifyContent:'space-between', height:'100%', paddingBottom:20, fontSize:10, color:'var(--b360-text-secondary)', textAlign:'right', minWidth:30 }}>
                  <span>400K</span>
                  <span>300K</span>
                  <span>200K</span>
                  <span>100K</span>
                  <span>0</span>
                </div>
                {/* Bars */}
                <div style={{ display:'flex', flex:1, height:'100%', justifyContent:'space-around', alignItems:'flex-end' }}>
                  {[
                    { day: 'Mon', val: 18000 },
                    { day: 'Tue', val: 24000 },
                    { day: 'Wed', val: 19000 },
                    { day: 'Thu', val: 31000 },
                    { day: 'Fri', val: 27000 },
                    { day: 'Sat', val: 22000 },
                    { day: 'Sun', val: 34000 }
                  ].map((d, idx) => {
                    const maxVal = 34000
                    const heightPercent = `${(d.val / maxVal) * 80}%`
                    return (
                      <div key={idx} style={{ display:'flex', flexDirection:'column', alignItems:'center', flex:1, gap:8 }}>
                        <div style={{ width:24, height:heightPercent, background:'var(--b360-green)', borderRadius:'4px 4px 0 0' }} />
                        <span style={{ fontSize:11, color:'var(--b360-text-secondary)', fontWeight:500 }}>{d.day}</span>
                      </div>
                    )
                  })}
                </div>
              </div>
            </Card>

            {/* Quick Alerts Card */}
            <Card style={{ padding:20 }}>
              <h3 style={{ fontWeight:700, fontSize:15, color:'var(--b360-text)', marginBottom:20 }}>Quick Alerts</h3>
              <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
                {[
                  { msg: `${lowStockProducts.length || 2} products low stock`, icon: AlertTriangle, color: 'var(--b360-amber)', bg: 'var(--b360-amber-bg)' },
                  { msg: `${recentOrders.filter(o => o.paymentStatus === 'PENDING').length} unpaid orders`, icon: Clock, color: 'var(--b360-red)', bg: 'var(--b360-red-bg)' },
                  { msg: '5 new customers this week', icon: UserPlus, color: 'var(--b360-green)', bg: 'var(--b360-green-bg)' },
                  { msg: 'Mpesa: 2 unreconciled', icon: Activity, color: 'var(--b360-blue)', bg: 'var(--b360-blue-bg)' }
                ].map((a, i) => {
                  const Icon = a.icon
                  return (
                    <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'0 12px', height:52, borderRadius:10, background:'#F8FAFC', border:'1px solid var(--b360-border)' }}>
                      <div style={{ background:a.bg, color:a.color, borderRadius:'50%', width:32, height:32, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                        <Icon size={16} />
                      </div>
                      <span style={{ fontSize:13, fontWeight:600, color:'var(--b360-text)' }}>{a.msg}</span>
                    </div>
                  )
                })}
              </div>
            </Card>
          </div>

          {/* Recent Orders + Top Customers */}
          <div className="responsive-grid responsive-grid-2">
            {/* Recent Orders */}
            <Card style={{ padding:20 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
                <h3 style={{ fontWeight:700, fontSize:15, color:'var(--b360-text)' }}>Recent Orders</h3>
                <span style={{ fontSize:13, fontWeight:600, color:'var(--b360-green)', cursor:'pointer' }}>View all</span>
              </div>
              
              <div className="table-container" style={{ border: 'none', marginBottom: 0 }}>
                <table style={{ width:'100%', borderCollapse:'collapse', textAlign:'left' }}>
                  <thead>
                    <tr style={{ borderBottom:'1px solid var(--b360-border)' }}>
                      <th style={{ padding:'8px 0', fontSize:12, fontWeight:700, color:'var(--b360-text-secondary)', width:'20%' }}>Order No.</th>
                      <th style={{ padding:'8px 0', fontSize:12, fontWeight:700, color:'var(--b360-text-secondary)', width:'35%' }}>Customer</th>
                      <th style={{ padding:'8px 0', fontSize:12, fontWeight:700, color:'var(--b360-text-secondary)', width:'15%' }}>Status</th>
                      <th style={{ padding:'8px 0', fontSize:12, fontWeight:700, color:'var(--b360-text-secondary)', width:'15%' }}>Amount</th>
                      <th style={{ padding:'8px 0', fontSize:12, fontWeight:700, color:'var(--b360-text-secondary)', width:'15%' }}>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentOrders.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ padding:'20px 0', textAlign:'center', color:'var(--b360-text-secondary)' }}>No orders yet</td>
                      </tr>
                    ) : (
                      recentOrders.slice(0, 5).map((o, idx) => (
                        <tr key={o.id} style={{ borderBottom: idx < 4 ? '1px solid #F1F5F9' : 'none' }}>
                          <td style={{ padding:'12px 0', fontSize:13, fontWeight:600, color:'var(--b360-text)' }}>{o.orderNumber}</td>
                          <td style={{ padding:'12px 0', fontSize:13, color:'var(--b360-text-secondary)' }}>{o.customerName}</td>
                          <td style={{ padding:'12px 0' }}><StatusBadge status={o.paymentStatus} /></td>
                          <td style={{ padding:'12px 0', fontSize:13, fontWeight:700, color:'var(--b360-text)' }}>KES {o.subtotal.toLocaleString()}</td>
                          <td style={{ padding:'12px 0', fontSize:13, color:'var(--b360-text-secondary)' }}>Today, 10:30 AM</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </Card>

            {/* Top Customers */}
            <Card style={{ padding:20 }}>
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 }}>
                <h3 style={{ fontWeight:700, fontSize:15, color:'var(--b360-text)' }}>Top Customers</h3>
                <span style={{ fontSize:13, fontWeight:600, color:'var(--b360-green)', cursor:'pointer' }}>View all</span>
              </div>
              
              <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
                {topCustomers.length === 0 ? (
                  <div style={{ padding:'20px 0', textAlign:'center', color:'var(--b360-text-secondary)', fontSize:13 }}>No customer data yet</div>
                ) : (
                  topCustomers.map((c, i, arr) => {
                    const initials = c.name.split(' ').map((n: string) => n[0]).join('').toUpperCase()
                    return (
                      <div key={c.id}>
                        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 0' }}>
                          <div style={{ display:'flex', alignItems:'center', gap:12 }}>
                            <div style={{ width:36, height:36, borderRadius:'50%', background:'var(--b360-blue-bg)', color:'var(--b360-blue)', fontWeight:700, fontSize:12, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                              {initials}
                            </div>
                            <div style={{ display:'flex', flexDirection:'column' }}>
                              <span style={{ fontSize:13, fontWeight:600, color:'var(--b360-text)' }}>{c.name}</span>
                              <span style={{ fontSize:11, color:'var(--b360-text-secondary)' }}>{c.totalOrders} {c.totalOrders === 1 ? 'order' : 'orders'}</span>
                            </div>
                          </div>
                          <span style={{ fontSize:13, fontWeight:700, color:'var(--b360-text)' }}>KES {c.totalSpent.toLocaleString()}</span>
                        </div>
                        {i < arr.length - 1 && <div style={{ borderTop:'1px solid #F1F5F9' }} />}
                      </div>
                    )
                  })
                )}
              </div>
            </Card>
          </div>
        </>
      )}
    </div>
  )
}

// ── Inventory ─────────────────────────────────────────────────────────────────
const CATEGORIES = ['Electronics','Clothing','Food & Beverage','Health & Beauty','Home & Garden','Stationery','Other']

const emptyProduct = { name:'', sku:'', category:'Other', buyingPrice:'', sellingPrice:'', currentStock:'', lowStockThreshold:'10', description:'' }

export function InventoryPage() {
  const [search, setSearch] = useState('')
  const [lowOnly, setLowOnly] = useState(false)
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [showAdd, setShowAdd] = useState(false)
  const [editProduct, setEditProduct] = useState<ProductResponse | null>(null)
  const [stockProduct, setStockProduct] = useState<ProductResponse | null>(null)
  const [form, setForm] = useState(emptyProduct)
  const [stockQty, setStockQty] = useState('')
  const [error, setError] = useState('')

  const loadProducts = () => {
    setLoading(true)
    productApi.list().then(res => {
      if (res.success && res.data) setProducts(res.data)
    }).finally(() => setLoading(false))
  }

  useEffect(() => { loadProducts() }, [])

  const stockStatus = (p: ProductResponse) => p.isOutOfStock ? 'OUT' : p.isLowStock ? 'LOW' : 'OK'
  const stockColor = (s: string) => s === 'OUT' ? 'var(--b360-red)' : s === 'LOW' ? 'var(--b360-amber)' : 'var(--b360-green)'

  const filtered = products.filter(p =>
    (p.name.toLowerCase().includes(search.toLowerCase()) || p.sku.toLowerCase().includes(search.toLowerCase()))
    && (!lowOnly || stockStatus(p) !== 'OK')
  )

  const openAdd = () => { setForm(emptyProduct); setError(''); setShowAdd(true) }
  const openEdit = (p: ProductResponse) => {
    setForm({ name:p.name, sku:p.sku, category:p.category, buyingPrice:String(p.buyingPrice),
      sellingPrice:String(p.sellingPrice), currentStock:String(p.currentStock),
      lowStockThreshold:String(p.lowStockThreshold), description:p.description })
    setError(''); setEditProduct(p)
  }
  const openStock = (p: ProductResponse) => { setStockQty(''); setError(''); setStockProduct(p) }

  const handleSaveProduct = async () => {
    if (!form.name || !form.sku || !form.buyingPrice || !form.sellingPrice || !form.currentStock) {
      setError('Please fill in all required fields.'); return
    }
    setSaving(true); setError('')
    try {
      const payload = {
        name: form.name, sku: form.sku, category: form.category,
        buyingPrice: Number(form.buyingPrice), sellingPrice: Number(form.sellingPrice),
        currentStock: Number(form.currentStock), lowStockThreshold: Number(form.lowStockThreshold) || 10,
        description: form.description,
      }
      const res = editProduct
        ? await productApi.update(editProduct.id, payload)
        : await productApi.create(payload)
      if (res.success) {
        setShowAdd(false); setEditProduct(null); loadProducts()
      } else {
        setError(res.message || 'Failed to save product.')
      }
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const handleUpdateStock = async () => {
    if (!stockQty || isNaN(Number(stockQty))) { setError('Enter a valid quantity.'); return }
    if (!stockProduct) return
    setSaving(true); setError('')
    try {
      const res = await productApi.updateStock(stockProduct.id, { quantityToAdd: Number(stockQty) })
      if (res.success) { setStockProduct(null); loadProducts() }
      else setError(res.message || 'Failed to update stock.')
    } catch (e: any) {
      setError(e.response?.data?.message || 'Network error. Please try again.')
    } finally { setSaving(false) }
  }

  const f = (k: keyof typeof emptyProduct) => (v: string) => setForm(prev => ({ ...prev, [k]: v }))

  const productModal = (title: string, onClose: () => void) => (
    <Modal title={title} onClose={onClose}
      footer={<><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn onClick={handleSaveProduct} disabled={saving}>{saving ? 'Saving...' : 'Save Product'}</Btn></>}>
      <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
        {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
        <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
          <Input label="Product Name *" value={form.name} onChange={f('name')} placeholder="e.g. Men's Shirt" />
          <Input label="SKU *" value={form.sku} onChange={f('sku')} placeholder="e.g. SHIRT-001" />
        </div>
        <Select label="Category" value={form.category} onChange={f('category')}
          options={CATEGORIES.map(c => ({ value:c, label:c }))} />
        <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
          <Input label="Buying Price (KES) *" value={form.buyingPrice} onChange={f('buyingPrice')} type="number" placeholder="0" />
          <Input label="Selling Price (KES) *" value={form.sellingPrice} onChange={f('sellingPrice')} type="number" placeholder="0" />
        </div>
        <div className="responsive-grid responsive-grid-2" style={{ gap:12 }}>
          <Input label="Current Stock *" value={form.currentStock} onChange={f('currentStock')} type="number" placeholder="0" />
          <Input label="Low Stock Threshold" value={form.lowStockThreshold} onChange={f('lowStockThreshold')} type="number" placeholder="10" />
        </div>
        <Input label="Description" value={form.description} onChange={f('description')} placeholder="Optional product description" />
      </div>
    </Modal>
  )

  return (
    <div className="fade-in" style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {showAdd && productModal('Add Product', () => setShowAdd(false))}
      {editProduct && productModal('Edit Product', () => setEditProduct(null))}
      {stockProduct && (
        <Modal title={`Update Stock — ${stockProduct.name}`} onClose={() => setStockProduct(null)}
          footer={<><Btn variant="secondary" onClick={() => setStockProduct(null)}>Cancel</Btn><Btn onClick={handleUpdateStock} disabled={saving}>{saving ? 'Updating...' : 'Update Stock'}</Btn></>}>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {error && <p style={{ color:'var(--b360-red)', fontSize:12 }}>{error}</p>}
            <p style={{ fontSize:13, color:'var(--b360-text-secondary)' }}>Current stock: <strong>{stockProduct.currentStock}</strong></p>
            <Input label="Quantity to Add" value={stockQty} onChange={setStockQty} type="number" placeholder="e.g. 50" />
          </div>
        </Modal>
      )}

      <PageHeader title="Inventory"
        action={<Btn icon={<Plus size={14}/>} onClick={openAdd}>Add Product</Btn>} />

      <div className="responsive-grid responsive-grid-4" style={{ gap:12 }}>
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
          <div style={{ padding:40, textAlign:'center', color:'var(--b360-text-secondary)' }}>No products yet. Click "Add Product" to get started.</div>
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
                  <Btn variant="secondary" small icon={<Edit size={12}/>} onClick={() => openEdit(p)}>Edit</Btn>
                  <Btn variant="secondary" small icon={<Plus size={12}/>} onClick={() => openStock(p)}>Stock</Btn>
                </div>
              ]
            })}
          />
        )}
      </Card>
    </div>
  )
}

